
import java.time.LocalDate;
import java.time.LocalTime;

public class Cita {
    LocalDate fecha;
    LocalTime hora;
    boolean candado;
    String qr;

    boolean notificar;
    int minutosAntes;


    public Cita(LocalDate fecha, LocalTime hora, boolean candado, String qr) {
        this.fecha = fecha;
        this.hora = hora;
        this.candado = candado;
        this.qr = qr;
        this.notificar = false; 
        this.minutosAntes = 10; 

    }
}
