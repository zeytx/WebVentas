package org.upn.edu.pe.repository;

import org.upn.edu.pe.model.Detalle;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IDetalleRepository extends JpaRepository<Detalle, Integer>{
	
	@Query("SELECT d FROM Detalle d JOIN FETCH d.venta v WHERE v.idVenta = :idVenta")
    List<Detalle> findByVentaId(@Param("idVenta") int idVenta);
}
