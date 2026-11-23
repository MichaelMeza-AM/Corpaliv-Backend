package cl.corpaliv.colaboracion.service;

import cl.corpaliv.colaboracion.controller.SolicitudController;
import cl.corpaliv.colaboracion.model.HistorialSolicitud;
import cl.corpaliv.colaboracion.model.Solicitud;
import cl.corpaliv.colaboracion.repository.HistorialSolicitudRepository;
import cl.corpaliv.colaboracion.repository.SolicitudRepository;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ColaboracionService {
    private final SolicitudRepository solicitudes;
    private final HistorialSolicitudRepository historial;
    public ColaboracionService(SolicitudRepository solicitudes, HistorialSolicitudRepository historial) {
        this.solicitudes=solicitudes; this.historial=historial;
    }

    @Transactional
    public Solicitud crearSolicitud(SolicitudController.Entrada d) {
        String tipo=d.tipo().trim().toUpperCase();
        if (!List.of("DONACION","ALIANZA","CAPACITACION").contains(tipo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo no permitido");
        if (!Boolean.TRUE.equals(d.aceptaPrivacidad()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Debe aceptar la política de privacidad");
        if (tipo.equals("DONACION") && (d.montoCLP()==null || d.montoCLP()<=0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El aporte requiere monto positivo");
        if (!tipo.equals("DONACION") && (d.organizacion()==null || d.organizacion().isBlank()
            || d.detalleColaboracion()==null || d.detalleColaboracion().trim().length()<10))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Organización y detalle de al menos 10 caracteres requeridos");
        String descripcion=switch(tipo) {
            case "DONACION" -> "Aporte solidario";
            case "ALIANZA" -> "Programa de Inclusión Laboral";
            default -> "Capacitación en diversidad, inclusión y buen trato";
        };
        Solicitud s=new Solicitud(tipo,d.nombre().trim(),descripcion,d.correo().trim());
        s.telefono=limpiar(d.telefono()); s.organizacion=limpiar(d.organizacion());
        s.cargo=limpiar(d.cargo()); s.detalleColaboracion=limpiar(d.detalleColaboracion());
        s.notificaciones=Boolean.TRUE.equals(d.notificaciones());
        if (tipo.equals("DONACION")) {
            s.donanteTipo=d.donanteTipo(); s.montoCLP=d.montoCLP(); s.anonimo=Boolean.TRUE.equals(d.anonimo());
        }
        return solicitudes.save(s);
    }

    private String limpiar(String valor) { return valor==null || valor.isBlank()?null:valor.trim(); }

    @Transactional
    public Solicitud cambiarEstado(Long id, String nuevo, String motivo) {
        Solicitud s=obtenerSolicitud(id);
        if (!List.of("RECIBIDA","EN_REVISION","CONTACTADO","CONCRETADO","DESCARTADO").contains(nuevo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado no permitido");
        if (s.estado.equals(nuevo)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya tiene ese estado");
        if (motivo!=null && motivo.length()>2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Motivo demasiado largo");
        String anterior=s.estado; s.estado=nuevo;
        historial.save(new HistorialSolicitud(id,"CAMBIO_ESTADO",motivo==null?"":motivo.trim(),anterior,nuevo));
        return solicitudes.save(s);
    }

    @Transactional
    public HistorialSolicitud observar(Long id, String texto) {
        obtenerSolicitud(id);
        if (texto==null || texto.isBlank() || texto.length()>2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Observación inválida");
        return historial.save(new HistorialSolicitud(id,"OBSERVACION",texto.trim(),null,null));
    }

    public Solicitud obtenerSolicitud(Long id) {
        return solicitudes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void eliminarColaboracion(Long id) {
        Solicitud s=obtenerSolicitud(id);
        if (!(s.tipo.equals("ALIANZA") || s.tipo.equals("CAPACITACION")) || !s.estado.equals("RECIBIDA"))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se elimina una colaboración recibida");
        solicitudes.delete(s); // FK ON DELETE CASCADE elimina el historial de la misma solicitud.
    }
}
