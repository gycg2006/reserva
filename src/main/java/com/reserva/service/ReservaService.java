package com.reserva.service;

import com.reserva.client.FrotaClient;
import com.reserva.dto.ReservaRequest;
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

        try {
            var veiculos = frotaClient.listarVeiculos();
            
            boolean isDisponivel = veiculos != null && 
                    veiculos.stream()
                            .anyMatch(v -> "disponível".equalsIgnoreCase(v.getStatus()));

            if (!isDisponivel) {
                throw new IllegalArgumentException("Não há carros disponíveis para esta categoria nestas datas.");
            }
        } catch (feign.FeignException e) {
            throw new IllegalStateException("Erro ao comunicar com o serviço de frota. Tente novamente mais tarde.", e);
        }

        Reserva novaReserva = new Reserva();
        novaReserva.setClienteId(dados.getClienteId());
        novaReserva.setCategoriaCarroId(dados.getCategoriaCarroId());
        novaReserva.setDataInicio(dados.getDataInicio());
        novaReserva.setDataFim(dados.getDataFim());

        novaReserva.setStatus(ReservaStatus.PENDENTE);

        long dias = ChronoUnit.DAYS.between(dados.getDataInicio(), dados.getDataFim());
        if (dias == 0) dias = 1; 
        novaReserva.setValorTotalEstimado(dias * 100.00);

        return reservaRepository.save(novaReserva);
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
        reserva.setStatus(novoStatus);
        return reservaRepository.save(reserva);
    }
}