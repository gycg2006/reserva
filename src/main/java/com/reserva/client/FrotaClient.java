package com.reserva.client;

import java.math.BigDecimal;
import java.util.List;

import com.reserva.dto.VeiculoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;     
import org.springframework.web.bind.annotation.RequestBody;

import lombok.Data;

@FeignClient(name = "frota-service", url = "${frota.service.url:https://ms-veiculos.onrender.com}")
public interface FrotaClient {

    @GetMapping("/api/veiculos")
    List<VeiculoDto> listarVeiculos();

    @GetMapping("/api/veiculos/{id}")
    VeiculoDto consultarVeiculo(@PathVariable("id") Long id);

    @PutMapping("/api/veiculos/{id}")
    VeiculoDto atualizarVeiculo(@PathVariable("id") Long id, @RequestBody VeiculoDto veiculo);
}
