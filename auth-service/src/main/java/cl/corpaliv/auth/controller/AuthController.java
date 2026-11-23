package cl.corpaliv.auth.controller;

import cl.corpaliv.auth.model.Usuario;
import cl.corpaliv.auth.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwt;
    private final String issuer;
    public AuthController(UsuarioRepository repository, PasswordEncoder encoder, JwtEncoder jwt,
        @Value("${corpaliv.jwt-issuer}") String issuer) {
        this.repository=repository; this.encoder=encoder; this.jwt=jwt; this.issuer=issuer;
    }
    public record Credenciales(@NotBlank @Email String email, @NotBlank String password) {}
    public record Sesion(String token, String tipo, long venceEnSegundos) {}
    public record Perfil(Long id, String email, String rol, boolean activo) {
        public static Perfil de(Usuario u) { return new Perfil(u.id,u.email,u.rol,u.activo); }
    }

    @PostMapping("/login")
    public Sesion login(@Valid @RequestBody Credenciales credenciales) {
        Usuario u=repository.findByEmailIgnoreCase(credenciales.email().trim()).orElse(null);
        if (u == null || !u.activo || !encoder.matches(credenciales.password(),u.passwordHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas");
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer(issuer).issuedAt(now)
            .expiresAt(now.plus(8,ChronoUnit.HOURS)).subject(u.email)
            .claim("roles", List.of(u.rol)).build();
        String token=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Sesion(token,"Bearer",28800);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public Perfil perfil(Authentication auth) {
        return repository.findByEmailIgnoreCase(auth.getName()).map(Perfil::de)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
