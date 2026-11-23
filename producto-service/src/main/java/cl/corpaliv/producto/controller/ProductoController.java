package cl.corpaliv.producto.controller;

import cl.corpaliv.producto.model.Producto;
import cl.corpaliv.producto.repository.ProductoRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoRepository repository;
    public ProductoController(ProductoRepository repository) { this.repository = repository; }

    public record ProductoDatos(@NotBlank @Size(max=160) String nombre,
        @NotBlank @Size(max=1000) String descripcion, @NotNull @Min(1) Integer precioCLP) {}

    @GetMapping
    public List<Producto> listar() { return repository.findByActivoTrueOrderByIdAsc(); }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        Producto p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!p.activo) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p;
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody ProductoDatos datos) {
        return repository.save(new Producto(datos.nombre(), datos.descripcion(), datos.precioCLP()));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoDatos datos) {
        Producto p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.nombre = datos.nombre(); p.descripcion = datos.descripcion(); p.precioCLP = datos.precioCLP();
        return repository.save(p);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable Long id) {
        Producto p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.activo = false; repository.save(p); // Histórico de pedidos conserva nombre y precio.
    }
}
