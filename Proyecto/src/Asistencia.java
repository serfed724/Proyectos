
import java.time.LocalDateTime;

public class Asistencia {
    String usuario;
    LocalDateTime entrada;
    LocalDateTime salida;

    public Asistencia(String usuario, LocalDateTime entrada, LocalDateTime salida) {
        this.usuario = usuario;
        this.entrada = entrada;
        this.salida = salida;
    }

    // validar que el tiempo actual esté entre entrada y salida
    public boolean estaDentro(LocalDateTime now) {
        return now.isAfter(entrada) && now.isBefore(salida);
    }
}
