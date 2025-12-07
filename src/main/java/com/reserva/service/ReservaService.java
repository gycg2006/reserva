package com.reserva.service;

import com.reserva.client.FrotaClient;
import com.reserva.dto.ReservaRequest;
import com.reserva.dto.VeiculoDto;
import com.reserva.model.Reserva;
import com.reserva.model.ReservaStatus;
import com.reserva.repository.ReservaRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final FrotaClient frotaClient;

    public ReservaService(ReservaRepository reservaRepository, FrotaClient frotaClient) {
        this.reservaRepository = reservaRepository;
        this.frotaClient = frotaClient;
    }

    @Transactional
    public Reserva criarReserva(ReservaRequest dados) {

        validarDatas(dados.getDataInicio(), dados.getDataFim());

        VeiculoDto veiculo;
        try {
            veiculo = frotaClient.consultarVeiculo(dados.getCategoriaCarroId());
        } catch (feign.FeignException.NotFound e) {
            throw new IllegalArgumentException("Veículo não encontrado com o ID informado.");
        } catch (feign.FeignException e) {
            throw new IllegalStateException("Erro ao comunicar com o serviço de frota.", e);
        }

        if (!"disponível".equalsIgnoreCase(veiculo.getStatus())) {
            throw new IllegalArgumentException("O veículo solicitado não está disponível no momento (Status: " + veiculo.getStatus() + ").");
        }

        Reserva novaReserva = new Reserva();
        novaReserva.setClienteId(dados.getClienteId());
        novaReserva.setCategoriaCarroId(dados.getCategoriaCarroId());
        novaReserva.setDataInicio(dados.getDataInicio());
        novaReserva.setDataFim(dados.getDataFim());
        novaReserva.setStatus(ReservaStatus.PENDENTE);

        long dias = ChronoUnit.DAYS.between(dados.getDataInicio(), dados.getDataFim());
        if (dias == 0) dias = 1;
        
        if (veiculo.getPreco() != null) {
            novaReserva.setValorTotalEstimado(dias * veiculo.getPreco().doubleValue());
        } else {
            novaReserva.setValorTotalEstimado(dias * 100.00);
        }

        Reserva reservaSalva = reservaRepository.save(novaReserva);

        try {
            veiculo.setStatus("ALUGADO");
            frotaClient.atualizarVeiculo(veiculo.getId(), veiculo);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao atualizar status do veículo para ALUGADO. Reserva cancelada.", e);
        }

        return reservaSalva;
    }

    private void validarDatas(LocalDateTime inicio, LocalDateTime fim) {
        if (fim.isBefore(inicio) || fim.isEqual(inicio)) {
            throw new IllegalArgumentException("A data de devolução deve ser posterior à data de retirada.");
        }
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada com id: " + id));
    }

    @Transactional
    public Reserva atualizarStatus(Long id, ReservaStatus novoStatus) {
        Reserva reserva = buscarPorId(id);
        
        if ((novoStatus == ReservaStatus.CANCELADA || novoStatus == ReservaStatus.CONCLUIDA) 
                && reserva.getStatus() != ReservaStatus.CANCELADA) {
            try {
                VeiculoDto veiculo = frotaClient.consultarVeiculo(reserva.getCategoriaCarroId());
                veiculo.setStatus("disponível");
                frotaClient.atualizarVeiculo(veiculo.getId(), veiculo);
            } catch (Exception e) {
                System.err.println("Erro ao liberar veículo no serviço de frota: " + e.getMessage());
            }
        }

        reserva.setStatus(novoStatus);
        return reservaRepository.save(reserva);
    }
}