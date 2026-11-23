package cl.corpaliv.producto.controller;

import cl.corpaliv.producto.model.Producto;
import cl.corpaliv.producto.repository.ProductoRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductoControllerTest {
    @Test void retirarProductoLoOcultaSinPerderDatos() {
        ProductoRepository repo=mock(ProductoRepository.class);
        Producto p=new Producto("Bolsa","Descripción",42000);
        when(repo.findById(4L)).thenReturn(Optional.of(p));
        ProductoController controller=new ProductoController(repo);
        controller.retirar(4L);
        assertFalse(p.activo);
        verify(repo).save(p);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.obtener(4L));
    }
}
