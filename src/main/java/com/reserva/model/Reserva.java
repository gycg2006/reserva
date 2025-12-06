package com.reserva.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;
    private Long categoriaCarroId;

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    private Double valorTotalEstimado;

    @Enumerated(EnumType.STRING)
    private ReservaStatus status;
}