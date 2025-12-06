package com.reserva.controller;

import com.reserva.dto.ReservaRequest;
import com.reserva.dto.ReservaResponse;
import com.reserva.model.Reserva;
import com.reserva.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criarReserva(@RequestBody @Valid ReservaRequest dados) {
        Reserva novaReserva = reservaService.criarReserva(dados);
        ReservaResponse response = mapToResponse(novaReserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> buscarReserva(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id);
        ReservaResponse response = mapToResponse(reserva);
        return ResponseEntity.ok(response);
    }

    private ReservaResponse mapToResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getClienteId(),
                reserva.getCategoriaCarroId(),
                reserva.getDataInicio(),
                reserva.getDataFim(),
                reserva.getValorTotalEstimado(),
                reserva.getStatus()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}