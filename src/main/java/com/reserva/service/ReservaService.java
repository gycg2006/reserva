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
import java.util.List;

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

        // 1. Check for conflicting dates
        List<ReservaStatus> statusIgnorados = List.of(ReservaStatus.CANCELADA, ReservaStatus.CONCLUIDA);
        List<Reserva> conflitos = reservaRepository.findConflitosDeReserva(
                dados.getCategoriaCarroId(),
                dados.getDataInicio(),
                dados.getDataFim(),
                statusIgnorados
        );

        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException("Este veículo já está reservado para o período selecionado.");
        }

        // 2. Fetch vehicle data (to get price and ensure it exists)
        VeiculoDto veiculo;
        try {
            veiculo = frotaClient.consultarVeiculo(dados.getCategoriaCarroId());
        } catch (feign.FeignException.NotFound e) {
            throw new IllegalArgumentException("Veículo não encontrado com o ID informado.");
        } catch (feign.FeignException e) {
            throw new IllegalStateException("Erro ao comunicar com o serviço de frota.", e);
        }

        // NOTE: We REMOVED the check: if (!"DISPONIVEL".equalsIgnoreCase(veiculo.getStatus()))
        // This allows booking a car that is currently "ALUGADO" but will be free on the requested dates.

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

        // 3. Update status in FrotaService ONLY if the reservation starts effectively NOW (today)
        // If the reservation is for a future date, we don't change the car's current status yet.
        boolean comecaHoje = dados.getDataInicio().toLocalDate().isEqual(LocalDateTime.now().toLocalDate());

        if (comecaHoje) {
            try {
                veiculo.setStatus("ALUGADO");
                frotaClient.atualizarVeiculo(veiculo.getId(), veiculo);
            } catch (Exception e) {
                // Log warning but don't fail the transaction, as the reservation is valid in our DB
                System.err.println("Aviso: Não foi possível atualizar status do carro no serviço de frota: " + e.getMessage());
            }
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
        
        boolean deveLiberarCarro = (novoStatus == ReservaStatus.CANCELADA || novoStatus == ReservaStatus.CONCLUIDA);
        
        boolean carroEstaPreso = (reserva.getStatus() != ReservaStatus.CANCELADA && reserva.getStatus() != ReservaStatus.CONCLUIDA);

        LocalDateTime agora = LocalDateTime.now();
        boolean reservaAtiva = (agora.isAfter(reserva.getDataInicio()) || agora.isEqual(reserva.getDataInicio())) 
                                && agora.isBefore(reserva.getDataFim());

        if (deveLiberarCarro && carroEstaPreso) {
            try {
                VeiculoDto veiculo = frotaClient.consultarVeiculo(reserva.getCategoriaCarroId());
                
                veiculo.setStatus("DISPONIVEL"); 
                
                frotaClient.atualizarVeiculo(veiculo.getId(), veiculo);
                System.out.println("Veículo liberado com sucesso: ID " + veiculo.getId());
                
            } catch (Exception e) {
                System.err.println("Erro ao liberar veículo: " + e.getMessage());
            }
        }

        reserva.setStatus(novoStatus);
        return reservaRepository.save(reserva);
    }

    public java.util.List<Reserva> listarReservas() {
        return reservaRepository.findAll();
    }
}