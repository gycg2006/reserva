package com.reserva.repository;

import com.reserva.model.Reserva;
import com.reserva.model.ReservaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByStatusAndDataInicioBefore(ReservaStatus status, LocalDateTime dataLimite);

    List<Reserva> findByClienteId(Long clienteId);
}