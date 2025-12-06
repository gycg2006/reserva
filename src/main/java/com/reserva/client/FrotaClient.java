package com.reserva.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "frota-service", url = "${frota.service.url:https://seu-servico.onrender.com}")
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
