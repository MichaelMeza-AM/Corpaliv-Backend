package cl.corpaliv.auth.controller;

import cl.corpaliv.auth.model.Usuario;
import cl.corpaliv.auth.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    public UsuarioController(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository=repository; this.encoder=encoder;
    }
    public record NuevoUsuario(@NotBlank @Email String email, @NotBlank @Size(min=10) String password) {}
    @GetMapping public List<AuthController.Perfil> listar() {
        return repository.findAll().stream().map(AuthController.Perfil::de).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AuthController.Perfil crear(@Valid @RequestBody NuevoUsuario datos) {
        String email=datos.email().trim().toLowerCase();
        if (repository.findByEmailIgnoreCase(email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Correo ya registrado");
        return AuthController.Perfil.de(repository.save(new Usuario(email,encoder.encode(datos.password()))));
    }
    @PatchMapping("/{id}/activo")
    public AuthController.Perfil activar(@PathVariable Long id, @RequestParam boolean valor) {
        Usuario u=repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        u.activo=valor; return AuthController.Perfil.de(repository.save(u));
    }
}
