package com.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VeiculoDto {
    private Long id;
    private String modelo;
    private String marca;
    private Integer ano;
    private String placa;
    private BigDecimal preco;
    private String status;
}
