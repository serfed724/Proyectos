
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Iterator;


public class Gimnasio {

    protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    protected int toleranciaMinutos = 10;
    protected int duracionAsistenciaMinutos = 120;
    

    List<Usuario> usuarios = new ArrayList<>();
    List<Cita> citas = new ArrayList<>();
    List<java.time.LocalDate> bloqueos = new ArrayList<>();
    List<Asistencia> asistencias = new ArrayList<>();
    int candadosTotales = 80;

    public boolean existeUsuario(String nombre) {
        for (Usuario u : usuarios) {
            if (u.nombre.equalsIgnoreCase(nombre)) return true;
        }
        return false;
    }

  
public void setToleranciaMinutos(int minutos) {
    if (minutos < 0) {
        throw new IllegalArgumentException("La tolerancia no puede ser negativa");
    }
    this.toleranciaMinutos = minutos;
    System.out.println("[INFO] Tolerancia de QR establecida en ±" + minutos + " minutos.");
}


public int getToleranciaMinutos() {
    return this.toleranciaMinutos;
}


public void setDuracionAsistenciaMinutos(int minutos) {
    if (minutos <= 0) {
        throw new IllegalArgumentException("La duración debe ser mayor a cero minutos.");
    }
    this.duracionAsistenciaMinutos = minutos;
    System.out.println("[INFO] Duración de asistencia establecida en " + minutos + " minutos.");
}

public int getDuracionAsistenciaMinutos() {
    return this.duracionAsistenciaMinutos;
}



 // Devolver candado (memoria)
    public void devolverCandado() {
        candadosTotales++;
        System.out.println("[MEM] Candado devuelto. Stock actual: " + candadosTotales);
    }

public void procesarSalidasYDevolverCandados() {
    // Comportamiento base: sólo limpia asistencias vencidas en memoria
    // y devuelve candados de esas asistencias
    limpiarAsistenciasVencidas();
}





private void programarNotificacion(Cita cita, Usuario usuario) {
    if (!cita.notificar) return;

    long delay = java.time.Duration.between(
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.of(cita.fecha, cita.hora).minusMinutes(cita.minutosAntes)
    ).toMillis();

    if (delay > 0) {
        scheduler.schedule(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                int opt = javax.swing.JOptionPane.showOptionDialog(
                    null,
                    "Aviso para " + usuario.nombre + "\n" +
                    "Tu cita es a las " + cita.hora + " en " + cita.fecha,
                    "Recordatorio",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[]{"Ir al menú"},
                    "Ir al menú"
                );

                if (opt == 0) {
                    new UI(this).abrirMenuUsuario(usuario); // Usamos this en lugar de gym
                }
            });
        }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
    public Usuario registrar(String nombre, String pass) {
        nombre = nombre.trim();
        pass = pass.trim();
        if (existeUsuario(nombre)) return null;
        Usuario u = new Usuario(nombre, pass, false);
        usuarios.add(u);
        return u;
    }

    public Usuario login(String nombre, String pass) {
        nombre = nombre.trim();
        pass = pass.trim();
        for (Usuario u : usuarios) {
            if (u.nombre.equals(nombre) && u.password.equals(pass)) return u;
        }
        return null;
    }

  

public Cita agendar(Usuario u, LocalDate fecha, LocalTime hora, boolean candado) {
    if (bloqueos.contains(fecha)) throw new RuntimeException("Día bloqueado");

    if (fecha.equals(LocalDate.now()) && hora.isBefore(LocalTime.now())) {
        throw new RuntimeException("No se puede agendar en una hora que ya pasó");
    }

    // Validar duplicado exacto (fecha + hora)
    for (Cita existente : u.citas) {
        if (existente.fecha.equals(fecha) && existente.hora.equals(hora)) {
            throw new RuntimeException("Ya tienes una cita para esa fecha y hora");
        }
    }

    
    
    if (candado) {
        if (candadosTotales <= 0) {
            javax.swing.JOptionPane.showMessageDialog(null,
            "No hay candados disponibles, la cita se agendará sin candado.");
            candado = false; // Se agenda sin candado
        } else {
        candadosTotales--; // Reservar candado
        }
    }


    String qr = java.util.UUID.randomUUID().toString();
    Cita c = new Cita(fecha, hora, candado, qr);
    u.citas.add(c);
    citas.add(c);
    programarNotificacion(c, u);
    return c;
}


    // Reagendar: cambia hora del día elegido y regenera QR

public Cita reagendarDia(Usuario u, LocalDate fecha, String nuevaHoraStr, boolean candadoSolicitado) {
    // Validar si el día está bloqueado
    if (bloqueos.contains(fecha)) {
        throw new RuntimeException("No se puede reagendar: el día está bloqueado");
    }

    LocalTime nuevaHora = LocalTime.parse(nuevaHoraStr);

    // Validar hora pasada (para reagendar y crear nueva)
    if (fecha.equals(LocalDate.now()) && nuevaHora.isBefore(LocalTime.now())) {
        throw new RuntimeException("No se puede agendar en una hora que ya pasó");
    }

    // Buscar cita existente
    for (Cita c : u.citas) {
        if (c.fecha.equals(fecha)) {
            // Actualizar hora y candado
            c.hora = nuevaHora;
            if (candadoSolicitado && !c.candado) {
                if (candadosTotales <= 0) throw new RuntimeException("No hay candados disponibles para reagendar");
                candadosTotales--;
                c.candado = true;
            } else if (!candadoSolicitado && c.candado) {
                candadosTotales++;
                c.candado = false;
            }

            // Regenerar QR
            c.qr = java.util.UUID.randomUUID().toString();

            // Programar notificación si aplica
            programarNotificacion(c, u);
            return c;
        }
    }

    //  Si no existe, crear nueva cita
    return agendar(u, fecha, nuevaHora, candadoSolicitado);
}


    
public void bloquearDia(LocalDate fecha) {
    int candadosDevueltos = 0;

    // Contar candados en la lista global (cada cita aparece solo una vez aquí)
    for (Cita c : citas) {
        if (c.fecha.equals(fecha) && c.candado) {
            candadosDevueltos++;
        }
    }

    // Eliminar citas globales y por usuario
    citas.removeIf(c -> c.fecha.equals(fecha));
    for (Usuario u : usuarios) {
        u.citas.removeIf(c -> c.fecha.equals(fecha));
    }

    // Actualizar stock
    candadosTotales += candadosDevueltos;
    bloqueos.add(fecha);
}


    private String buscarNombreUsuarioPorCita(Cita cita) {
        for (Usuario u : usuarios) {
            if (u.citas.contains(cita)) return u.nombre;
        }
        return "desconocido";
    }

    public int usuariosDentro() {
        int count = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Asistencia a : asistencias) if (a.estaDentro(now)) count++;
        return count;
    }

    public int reservasSemana() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate domingo = hoy.with(java.time.DayOfWeek.SUNDAY);
        int count = 0;
        for (Cita c : citas) {
            if (!c.fecha.isBefore(lunes) && !c.fecha.isAfter(domingo)) count++;
        }
        return count;
    }
    
    



 
    public List<java.time.LocalDate> semanaActualLunesADomingo() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate domingo = hoy.with(java.time.DayOfWeek.SUNDAY);
        List<java.time.LocalDate> dias = new ArrayList<>();
        java.time.LocalDate d = lunes;
        while (!d.isAfter(domingo)) { dias.add(d); d = d.plusDays(1); }
        return dias;
    }
    
    // imprime estado de la semana en la terminal ---
    public void imprimirSemanaEstado() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate domingo = hoy.with(java.time.DayOfWeek.SUNDAY);

        System.out.println("=== Estado de semana (Lunes a Domingo) ===");
        java.time.LocalDate d = lunes;
        while (!d.isAfter(domingo)) {
            boolean bloqueado = bloqueos.contains(d);
            int countCitas = 0;
            for (Cita c : citas) {
                if (c.fecha.equals(d)) countCitas++;
            }
            String diaEnEspañol = d.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            System.out.println(d + " (" + diaEnEspañol + ") -> bloqueado=" + bloqueado + ", citas=" + countCitas);
            d = d.plusDays(1);
        }
        System.out.println("==========================================");
    }

    
public void limpiarAsistenciasVencidas() {
    java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
    java.util.List<Asistencia> vencidas = new java.util.ArrayList<>();
    for (Asistencia a : asistencias) {
        if (ahora.isAfter(a.salida)) {
            // Buscar cita asociada por fecha y usuario
            for (Cita c : citas) {
                if (c.fecha.equals(a.entrada.toLocalDate()) && c.candado) {
                    candadosTotales++; // Devolver candado al stock
                }
            }
            vencidas.add(a);
        }
    }
    asistencias.removeAll(vencidas);
}


    // Agenda una semana completa para un usuario con hora fija y candado opcional ---
    public void agendarSemanaParaUsuario(Usuario u, String horaStr, boolean candado) {
        java.time.LocalTime hora = java.time.LocalTime.parse(horaStr);
        for (java.time.LocalDate fecha : semanaActualLunesADomingo()) {
            if (!bloqueos.contains(fecha)) {
                try {
                    agendar(u, fecha, hora, candado);
                } catch (RuntimeException ex) {
                    // Si está bloqueado o hay error, lo saltamos (ya se notifica con imprimirSemanaEstado)
                }
            }
        }
    }
    
    // Genera/regenera el QR para el usuario en una fecha/hora.
    // Si ya existe cita ese día, actualiza la hora y regenera QR.
    // Si no existe, crea una nueva cita (candado=false).
    public Cita generarQR(Usuario u, java.time.LocalDate fecha, java.time.LocalTime hora) {
        if (bloqueos.contains(fecha)) throw new RuntimeException("Día bloqueado");
        for (Cita c : u.citas) {
            if (c.fecha.equals(fecha)) {
                c.hora = hora;
                c.qr = java.util.UUID.randomUUID().toString();
                return c;
            }
        }
        return agendar(u, fecha, hora, false); // crea nueva y devuelve QR
    }


    // Devuelve la próxima cita del usuario (hoy en adelante), prefiriendo la semana actual (L–D).
    public Cita proximaCita(Usuario u) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        Cita mejor = null;
        // Primero: dentro de la semana actual (L–D)
        java.time.LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate domingo = hoy.with(java.time.DayOfWeek.SUNDAY);
        for (Cita c : u.citas) {
            if (!c.fecha.isBefore(hoy) && !c.fecha.isAfter(domingo)) {
                if (mejor == null || c.fecha.isBefore(mejor.fecha) ||
                   (c.fecha.equals(mejor.fecha) && c.hora.isBefore(mejor.hora))) {
                    mejor = c;
                }
            }
        }
        if (mejor != null) return mejor;
        // Si no hay en la semana, buscar cualquier futura
        for (Cita c : u.citas) {
            if (!c.fecha.isBefore(hoy)) {
                if (mejor == null || c.fecha.isBefore(mejor.fecha) ||
                   (c.fecha.equals(mejor.fecha) && c.hora.isBefore(mejor.hora))) {
                    mejor = c;
                }
            }
        }
        return mejor;
    }

    // Regenera el QR de la próxima cita; si no tiene próxima cita, retorna null.
    public Cita generarQRProxima(Usuario u) {
        Cita prox = proximaCita(u);
        if (prox == null) return null;
        prox.qr = java.util.UUID.randomUUID().toString(); // QR dinámico simple
        return prox;
    }

    // Devuelve los días bloqueados de la semana actual (Lunes–Domingo)
    public List<java.time.LocalDate> diasBloqueadosSemanaActual() {
        List<java.time.LocalDate> out = new ArrayList<>();
        for (java.time.LocalDate d : semanaActualLunesADomingo()) {
            if (bloqueos.contains(d)) out.add(d);
        }
        return out;
    }
    
   

    // Devuelve los días de la semana actual donde el usuario tenía una cita
    // y ya no está (pudo haber sido "cancelada" por bloqueo global).
    // Como el diseño elimina las citas al bloquear, esto detecta ese efecto
    // comparando los días vs. las citas actuales del usuario.
    public List<java.time.LocalDate> diasCanceladosSemanaParaUsuario(Usuario u) {
        List<java.time.LocalDate> out = new ArrayList<>();
        var semana = semanaActualLunesADomingo();
        // Construir conjunto de fechas con cita vigente del usuario en la semana
        List<java.time.LocalDate> fechasConCita = new ArrayList<>();
        for (Cita c : u.citas) {
            if (semana.contains(c.fecha)) fechasConCita.add(c.fecha);
        }
        // Si el día está bloqueado, la cita del usuario no debería estar; lo marcamos como "cancelado"
        for (java.time.LocalDate d : semana) {
            if (bloqueos.contains(d) && !fechasConCita.contains(d)) {
                out.add(d);
            }
        }
        return out;
    }

   
public void imprimirUsuariosDentro() {
    java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
    System.out.println("=== Usuarios dentro del gimnasio ===");
    boolean hayUsuarios = false;
    for (Asistencia a : asistencias) {
        if (a.estaDentro(ahora)) {
            System.out.println("- " + a.usuario);
            hayUsuarios = true;
        }
    }
    if (!hayUsuarios) {
        System.out.println("No hay usuarios dentro del gimnasio en este momento.");
    }
    System.out.println("====================================");
}
 
public void eliminarCitasInvalidas() {
    java.time.LocalDate hoy = java.time.LocalDate.now();

    // Limpiar citas globales pasadas
    citas.removeIf(c -> c.fecha.isBefore(hoy));

    // Limpiar duplicadas exactas (fecha + hora) por usuario
    for (Usuario u : usuarios) {
        u.citas.removeIf(c -> c.fecha.isBefore(hoy));
        java.util.Set<String> combinacionesUnicas = new java.util.HashSet<>();
        u.citas.removeIf(c -> !combinacionesUnicas.add(c.fecha + "_" + c.hora));
    }
}


public String escanearQR(String token) {
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    Cita encontrada = null;

    // Buscar la cita por QR
    for (Cita c : citas) {
        if (c.qr.equals(token)) {
            encontrada = c;
            break;
        }
    }

    if (encontrada == null) {
        return "QR inválido"; //  Si no existe la cita
    }

    // Validar  entrada ±10 minutos
    java.time.LocalDateTime inicio = java.time.LocalDateTime.of(encontrada.fecha, encontrada.hora);
    java.time.LocalDateTime ventanaInicio = inicio.minusMinutes(toleranciaMinutos);
    java.time.LocalDateTime ventanaFin = inicio.plusMinutes(toleranciaMinutos);

    if (now.isBefore(ventanaInicio) || now.isAfter(ventanaFin)) {
        return "QR fuera de rango"; //  Mensaje más claro
    }

    // Registrar asistencia con salida automática (entrada + 120 min)
    java.time.LocalDateTime salida = now.plusMinutes(duracionAsistenciaMinutos);
    asistencias.add(new Asistencia(buscarNombreUsuarioPorCita(encontrada), now, salida));

    return "Entrada registrada para " + buscarNombreUsuarioPorCita(encontrada)
           + " (salida automática a las " + salida.toLocalTime() + ")";
}

    

public Usuario agregarAdmin(String nombre, String pass) {
        Usuario admin = new Usuario(nombre, pass, true);
        usuarios.add(admin);
        return admin;
    }

    
public boolean desbloquearDia(java.time.LocalDate fecha) {
    // Si el día está bloqueado, eliminarlo y devolver true
    if (bloqueos.contains(fecha)) {
        bloqueos.remove(fecha);
        return true;
    }
    return false; // No estaba bloqueado
}


// Dentro de la clase Gimnasio
public void imprimirUsuarios() {
    System.out.println("Lista de usuarios registrados:");
    for (Usuario u : usuarios) { 
        String tipo = u.esAdmin ? "(Admin)" : "(Usuario)";
        System.out.println("- " + u.nombre + " " + tipo);
    }
}

public void imprimirAdmins() {
    System.out.println("Lista de administradores:");
    for (Usuario u : usuarios) {
        if (u.esAdmin) {
            System.out.println("- " + u.nombre);
        }
    }
}

// Eliminar todas las citas anteriores a la fecha actual

public void eliminarCitasPasadas() {
    LocalDate hoy = LocalDate.now();
    LocalDate lunesActual = hoy.with(java.time.DayOfWeek.MONDAY);
    LocalTime ahora = LocalTime.now();

    // Eliminar de la lista global
    citas.removeIf(c -> c != null && (
        c.fecha.isBefore(lunesActual) || // fuera de la semana actual
        (c.fecha.equals(hoy) && c.hora.isBefore(ahora)) // hoy pero hora pasada
    ));

    // Eliminar de cada usuario
    for (Usuario u : usuarios) {
        u.citas.removeIf(c -> c != null && (
            c.fecha.isBefore(lunesActual) ||
            (c.fecha.equals(hoy) && c.hora.isBefore(ahora))
        ));
    }
}

public boolean validarUsuarioExistente(String nombre, String password) {
    for (Usuario u : usuarios) {
        if (u.nombre.equalsIgnoreCase(nombre)) {
            return !u.password.equals(password); // true si la contraseña es distinta
        }
    }
    return false;
}



public void actualizarQR(Cita cita) {
    // Genera un nuevo código QR único y lo asigna a la cita
    cita.qr = java.util.UUID.randomUUID().toString();
}

public long calcularDelay(Cita cita) {
    return java.time.Duration.between(
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.of(cita.fecha, cita.hora).minusMinutes(cita.minutosAntes)
    ).toMillis();
}

// Devuelve días bloqueados (por admin) + días pasados (calculados)
public List<LocalDate> diasBloqueadosYPasadosSemanaActual() {
    List<LocalDate> resultado = new ArrayList<>();
    LocalDate hoy = LocalDate.now();

    for (LocalDate d : semanaActualLunesADomingo()) {
        if (bloqueos.contains(d) || d.isBefore(hoy)) {
            resultado.add(d);
        }
    }
    return resultado;
}


public void limpiarBloqueosAntiguos() {
    // Determinar el lunes de la semana actual
    java.time.LocalDate lunesActual = java.time.LocalDate.now()
            .with(java.time.DayOfWeek.MONDAY);

    // Remover de memoria cualquier bloqueo anterior al lunes actual
    bloqueos.removeIf(d -> d.isBefore(lunesActual));
}


}

