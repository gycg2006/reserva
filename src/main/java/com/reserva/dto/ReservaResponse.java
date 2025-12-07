package com.reserva.dto;

import com.reserva.model.ReservaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponse {
    private Long id;
    private String clienteId;
    private Long categoriaCarroId;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Double valorTotalEstimado;
    private ReservaStatus status;
}

