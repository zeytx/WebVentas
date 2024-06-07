package org.upn.edu.pe.model;

import java.util.Date;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "venta")
public class Venta {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int idVenta;
	@Temporal(TemporalType.DATE)
	private Date fechaRegistro;
	private double montoTotal;
	@ManyToOne
	@JoinColumn(name="idUsuario")
	private Usuario usuario;
	
	
	public Venta() {
	}

	
	
	
	public Venta(int idVenta, Date fechaRegistro, double montoTotal, Usuario usuario) {
		super();
		this.idVenta = idVenta;
		this.fechaRegistro = fechaRegistro;
		this.montoTotal = montoTotal;
		this.usuario = usuario;
	}




	public int getIdVenta() {
		return idVenta;
	}


	public void setIdVenta(int idVenta) {
		this.idVenta = idVenta;
	}


	public Date getFechaRegistro() {
		return fechaRegistro;
	}


	public void setFechaRegistro(Date fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}


	public double getMontoTotal() {
		return montoTotal;
	}


	public void setMontoTotal(double montoTotal) {
		this.montoTotal = montoTotal;
	}


	public Usuario getUsuario() {
		return usuario;
	}


	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}
	
	
	
	
}
