package com.reserva.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservaRequest {

    @NotNull(message = "O ID do cliente não pode ser nulo.")
    private Long clienteId;

    @NotNull(message = "O ID da categoria do carro não pode ser nulo.")
    private Long categoriaCarroId;

    @NotNull(message = "A data de início não pode ser nula.")
    @FutureOrPresent(message = "A data de início não pode ser no passado")
    private LocalDateTime dataInicio;

    @NotNull(message = "A data de fim não pode ser nula")
    @Future(message = "A data de fim deve ser uma data futura")
    private LocalDateTime dataFim;
}