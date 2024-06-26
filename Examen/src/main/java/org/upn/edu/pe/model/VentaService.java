package org.upn.edu.pe.model;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upn.edu.pe.repository.IVentaRepository;

@Service
@Transactional
public class VentaService {
	@Autowired
    private IVentaRepository ventaRepository;

    public void guardarVentaConDetalles(Venta venta) {
        // Asegura que los detalles estén asociados a la venta
        for (Detalle detalle : venta.getDetalles()) {
            detalle.setVenta(venta);
        }
        ventaRepository.save(venta);
    }
}
