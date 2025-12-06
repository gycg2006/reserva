package com.reserva.dto;

import com.reserva.model.ReservaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "O status não pode ser nulo.")
    private ReservaStatus status;
}

