package org.upn.edu.pe.model;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upn.edu.pe.repository.IProductoRepository;

@Service
public class ProductoService {
	@Autowired
    private IProductoRepository productoRepository;
	
	public Producto buscarPorNombre(String nombre) {
        return productoRepository.findByDescripcion(nombre);
    }

    public void guardarProducto(Producto producto) {
        productoRepository.save(producto);
    }
}
