package cl.corpaliv.pedido.service;

import cl.corpaliv.pedido.controller.PedidoController;
import cl.corpaliv.pedido.model.HistorialPedido;
import cl.corpaliv.pedido.model.Pedido;
import cl.corpaliv.pedido.repository.HistorialPedidoRepository;
import cl.corpaliv.pedido.repository.PedidoRepository;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final HistorialPedidoRepository historial;
    private final RestClient productos;

    public PedidoService(PedidoRepository pedidos, HistorialPedidoRepository historial,
                         @Value("${corpaliv.producto-url}") String productoUrl) {
        this.pedidos = pedidos;
        this.historial = historial;
        this.productos = RestClient.create(productoUrl);
    }

    record ProductoRemoto(Long id, String nombre, Integer precioCLP, boolean activo) {}

    @Transactional
    public Pedido crearPedido(PedidoController.Entrada d) {
        if (!Boolean.TRUE.equals(d.aceptaCondiciones()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe aceptar las condiciones");
        ProductoRemoto producto;
        try {
            producto = productos.get().uri("/api/productos/{id}", d.productoId())
                .retrieve().body(ProductoRemoto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto no disponible");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible", e);
        }
        if (producto == null || !d.productoId().equals(producto.id()) || !producto.activo()
            || producto.nombre() == null || producto.nombre().isBlank()
            || producto.precioCLP() == null || producto.precioCLP() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto no disponible");

        return pedidos.save(new Pedido(producto.id(), producto.nombre(), producto.precioCLP(),
            d.nombreRetira().trim(), d.cantidad(), d.fechaRetiro(), Boolean.TRUE.equals(d.envolturaRegalo()),
            d.correo().trim(), d.notas() == null || d.notas().isBlank() ? null : d.notas().trim()));
    }

    public Pedido obtenerPedido(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Pedido cambiarEstado(Long id, String nuevo, String motivo) {
        Pedido p = obtenerPedido(id);
        if (!List.of("RECIBIDA", "EN_REVISION", "CONTACTADO", "CONCRETADO", "DESCARTADO").contains(nuevo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no permitido");
        if (p.estado.equals(nuevo)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tiene ese estado");
        if (motivo != null && motivo.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo demasiado largo");
        String anterior = p.estado;
        p.estado = nuevo;
        historial.save(new HistorialPedido(id, "CAMBIO_ESTADO", motivo == null ? "" : motivo.trim(), anterior, nuevo));
        return pedidos.save(p);
    }

    @Transactional
    public HistorialPedido observar(Long id, String texto) {
        obtenerPedido(id);
        if (texto == null || texto.isBlank() || texto.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Observación inválida");
        return historial.save(new HistorialPedido(id, "OBSERVACION", texto.trim(), null, null));
    }
}
