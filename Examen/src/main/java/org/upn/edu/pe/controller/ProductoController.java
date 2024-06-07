package org.upn.edu.pe.controller;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.upn.edu.pe.model.Detalle;
import org.upn.edu.pe.model.Producto;
import org.upn.edu.pe.model.Usuario;
import org.upn.edu.pe.model.UsuarioService;
import org.upn.edu.pe.model.Venta;
import org.upn.edu.pe.repository.IDetalleRepository;
import org.upn.edu.pe.repository.IProductoRepository;
import org.upn.edu.pe.repository.IUserRepository;
import org.upn.edu.pe.repository.IVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes({"carrito","total","subtotal","costoEnvio","descuento"})
public class ProductoController {
	
	// Inicializacion del objeto carrito
	@ModelAttribute("carrito")
	public List<Detalle> getCarrito(){
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
	
	// Declaracion e Inicializacion de objetos para el control del carrito de compras
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
	            if (!existe) carrito.add(detalle);
	        }

	        for (Detalle d : carrito) subtotal += d.getSubtotal();

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
	 public String calcularDescuentoYCostoEnvio(Model model) {
        List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
        double subtotal = 0.0;

        for (Detalle d : carrito) {
            subtotal += d.getSubtotal();
        }

        double costoEnvio = 0.05 * subtotal;
        double descuento = 0;
        if (subtotal >= 200) {
            descuento = 20;
            costoEnvio = 50;
        } 
        if (subtotal >=60) {
        	costoEnvio = 20;
        }
        else {
            costoEnvio = 10;
        }

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("costoEnvio", costoEnvio);
        model.addAttribute("descuento", descuento);

        double total = subtotal - descuento + costoEnvio;
        model.addAttribute("total", total);

        return "redirect:/carrito";
    }


	// Método para eliminar un producto del carrito
	@PostMapping("/eliminar")
	public String eliminarProducto(Model model, HttpSession session, @RequestParam(name = "idProducto", required = true) int idProducto, @RequestParam(name = "cantidadEliminar", required = true) int cantidadEliminar) {
	    List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");
	    if (carrito != null) {
	        double subtotal = 0.0;

	        for (Iterator<Detalle> iterator = carrito.iterator(); iterator.hasNext();) {
	            Detalle detalle = iterator.next();
	            if (detalle.getProducto().getIdProducto() == idProducto) {
	                int cantidadActual = detalle.getCantidad();
	                if (cantidadEliminar >= cantidadActual) {
	                    iterator.remove(); // Eliminar el producto completo
	                } else {
	                    detalle.setCantidad(cantidadActual - cantidadEliminar); // Reducir la cantidad del producto
	                }
	            }
	            subtotal += detalle.getSubtotal();
	        }

	        // Calcula el subtotal, el costo de envío, el descuento y el total
	        double costoEnvio = 0;
	        double descuento = 0;
	        if (subtotal > 2000) {
	            descuento = 0;
	        }
	        double total = subtotal - descuento + costoEnvio;

	        // Actualiza los atributos del modelo
	        model.addAttribute("subtotal", subtotal);
	        model.addAttribute("total", total);
	        model.addAttribute("descuento", descuento);
	        model.addAttribute("carrito", carrito);
	        model.addAttribute("costoEnvio", costoEnvio);
	    }

	    return "redirect:/carrito";
	}


	
	// Método para visualizar el carrito de compras
	@GetMapping("/carrito")
	public String carrito(Model model, HttpSession session) {
		
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
	    public String loginUser(@RequestParam("dni") int dni, @RequestParam("contrase") String contrase, HttpSession session, Model model) {
	        Usuario usuario = usuarioService.findByDNIAndContrase(dni, contrase);
	        if (usuario != null && usuario.getContrase().equals(contrase)) {
	            session.setAttribute("usuario", usuario);
	            return "redirect:/index";
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
	        // Aquí se podría agregar la lógica para codificar la contraseña si fuera necesario
	        // usuario.setContrase(passwordEncoder.encode(usuario.getContrase()));
	        usuarioRepository.save(usuario);
	        return "redirect:/login";
	    }
	}
	
	// Método para realizar el pago y registrar la venta
	@GetMapping("/pagar")
	public String procesarPago(Model model, HttpSession session) {
	    List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
	    double total = (double) model.getAttribute("total");

	    // Calcula el descuento y el costo de envío
	    calcularDescuentoYCostoEnvio(model);

	    // Obtiene el usuario actual de la sesión
	    Usuario usuario = (Usuario) session.getAttribute("usuario");

	    if (usuario != null) {
	        // Si el usuario está autenticado, crea una nueva instancia de venta y la asocia con el usuario
	        Venta nuevaVenta = new Venta();
	        nuevaVenta.setMontoTotal(total);
	        nuevaVenta.setFechaRegistro(new java.sql.Date(System.currentTimeMillis()));
	        nuevaVenta.setUsuario(usuario);

	        // Guarda la venta en la base de datos
	        ventaRepository.save(nuevaVenta);

	        // Limpia el carrito después de la compra
	        carrito.clear();
	        model.addAttribute("carrito", carrito);
	        model.addAttribute("total", 0.0);

	        // Redirige al índice después de realizar el pago
	        return "redirect:/index";
	    } else {
	        // Si el usuario no está autenticado, establece un mensaje de alerta en el modelo
	        model.addAttribute("mensajeAlerta", "Debes iniciar sesión antes de realizar la compra.");

	        // Devuelve la vista del carrito para que el usuario pueda ver el mensaje de alerta
	        return "carrito";
	    }
	}

	
}
