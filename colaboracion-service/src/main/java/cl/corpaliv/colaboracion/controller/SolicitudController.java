package cl.corpaliv.colaboracion.controller;

import cl.corpaliv.colaboracion.model.HistorialSolicitud;
import cl.corpaliv.colaboracion.model.Solicitud;
import cl.corpaliv.colaboracion.repository.HistorialSolicitudRepository;
import cl.corpaliv.colaboracion.repository.SolicitudRepository;
import cl.corpaliv.colaboracion.service.ColaboracionService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    private final SolicitudRepository repository;
    private final HistorialSolicitudRepository historial;
    private final ColaboracionService service;
    public SolicitudController(SolicitudRepository repository, HistorialSolicitudRepository historial, ColaboracionService service) {
        this.repository=repository; this.historial=historial; this.service=service;
    }
    public record Entrada(@NotBlank String tipo, @NotBlank @Size(max=160) String nombre,
        @NotBlank @Email String correo, @Size(max=32) String telefono,
        @Size(max=160) String organizacion, @Size(max=160) String cargo,
        @Size(max=2000) String detalleColaboracion, Boolean notificaciones,
        String donanteTipo, Long montoCLP, Boolean anonimo, @NotNull Boolean aceptaPrivacidad) {}
    public record CambioEstado(@NotBlank String estado, String motivo) {}
    public record Observacion(@NotBlank String texto) {}

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Solicitud crear(@Valid @RequestBody Entrada entrada) { return service.crearSolicitud(entrada); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping public List<Solicitud> listar() { return repository.findAllByOrderByFechaRegistroDesc(); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}") public Solicitud obtener(@PathVariable Long id) { return service.obtenerSolicitud(id); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/historial") public List<HistorialSolicitud> historial(@PathVariable Long id) {
        service.obtenerSolicitud(id); return historial.findBySolicitudIdOrderByFechaRegistroAsc(id);
    }
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/estado")
    public Solicitud estado(@PathVariable Long id, @Valid @RequestBody CambioEstado entrada) {
        return service.cambiarEstado(id,entrada.estado(),entrada.motivo());
    }
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/observaciones") @ResponseStatus(HttpStatus.CREATED)
    public HistorialSolicitud observacion(@PathVariable Long id, @Valid @RequestBody Observacion entrada) {
        return service.observar(id,entrada.texto());
    }
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminarColaboracion(id); }
}
