
import java.util.ArrayList;
import java.util.List;

public class Usuario {
    String nombre;
    String password;
    boolean esAdmin; // Atributo para distinguir admins
    List<Cita> citas = new ArrayList<>();

    // Constructor para usuarios normales
    public Usuario(String nombre, String password) {
        this(nombre.trim(), password.trim(), false);
    }

    // Constructor para especificar si es admin
    public Usuario(String nombre, String password, boolean esAdmin) {
        this.nombre = nombre.trim();
        this.password = password.trim();
        this.esAdmin = esAdmin;
    }
}
