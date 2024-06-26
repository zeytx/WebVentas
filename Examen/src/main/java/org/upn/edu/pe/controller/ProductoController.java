package org.upn.edu.pe.controller;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.naming.AuthenticationException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.upn.edu.pe.model.Detalle;
import org.upn.edu.pe.model.FileUploadConfig;
import org.upn.edu.pe.model.Producto;
import org.upn.edu.pe.model.ProductoService;
import org.upn.edu.pe.model.Usuario;
import org.upn.edu.pe.model.UsuarioService;
import org.upn.edu.pe.model.Venta;
import org.upn.edu.pe.model.VentaService;
import org.upn.edu.pe.model.reclamo;
import org.upn.edu.pe.repository.IDetalleRepository;
import org.upn.edu.pe.repository.IProductoRepository;
import org.upn.edu.pe.repository.IReclamoRepository;
import org.upn.edu.pe.repository.IUserRepository;
import org.upn.edu.pe.repository.IVentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
@SessionAttributes({ "carrito", "total", "subtotal", "costoEnvio", "descuento" })
public class ProductoController {

	// Inicializacion del objeto carrito
	@ModelAttribute("carrito")
	public List<Detalle> getCarrito() {
		return new ArrayList<>();
	}

	// Inicializacion del objeto total
	@ModelAttribute("total")
	public double getTotal() {
		return 0.0;
	}

	// Inicializacion del objeto subtotal
	@ModelAttribute("subtotal")
	public double getSubtotal() {
		return 0.0;
	}

	// Inicializacion del objeto costoEnvio

	@ModelAttribute("descuento")
	public double getDescuento() {
		return 0.0;
	}

	// Inicializacion del objeto costoEnvio

	@ModelAttribute("costoEnvio")
	public double getCostoEnvio() {
		return 0.0;
	}

	// Declaracion e Inicializacion de objetos para el control del carrito de
	// compras
	@Autowired
	private IProductoRepository productoRepository;

	@Autowired
	private IVentaRepository ventaRepository;

	@Autowired
	private IDetalleRepository detalleRepository;

	// Método para visualizar los productos a vender
	@GetMapping("/index")
	public String listado(Model model, HttpSession session) {
		List<Producto> lista = productoRepository.findAll();
		model.addAttribute("productos", lista);

		Usuario usuario = (Usuario) session.getAttribute("usuario");
		if (usuario != null) {
			model.addAttribute("nombreUsuario", usuario.getNombre());
		}

		return "index";
	}

	@GetMapping("/reclamo")
	public String reclamo(Model model, HttpSession session) {
		if (session.getAttribute("usuario") != null) {
			model.addAttribute("usuario", session.getAttribute("usuario"));
		}
		return "reclamo";
	}

	// Método para agregar productos al carrito
	@GetMapping("/agregar/{idProducto}")
	public String agregar(Model model, @PathVariable(name = "idProducto", required = true) int idProducto) {
		Producto p = productoRepository.findById(idProducto).orElse(null);
		List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
		double subtotal = 0.0;
		boolean existe = false;
		Detalle detalle = new Detalle();

		if (p != null) {
			detalle.setProducto(p);
			detalle.setCantidad(1);
			detalle.setSubtotal(detalle.getProducto().getPrecio() * detalle.getCantidad());
		}

		if (carrito.size() == 0) {
			carrito.add(detalle);
		} else {
			for (Detalle d : carrito) {
				if (d.getProducto().getIdProducto() == p.getIdProducto()) {
					d.setCantidad(d.getCantidad() + 1);
					d.setSubtotal(d.getProducto().getPrecio() * d.getCantidad());
					existe = true;
				}
			}
			if (!existe)
				carrito.add(detalle);
		}

		for (Detalle d : carrito)
			subtotal += d.getSubtotal();

		double costoEnvio = 0;
		double descuento = 0;
		if (subtotal > 2000) {
			descuento = 0;
		}

		double total = subtotal - descuento + costoEnvio;

		model.addAttribute("subtotal", subtotal);
		model.addAttribute("total", total);
		model.addAttribute("descuento", descuento);
		model.addAttribute("carrito", carrito);
		model.addAttribute("costoEnvio", costoEnvio);

		return "redirect:/index";
	}

	// Método para calcular el descuento y el costo de envío
	@PostMapping("/actualizarCarrito")
	public String actualizarCarrito(Model model, HttpSession session, @RequestParam(name = "idProducto") int idProducto,
			@RequestParam(name = "delta") int delta) {
		List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");

		if (carrito != null) {
			for (Iterator<Detalle> iterator = carrito.iterator(); iterator.hasNext();) {
				Detalle detalle = iterator.next();
				if (detalle.getProducto().getIdProducto() == idProducto) {
					int nuevaCantidad = detalle.getCantidad() + delta;
					if (nuevaCantidad <= 0) {
						iterator.remove();
					} else {
						detalle.setCantidad(nuevaCantidad);
						detalle.setSubtotal(detalle.getProducto().getPrecio() * nuevaCantidad);
					}
					break;
				}
			}
			session.setAttribute("carrito", carrito); // Actualizar la sesión también
		}

		calcularDescuentoYCostoEnvio(model, carrito);
		return "redirect:/carrito";
	}

	private void calcularDescuentoYCostoEnvio(Model model, List<Detalle> carrito) {
		double subtotal = 0.0;

		for (Detalle d : carrito) {
			subtotal += d.getSubtotal();
		}

		double costoEnvio = 0;
		double descuento = 0;
		if (subtotal >= 200) {
			descuento = 20;
			costoEnvio = 50;
		} else if (subtotal >= 60) {
			costoEnvio = 20;
		} else if (subtotal <= 50 && subtotal != 0) {
			costoEnvio = 10;
		} else {
			costoEnvio = 0.05 * subtotal;
		}

		model.addAttribute("subtotal", subtotal);
		model.addAttribute("costoEnvio", costoEnvio);
		model.addAttribute("descuento", descuento);

		double total = subtotal - descuento + costoEnvio;
		model.addAttribute("total", total);
	}

	// Método para eliminar un producto del carrito
	@PostMapping("/eliminar")
	public String eliminarProducto(Model model, HttpSession session,
			@RequestParam(name = "idProducto", required = true) int idProducto,
			@RequestParam(name = "cantidadEliminar", required = true) int cantidadEliminar) {
		List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");
		if (carrito != null) {
			double subtotal = 0.0;
			double costoEnvio = 0.0;

			for (Iterator<Detalle> iterator = carrito.iterator(); iterator.hasNext();) {
				Detalle detalle = iterator.next();
				if (detalle.getProducto().getIdProducto() == idProducto) {
					int cantidadActual = detalle.getCantidad();
					if (cantidadEliminar >= cantidadActual) {
						iterator.remove(); // Eliminar el producto completo
					} else {
						detalle.setCantidad(cantidadActual - cantidadEliminar); // Reducir la cantidad del producto
						detalle.setSubtotal(detalle.getProducto().getPrecio() * detalle.getCantidad()); // Actualizar el
																										// subtotal del
																										// detalle
					}
				}
				break;
			}

			session.setAttribute("carrito", carrito);

			calcularDescuentoYCostoEnvio(model, carrito);

		}

		return "redirect:/carrito";
	}

	// Método para visualizar el carrito de compras
	@GetMapping("/carrito")
	public String carrito(Model model, HttpSession session) {

		List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");

		if (carrito != null) {
			calcularDescuentoYCostoEnvio(model, carrito);
		}

		if (session.getAttribute("usuario") != null) {
			model.addAttribute("usuario", session.getAttribute("usuario"));
		}
		return "carrito";
	}

	// Método para logueo
	@Controller
	public class LoginController {

		@Autowired
		private UsuarioService usuarioService;

		@GetMapping("/login")
		public String login() {
			return "login";
		}

		@PostMapping("/login")
		public String loginUser(@RequestParam("dni") int dni, @RequestParam("contrase") String contrase,
				HttpSession session, Model model) {
			Usuario usuario = usuarioService.findByDNIAndContrase(dni, contrase);
			if (usuario != null && usuario.getContrase().equals(contrase)) {
				session.setAttribute("usuario", usuario);
				if (dni == 12345678) {
					return "redirect:/admin";
				} else {
					return "redirect:/index";
				}
			} else {
				model.addAttribute("error", "DNI o contraseña incorrectos");
				return "login";
			}
		}

		@GetMapping("/logout")
		public String logout(HttpSession session) {
			session.invalidate();
			return "redirect:/login";
		}
	}

	// Método para registrar usuario

	@Controller
	public class UsuarioController {

		@Autowired
		private IUserRepository usuarioRepository;

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@GetMapping("/registrar")
		public String showRegistrationForm(Model model) {
			model.addAttribute("usuario", new Usuario());
			return "registrar";
		}

		@PostMapping("/registrar")
		public String registrarUsuario(@ModelAttribute Usuario usuario) {
			// Aquí se podría agregar la lógica para codificar la contraseña si fuera
			// necesario
			// usuario.setContrase(passwordEncoder.encode(usuario.getContrase()));
			usuarioRepository.save(usuario);
			return "redirect:/login";
		}
	}

	// direcciones para html admin
	@GetMapping("/badministrarproductos")
	public String badministrarproductos(Model model) {
		return "badministrarproductos";
	}

	@GetMapping("/badministrarusuarios")
	public String badministrarusuarios(Model model) {
		return "badministrarusuarios";
	}

	@GetMapping("/gestionarpedidos")
	public String gestionarpedidos(Model model) {
		List<Venta> ventas = ventaRepository.findAll();
        model.addAttribute("ventas", ventas);
		return "gestionarpedidos";
	}
	
	@GetMapping("/detalles/{id_Venta}")
    @ResponseBody
    @Transactional(readOnly = true) 
    public List<Detalle> obtenerDetalles(@PathVariable int id_venta) {
        return detalleRepository.findByVentaId(id_venta);
    }

	
	@Controller
	@RequestMapping("/producto")
	public class Producto1Controller {

	    @Autowired
	    private FileUploadConfig fileUploadConfig;

	    @Autowired
	    private ProductoService productoService;

	    @GetMapping("/añadirproducto")
	    public String mostrarFormularioNuevo(Model model) {
	        model.addAttribute("producto", new Producto());
	        return "añadirproducto";
	    }

	    @PostMapping("/guardarProducto")
	    public String guardarProducto(@RequestParam("descripcion") String descripcion,
	                                  @RequestParam("precio") double precio,
	                                  @RequestParam("stock") int stock,
	                                  @RequestParam("imagen") MultipartFile imagen,
	                                  Model model) {
	        // Guardar la imagen en el directorio especificado
	    	 String fileName = imagen.getOriginalFilename();
	         try {
	             File file = new File(fileUploadConfig.getUploadDir() + File.separator + fileName);
	             imagen.transferTo(file);
	         } catch (IOException e) {
	             e.printStackTrace();
	             model.addAttribute("mensajeError", "Error al subir la imagen.");
	             return "añadirproducto";
	         }

	        // Crear el objeto Producto y guardar solo el nombre de la imagen en la base de datos
	        Producto producto = new Producto();
	        producto.setDescripcion(descripcion);
	        producto.setPrecio(precio);
	        producto.setStock(stock);
	        producto.setImagen(fileName); // Guardar solo el nombre de la imagen

	        // Guardar el producto usando el servicio
	        productoService.guardarProducto(producto);

	        // Mostrar mensaje de éxito
	        model.addAttribute("mensajeExito", "Producto añadido correctamente.");
	        
	        return "badministrarproductos"; // Redirige a donde corresponda
	    }
	    
	    
	    @GetMapping("/retirarproducto")
	    public String mostrarFormularioRetirar(Model model) {
	    	model.addAttribute("producto", null); 
	        return "retirarproducto";
	    }

	    @PostMapping("/buscarProductoParaRetirar")
	    public String buscarProductoParaRetirar(@RequestParam("nombreProducto") String nombreProducto, Model model) {
	    	Producto producto = productoService.buscarPorNombre(nombreProducto);
	        if (producto != null) {
	            model.addAttribute("producto", producto);
	        } else {
	            model.addAttribute("mensajeError", "No se encontró ningún producto con el nombre proporcionado.");
	        }
	        return "retirarproducto";
	    }

	    @PostMapping("/retirarProducto")
	    public String retirarProducto(@RequestParam("nombreProducto") String nombreProducto, Model model) {
	        // Aquí podrías implementar la lógica para retirar el producto
	        // Simulación: supongamos que el producto se retira correctamente
	        // Producto producto = productoService.buscarPorNombre(nombreProducto); // Ejemplo con servicio

	        // Simulación: como no tenemos un servicio real, simplemente mostramos el mensaje de éxito
	        model.addAttribute("mensajeExito", "Producto '" + nombreProducto + "' retirado exitosamente.");

	        return "retirarproducto"; // Redirigir a la página de administración de productos
	    }
	    
	}

	

	@Controller
	public class Usuario1Controller {

		@Autowired
		private IUserRepository usuarioRepository;

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@GetMapping("/modificarusuario")
		public String showModificarUsuarioForm(Model model) {
			model.addAttribute("usuario", new Usuario());
			return "modificarusuario";
		}

		@PostMapping("/buscarUsuarioPorDNI")
		public String buscarUsuarioPorDNI(@RequestParam("dni") int dni, Model model) {
			Usuario usuario = usuarioRepository.findByDni(dni);
			if (usuario != null) {
				model.addAttribute("usuario", usuario);
				return "modificarusuario";
			} else {
				model.addAttribute("mensajeError", "No se encontró ningún usuario con el DNI proporcionado.");
				return "modificarusuario";
			}
		}

		@PostMapping("/actualizarUsuario")
		public String actualizarUsuario(@ModelAttribute("usuario") Usuario usuarioActualizado, Model model) {
			// Buscar el usuario existente por DNI
			Usuario usuarioExistente = usuarioRepository.findByDni(usuarioActualizado.getDNI());

			if (usuarioExistente != null) {
				// Actualizar los campos del usuario existente con los valores del formulario
				usuarioExistente.setNombre(usuarioActualizado.getNombre());
				usuarioExistente.setApellido(usuarioActualizado.getApellido());
				usuarioExistente.setFechaNacimiento(usuarioActualizado.getFechaNacimiento());
				usuarioExistente.setTelefono(usuarioActualizado.getTelefono());
				usuarioExistente.setCorreo_electro(usuarioActualizado.getCorreo_electro()); // Asegúrate de establecer
																							// correctamente el
																							// correo_electro
				usuarioExistente.setContrase(usuarioActualizado.getContrase());
				usuarioExistente.setDireccion(usuarioActualizado.getDireccion());

				// Guardar el usuario actualizado en la base de datos
				usuarioRepository.save(usuarioExistente);

				// Mensaje de éxito
				model.addAttribute("mensajeExito", "Usuario actualizado correctamente.");
				model.addAttribute("usuario", new Usuario()); // Limpiar el formulario
			} else {
				// Mensaje de error si no se encuentra el usuario
				model.addAttribute("mensajeError", "No se pudo actualizar el usuario. Inténtalo nuevamente.");
			}

			return "modificarusuario";
		}

	}

	@Controller
	public class Usuario2Controller {

		@Autowired
		private IUserRepository usuarioRepository;

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@GetMapping("/eliminarusuario")
		public String showEliminarUsuarioForm(Model model) {
			model.addAttribute("usuario", new Usuario());
			return "eliminarusuario";
		}

		@PostMapping("/buscarUsuarioParaEliminar")
		public String buscarUsuarioParaEliminar(@RequestParam("dni") int dni, Model model) {
			Usuario usuario = usuarioRepository.findByDni(dni);
			if (usuario != null) {
				model.addAttribute("usuario", usuario);
			} else {
				model.addAttribute("mensajeError", "No se encontró ningún usuario con el DNI proporcionado.");
			}
			return "eliminarusuario";
		}

		@PostMapping("/eliminarUsuario")
		public String eliminarUsuario(@RequestParam("dni") int dni, Model model) {
		
			model.addAttribute("mensajeExito", "DNI '" + dni + "' retirado exitosamente.");

			
			return "eliminarusuario";
		}
	}
	
	 @Autowired
	private ProductoService productoService;
	 @Autowired
	 private VentaService ventaService;

	// Método para realizar el pago y registrar la venta
	@PostMapping("/pagar")
	public String procesarPago(Model model, HttpSession session) {
		
		List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
		if (carrito == null || carrito.isEmpty()) {
			model.addAttribute("mensajeAlerta", "El carrito está vacío. No se puede procesar el pago.");
			return "carrito";
		}

		// Calcula el descuento y el costo de envío
		calcularDescuentoYCostoEnvio(model, carrito);

		// Obtiene el usuario actual de la sesión
		Usuario usuario = (Usuario) session.getAttribute("usuario");
        

		if (usuario != null) {
			// Si el usuario está autenticado, crea una nueva instancia de venta y la asocia
			// con el usuario
			Venta nuevaVenta = new Venta();
			nuevaVenta.setMontoTotal((double) model.getAttribute("total"));
			nuevaVenta.setFechaRegistro(new java.sql.Date(System.currentTimeMillis()));
			nuevaVenta.setUsuario(usuario);

			// Guarda la venta en la base de datos
			ventaRepository.save(nuevaVenta);
			
			
			// Asocia cada detalle con la venta guardada
	        for (Detalle detalle : carrito) {
	            detalle.setVenta(nuevaVenta);
	        }

	        // Guarda los detalles en la base de datos
	        detalleRepository.saveAll(carrito);
	        
			// Limpia el carrito después de la compra
			carrito.clear();
			model.addAttribute("carrito", carrito);
			model.addAttribute("total", 0.0);
			model.addAttribute("subtotal", 0.0);
			model.addAttribute("costoEnvio", 0.0);
			model.addAttribute("descuento", 0.0);

			// Redirige al índice después de realizar el pago
			return "redirect:/index";
		} else {
			// Si el usuario no está autenticado, establece un mensaje de alerta en el
			// modelo
			model.addAttribute("mensajeAlerta", "Debes iniciar sesión antes de realizar la compra.");

			// Devuelve la vista del carrito para que el usuario pueda ver el mensaje de
			// alerta
			return "carrito";
		}
	}

	@Controller
	@RequestMapping("/enviarReclamo")
	public class ReclamoController {

		@Autowired
		private IReclamoRepository reclamoRepository;

		@PostMapping
		public String enviarReclamo(
				@RequestParam("nombre") String nombre,
				@RequestParam("apellido") String apellido,
				@RequestParam("dni") String dni,
				@RequestParam("correo") String correo,
				@RequestParam("descripcion") String descripcion,
				Model model) {
			reclamo nuevoReclamo = new reclamo(nombre, apellido, dni, correo, descripcion);
			reclamoRepository.save(nuevoReclamo);

			model.addAttribute("mensaje", "Reclamo enviado correctamente");

			return "reclamoenviado"; // Vuelve a la misma página de formulario y muestra el mensaje de éxito
		}
	}

	@Controller
	public class AdminController {

		@GetMapping("/admin")
		public String mostrarAdmin() {
			return "admin";
		}
	}
	
	
	@GetMapping("/reclamoenviado")
	public String reclamoenviado(Model model, HttpSession session) {
		Usuario usuario = (Usuario) session.getAttribute("usuario");
		if (usuario != null) {
			model.addAttribute("nombreUsuario", usuario.getNombre());
		}

		return "reclamoenviado";
	}
	


	@GetMapping("/pagoexitoso")
	public String pagoexitoso(Model model, HttpSession session) {
		Usuario usuario = (Usuario) session.getAttribute("usuario");
		if (usuario != null) {
			model.addAttribute("nombreUsuario", usuario.getNombre());
		}

		return "pagoexitoso";
	}
	

	@GetMapping("/realizar_pago")
	public String realizar_pago(Model model) {
		return "realizar_pago";
	}

	@Controller
	public class ReporteController {

		@Autowired
		private IVentaRepository ventaRepository;

		private final Logger logger = LoggerFactory.getLogger(ReporteController.class);

		@GetMapping("/reportedeventas")
		public String reportedeventas(Model model) {
			List<Venta> ventas = ventaRepository.findAll();
			// Filtra las ventas con usuario no nulo
			ventas = ventas.stream()
					.filter(v -> v.getUsuario() != null)
					.collect(Collectors.toList());

			double sumaMontoTotalVentas = ventas.stream()
					.mapToDouble(Venta::getMontoTotal)
					.sum();

			model.addAttribute("ventas", ventas);
			model.addAttribute("sumaMontoTotalVentas", sumaMontoTotalVentas);

			return "reportedeventas";
		}

	}

}
