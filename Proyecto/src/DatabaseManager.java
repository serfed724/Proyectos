
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection conn;

public Connection getConnection() {
    return conn;
}


public DatabaseManager(String dbPath) {
    try {
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTables();        // Crea tablas si no existen
        verificarColumnas();   
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private void createTables() {
    String usuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "nombre TEXT UNIQUE NOT NULL," +
            "password TEXT NOT NULL," +
            "es_admin INTEGER NOT NULL);";

    String citas = "CREATE TABLE IF NOT EXISTS citas (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "usuario_id INTEGER NOT NULL," +
            "fecha TEXT NOT NULL," +
            "hora TEXT NOT NULL," +
            "candado INTEGER NOT NULL," +
            "qr TEXT NOT NULL," +
            "notificar INTEGER NOT NULL," +     
            "minutosAntes INTEGER NOT NULL," +    
            "FOREIGN KEY(usuario_id) REFERENCES usuarios(id));";

    String asistencias = "CREATE TABLE IF NOT EXISTS asistencias (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "usuario_id INTEGER NOT NULL," +
            "entrada TEXT NOT NULL," +
            "salida TEXT NOT NULL," +
            "FOREIGN KEY(usuario_id) REFERENCES usuarios(id));";

    String bloqueos = "CREATE TABLE IF NOT EXISTS bloqueos (" +
            "fecha TEXT PRIMARY KEY);";

    
    String configuracion = "CREATE TABLE IF NOT EXISTS configuracion (" +
        "clave TEXT PRIMARY KEY," +
        "valor INTEGER NOT NULL);";
        

    try (Statement stmt = conn.createStatement()) {
        stmt.execute(usuarios);
        stmt.execute(citas);
        stmt.execute(asistencias);
        stmt.execute(bloqueos);
        stmt.execute(configuracion);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    // Métodos CRUD básicos
    public void insertUsuario(String nombre, String password, boolean esAdmin) {
        String sql = "INSERT INTO usuarios(nombre, password, es_admin) VALUES(?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, password);
            pstmt.setInt(3, esAdmin ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean existeUsuario(String nombre) {
        String sql = "SELECT 1 FROM usuarios WHERE nombre=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

private void verificarColumnas() {
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("PRAGMA table_info(citas)")) {

        boolean tieneNotificar = false;
        boolean tieneMinutosAntes = false;

        while (rs.next()) {
            String col = rs.getString("name");
            if ("notificar".equals(col)) tieneNotificar = true;
            if ("minutosAntes".equals(col)) tieneMinutosAntes = true;
        }

        if (!tieneNotificar) {
            stmt.execute("ALTER TABLE citas ADD COLUMN notificar INTEGER DEFAULT 1");
            System.out.println("[INFO] Columna 'notificar' agregada.");
        }
        if (!tieneMinutosAntes) {
            stmt.execute("ALTER TABLE citas ADD COLUMN minutosAntes INTEGER DEFAULT 15");
            System.out.println("[INFO] Columna 'minutosAntes' agregada.");
        }

        //  Verificar tabla configuracion
        stmt.execute("CREATE TABLE IF NOT EXISTS configuracion (clave TEXT PRIMARY KEY, valor INTEGER NOT NULL)");

        //  Insertar valor inicial para candadosTotales si no existe
        ResultSet rsConfig = stmt.executeQuery("SELECT 1 FROM configuracion WHERE clave='candadosTotales'");
        if (!rsConfig.next()) {
            stmt.execute("INSERT INTO configuracion (clave, valor) VALUES ('candadosTotales', 80)");
            System.out.println("[INFO] Valor inicial de candadosTotales insertado.");
        }

    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo en migración de columnas: " + e.getMessage());
    }
}


public int getCandadosTotales() {
    String sql = "SELECT valor FROM configuracion WHERE clave='candadosTotales'";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) return rs.getInt("valor");
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al obtener candadosTotales: " + e.getMessage());
    }
    return 80; // Valor por defecto
}

public void updateCandadosTotales(int nuevoValor) {
    String sql = "UPDATE configuracion SET valor=? WHERE clave='candadosTotales'";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, nuevoValor);
        pstmt.executeUpdate();
        System.out.println("[INFO] Stock de candados actualizado: " + nuevoValor);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al actualizar candadosTotales: " + e.getMessage());
    }
}


public void insertCita(int usuarioId, String fecha, String hora, boolean candado, String qr, boolean notificar, int minutosAntes) {
    String sql = "INSERT INTO citas(usuario_id, fecha, hora, candado, qr, notificar, minutosAntes) VALUES(?,?,?,?,?,?,?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, usuarioId);
        pstmt.setString(2, fecha);
        pstmt.setString(3, hora);
        pstmt.setInt(4, candado ? 1 : 0);
        pstmt.setString(5, qr);
        pstmt.setInt(6, notificar ? 1 : 0);
        pstmt.setInt(7, minutosAntes);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void insertBloqueo(String fecha) {
    String checkSql = "SELECT 1 FROM bloqueos WHERE fecha=?";
    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setString(1, fecha);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            System.out.println("[INFO] El día " + fecha + " ya está bloqueado. No se inserta duplicado.");
            return;
        }
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al verificar bloqueo: " + e.getMessage());
    }

    String sql = "INSERT INTO bloqueos(fecha) VALUES(?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, fecha);
        pstmt.executeUpdate();
        System.out.println("[INFO] Bloqueo insertado para fecha: " + fecha);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al insertar bloqueo: " + e.getMessage());
    }
}


public boolean existeCitaParaUsuarioEnFecha(int usuarioId, String fecha) {
    String sql = "SELECT COUNT(*) FROM citas WHERE usuario_id = ? AND fecha = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, usuarioId);
        pstmt.setString(2, fecha);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
    } catch (SQLException e) {
        System.err.println("[ERROR] Verificando cita existente: " + e.getMessage());
    }
    return false;
}



public List<java.time.LocalDate> getBloqueosSemana() {
    List<java.time.LocalDate> bloqueos = new ArrayList<>();
    String sql = "SELECT fecha FROM bloqueos";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            bloqueos.add(java.time.LocalDate.parse(rs.getString("fecha")));
        }
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al obtener bloqueos: " + e.getMessage());
    }
    return bloqueos;
}


public int contarReservasSemana() {
    int count = 0;
    String sql = "SELECT COUNT(*) FROM citas WHERE fecha BETWEEN ? AND ?";
    LocalDate hoy = LocalDate.now();
    LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
    LocalDate domingo = hoy.with(java.time.DayOfWeek.SUNDAY);
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, lunes.toString());
        pstmt.setString(2, domingo.toString());
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) count = rs.getInt(1);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al contar reservas: " + e.getMessage());
    }
    return count;
}



public int contarUsuariosDentro() {
    int count = 0;
    String sql = "SELECT COUNT(*) FROM asistencias WHERE entrada <= ? AND salida >= ?";
    //  Formato compatible con SQLite
    String now = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, now);
        pstmt.setString(2, now);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) count = rs.getInt(1);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al contar usuarios dentro: " + e.getMessage());
    }
    return count;
}


public void insertAsistencia(int usuarioId, java.time.LocalDateTime entrada, java.time.LocalDateTime salida) {
    //  Formato compatible con SQLite
    String now = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    // Verificar si ya existe asistencia activa
    String checkSql = "SELECT 1 FROM asistencias WHERE usuario_id=? AND entrada <= ? AND salida >= ?";
    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setInt(1, usuarioId);
        checkStmt.setString(2, now);
        checkStmt.setString(3, now);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            System.out.println("[INFO] Usuario ya tiene asistencia activa. No se inserta duplicado.");
            return;
        }
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al verificar asistencia activa: " + e.getMessage());
    }

    // Insertar nueva asistencia si no hay activa
    String sql = "INSERT INTO asistencias(usuario_id, entrada, salida) VALUES(?,?,?)";
    String entradaStr = entrada.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String salidaStr = salida.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, usuarioId);
        pstmt.setString(2, entradaStr);
        pstmt.setString(3, salidaStr);
        pstmt.executeUpdate();
        System.out.println("[INFO] Asistencia insertada para usuario ID: " + usuarioId);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al insertar asistencia: " + e.getMessage());
    }
}



public List<Asistencia> getAsistenciasActivas() {
    List<Asistencia> lista = new ArrayList<>();
    String sql = "SELECT u.nombre, entrada, salida FROM asistencias a JOIN usuarios u ON a.usuario_id = u.id";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            String usuario = rs.getString("nombre");
            LocalDateTime entrada = LocalDateTime.parse(rs.getString("entrada").replace(" ", "T"));
            LocalDateTime salida = LocalDateTime.parse(rs.getString("salida").replace(" ", "T"));
            lista.add(new Asistencia(usuario, entrada, salida));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return lista;
}


public void eliminarCitaPorId(int citaId) {
    String sql = "DELETE FROM citas WHERE id=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, citaId);
        pstmt.executeUpdate();
        System.out.println("[INFO] Cita eliminada en DB con ID: " + citaId);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar cita: " + e.getMessage());
    }
}



public void eliminarCitasVencidas() {
    String sql = "DELETE FROM citas WHERE datetime(fecha || ' ' || hora) <= ?";
    //  Formato compatible con SQLite
    String now = java.time.LocalDateTime.now()
        .minusHours(2)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, now);
        int rows = pstmt.executeUpdate();
        System.out.println("[INFO] Citas vencidas eliminadas: " + rows);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar citas vencidas: " + e.getMessage());
    }
}

    
// Obtener el ID del usuario por nombre
public int getUsuarioId(String nombre) {
    String sql = "SELECT id FROM usuarios WHERE nombre=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombre);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1; // No encontrado
}

// Obtener un usuario completo
public Usuario getUsuario(String nombre) {
    String sql = "SELECT nombre, password, es_admin FROM usuarios WHERE nombre=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombre);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            String pass = rs.getString("password");
            boolean esAdmin = rs.getInt("es_admin") == 1;
            return new Usuario(nombre, pass, esAdmin);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return null;
}

// Obtener todas las citas de un usuario
public List<Cita> getCitasPorUsuario(String nombreUsuario) {
    List<Cita> lista = new ArrayList<>();
    int usuarioId = getUsuarioId(nombreUsuario);
    String sql = "SELECT fecha, hora, candado, qr, notificar, minutosAntes FROM citas WHERE usuario_id=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, usuarioId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            java.time.LocalDate fecha = java.time.LocalDate.parse(rs.getString("fecha"));
            java.time.LocalTime hora = java.time.LocalTime.parse(rs.getString("hora"));
            boolean candado = rs.getInt("candado") == 1;
            String qr = rs.getString("qr");
            boolean notificar = rs.getInt("notificar") == 1;
            int minutosAntes = rs.getInt("minutosAntes");

            Cita cita = new Cita(fecha, hora, candado, qr);
            cita.notificar = notificar;
            cita.minutosAntes = minutosAntes;

            lista.add(cita);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return lista;
}


// Buscar cita por QR
public Cita getCitaPorQR(String qr) {
    String sql = "SELECT fecha, hora, candado, qr FROM citas WHERE qr=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, qr);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            java.time.LocalDate fecha = java.time.LocalDate.parse(rs.getString("fecha"));
            java.time.LocalTime hora = java.time.LocalTime.parse(rs.getString("hora"));
            boolean candado = rs.getInt("candado") == 1;
            return new Cita(fecha, hora, candado, qr);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return null;
}

// Actualizar cita 

public void updateCita(int citaId, String nuevaFecha, String nuevaHora, String nuevoQR, boolean candado, boolean notificar, int minutosAntes) {
    String sql = "UPDATE citas SET fecha=?, hora=?, qr=?, candado=?, notificar=?, minutosAntes=? WHERE id=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nuevaFecha);
        pstmt.setString(2, nuevaHora);
        pstmt.setString(3, nuevoQR);
        pstmt.setInt(4, candado ? 1 : 0);
        pstmt.setInt(5, notificar ? 1 : 0);
        pstmt.setInt(6, minutosAntes);
        pstmt.setInt(7, citaId);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void eliminarBloqueo(String fecha) {
    String sql = "DELETE FROM bloqueos WHERE fecha=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, fecha);
        pstmt.executeUpdate();
        System.out.println("[INFO] Bloqueo eliminado de DB: " + fecha);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar bloqueo: " + e.getMessage());
    }
}

public int getCitaIdPorQR(String qr) {
    String sql = "SELECT id FROM citas WHERE qr=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, qr);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1; // No encontrado
}

public boolean existeUsuarioConOtraPassword(String nombre, String password) {
    String sql = "SELECT password FROM usuarios WHERE nombre=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombre);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            String passDB = rs.getString("password");
            return !passDB.equals(password); // true si la contraseña es distinta
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}

public void eliminarAsistenciasPorFecha(LocalDate fecha) {
    String sql = "DELETE FROM asistencias WHERE date(entrada) = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, fecha.toString());
        pstmt.executeUpdate();
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar asistencias por fecha: " + e.getMessage());
    }
}

public void eliminarAsistenciasSinCita() {
    String sql = "DELETE FROM asistencias WHERE usuario_id NOT IN (SELECT usuario_id FROM citas)";
    try (Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(sql);
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar asistencias sin cita: " + e.getMessage());
    }
}


public int eliminarCitasFueraDeSemana(LocalDate lunesActual, LocalDate hoy, LocalTime ahora) {
    String sql = "DELETE FROM citas WHERE date(fecha) < ? OR (fecha = ? AND hora < ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, lunesActual.toString());
        pstmt.setString(2, hoy.toString());
        pstmt.setString(3, ahora.toString());
        int rows = pstmt.executeUpdate();
        System.out.println("[INFO] Citas eliminadas en DB: " + rows);
        return rows;
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar citas vencidas en DB: " + e.getMessage());
    }
    return 0;
}


public int eliminarCitasCompletadasAntiguas(LocalDate limite) {
    String sql = "DELETE FROM citas WHERE completada = 1 AND date(fecha) < ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, limite.toString());
        int rows = pstmt.executeUpdate();
        System.out.println("[INFO] Citas completadas eliminadas: " + rows);
        return rows;
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al eliminar citas completadas: " + e.getMessage());
    }
    return 0;
}


public void marcarCitaCompletada(int citaId) {
    String sql = "UPDATE citas SET completada = 1 WHERE id = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, citaId);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al marcar cita completada: " + e.getMessage());
    }
}


public boolean estaCitaCompletada(int citaId) {
    String sql = "SELECT completada FROM citas WHERE id=?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, citaId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) return rs.getInt("completada") == 1;
    } catch (SQLException e) {
        System.err.println("[ERROR] Fallo al verificar si la cita está completada: " + e.getMessage());
    }
    return false;
}

//Cita eliminada


}
