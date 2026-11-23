package cl.corpaliv.pedido.controller;

import cl.corpaliv.pedido.model.HistorialPedido;
import cl.corpaliv.pedido.model.Pedido;
import cl.corpaliv.pedido.repository.HistorialPedidoRepository;
import cl.corpaliv.pedido.repository.PedidoRepository;
import cl.corpaliv.pedido.service.PedidoService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoRepository repository;
    private final HistorialPedidoRepository historial;
    private final PedidoService service;
    public PedidoController(PedidoRepository repository, HistorialPedidoRepository historial, PedidoService service) {
        this.repository=repository; this.historial=historial; this.service=service;
    }
    public record Entrada(@NotNull @Positive Long productoId, @NotBlank @Size(max=160) String nombreRetira,
        @NotNull @Min(1) @Max(100) Integer cantidad, @NotNull @Positive Long fechaRetiro,
        Boolean envolturaRegalo, @NotBlank @Email String correo, @Size(max=1000) String notas,
        @NotNull Boolean aceptaCondiciones) {}
    public record CambioEstado(@NotBlank String estado, String motivo) {}
    public record Observacion(@NotBlank String texto) {}

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Valid @RequestBody Entrada entrada) { return service.crearPedido(entrada); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping public List<Pedido> listar() { return repository.findAllByOrderByFechaRegistroDesc(); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}") public Pedido obtener(@PathVariable Long id) { return service.obtenerPedido(id); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/historial")
    public List<HistorialPedido> historial(@PathVariable Long id) {
        service.obtenerPedido(id);
        return historial.findByPedidoIdOrderByFechaRegistroAsc(id);
    }
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/estado")
    public Pedido estado(@PathVariable Long id, @Valid @RequestBody CambioEstado entrada) {
        return service.cambiarEstado(id, entrada.estado(), entrada.motivo());
    }
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/observaciones") @ResponseStatus(HttpStatus.CREATED)
    public HistorialPedido observacion(@PathVariable Long id, @Valid @RequestBody Observacion entrada) {
        return service.observar(id, entrada.texto());
    }
}
