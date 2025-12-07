package com.reserva.repository;

import com.reserva.model.Reserva;
import com.reserva.model.ReservaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByStatusAndDataInicioBefore(ReservaStatus status, LocalDateTime dataLimite);

    List<Reserva> findByClienteId(String clienteId);

    @Query("SELECT r FROM Reserva r " +
           "WHERE r.categoriaCarroId = :carroId " +
           "AND r.status NOT IN (:statusIgnorados) " +
           "AND (r.dataInicio < :dataFim AND r.dataFim > :dataInicio)")
    List<Reserva> findConflitosDeReserva(
            @Param("carroId") Long carroId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("statusIgnorados") List<ReservaStatus> statusIgnorados
    );
}