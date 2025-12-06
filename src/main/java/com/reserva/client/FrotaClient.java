package com.reserva.client;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.Data;

@FeignClient(name = "frota-service", url = "${frota.service.url:https://ms-veiculos.onrender.com}")
public interface FrotaClient {

    @GetMapping("/api/veiculos")
    List<VeiculoResponse> listarVeiculos();

    @GetMapping("/api/veiculos/{id}")
    VeiculoResponse consultarVeiculo(@PathVariable("id") Long id);

    @Data
    class VeiculoResponse {
        private Long id;
        private String modelo;
        private String marca;
        private Integer ano;
        private String placa;
        private BigDecimal preco;
        private String status;
    }
}
