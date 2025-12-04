
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import java.util.ArrayList;

import java.util.UUID;
import java.util.Iterator;

import javax.swing.JOptionPane;

import java.time.Duration;

public class GimnasioPersistente extends Gimnasio {
    private DatabaseManager db;

   


public GimnasioPersistente(DatabaseManager db) {
    super();
    this.db = db;
    candadosTotales = db.getCandadosTotales();
    System.out.println("[INFO] Stock inicial de candados: " + candadosTotales);

    // Cargar usuarios y citas desde DB
    try (Statement stmt = db.getConnection().createStatement();
         ResultSet rs = stmt.executeQuery("SELECT nombre FROM usuarios")) {
        while (rs.next()) {
            String nombre = rs.getString("nombre");
            Usuario u = db.getUsuario(nombre);
            if (u != null) {
                List<Cita> citasUsuario = db.getCitasPorUsuario(nombre);
                u.citas.addAll(citasUsuario);
                usuarios.add(u);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    // Limpieza inicial
    limpiarBloqueosAntiguos();
    bloquearDiasPasados();

   sincronizarCitasDesdeDB();
   sincronizarAsistenciasDesdeDB();
   procesarSalidasYDevolverCandados();
   limpiarCitasVencidas();       // elimina citas vencidas
   sincronizarCitasDesdeDB();
   sincronizarAsistenciasDesdeDB();

   // 1) Sincronizar asistencias y procesar vencimientos (devolver candados primero)
   sincronizarAsistenciasDesdeDB();
   procesarSalidasYDevolverCandados(); // esta versión ya marca candado=false en DB

   // 2) Sincronizar citas (porque acabas de modificarlas al marcar candado=false)
   sincronizarCitasDesdeDB();

  // 3) Ahora sí, eliminar citas vencidas (ya sin riesgo de duplicar candados)
  limpiarCitasVencidas();

   // 4) Re-sincronizar
  sincronizarCitasDesdeDB();
   sincronizarAsistenciasDesdeDB();

    reprogramarNotificaciones();
}



public void bloquearDiasPasados() {
    LocalDate hoy = LocalDate.now();
    for (LocalDate fecha : semanaActualLunesADomingo()) {
        if (fecha.isBefore(hoy)) {
            if (!estaBloqueado(fecha)) { // Usa el método que revisa memoria + DB
                super.bloquearDia(fecha); // Bloquea en memoria
                db.insertBloqueo(fecha.toString()); // Bloquea en DB
                System.out.println("[INFO] Día pasado bloqueado: " + fecha);
            }
        }
    }
}


public DatabaseManager getDb() {
    return db;
}

public boolean estaBloqueado(LocalDate fecha) {
    // Verifica en memoria y en DB
    return bloqueos.contains(fecha) || db.getBloqueosSemana().contains(fecha);
}


// Método para reprogramar notificaciones desde la DB


private void reprogramarNotificaciones() {
    List<Usuario> copiaUsuarios = new ArrayList<>(usuarios); // Copia segura

    for (Usuario u : copiaUsuarios) {
        List<Cita> copiaCitas = new ArrayList<>(u.citas); // Copia segura
        for (Cita c : copiaCitas) {
            if (c.notificar) {
                long delay = Duration.between(
                    java.time.LocalDateTime.now(),
                    java.time.LocalDateTime.of(c.fecha, c.hora).minusMinutes(c.minutosAntes)
                ).toMillis();

                if (delay > 0) {
                    scheduler.schedule(() -> {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            int opt = javax.swing.JOptionPane.showOptionDialog(
                                null,
                                "Aviso para " + u.nombre + "\nTu cita es a las " + c.hora + " en " + c.fecha,
                                "Recordatorio",
                                javax.swing.JOptionPane.DEFAULT_OPTION,
                                javax.swing.JOptionPane.INFORMATION_MESSAGE,
                                null,
                                new Object[]{"Aceptar"},
                                "Aceptar"
                            );

                            if (opt == 0) {
                                new UI(this).abrirMenuUsuario(u);
                            }
                        });
                    }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}


@Override
public int usuariosDentro() {
    return db.contarUsuariosDentro();
}



public void sincronizarAsistenciasDesdeDB() {
    asistencias.clear();
    asistencias.addAll(db.getAsistenciasActivas());
}




 
public void sincronizarCitasDesdeDB() {
    for (Usuario u : usuarios) {
        u.citas.clear();
        u.citas.addAll(db.getCitasPorUsuario(u.nombre));
    }
}   

    @Override
    public Usuario registrar(String nombre, String password) {
        nombre = nombre.trim();
        password = password.trim();
        Usuario u = super.registrar(nombre, password);
        if (u != null) {
            db.insertUsuario(nombre, password, false);
        }
        return u;
    }

    @Override
    public Usuario agregarAdmin(String nombre, String password) {
        Usuario u = super.agregarAdmin(nombre, password);
        if (u != null) {
            db.insertUsuario(nombre, password, true);
        }
        return u;
    }

@Override
public void limpiarAsistenciasVencidas() {
    LocalDateTime ahora = LocalDateTime.now();
    List<Asistencia> vencidas = new ArrayList<>();
    List<java.time.LocalDate> fechasParaLimpiarEnDB = new ArrayList<>();

    // Recorremos asistencias en memoria
    for (Asistencia a : asistencias) {
        if (ahora.isAfter(a.salida)) {
            // Marcar asistencia como vencida (se eliminará al final)
            vencidas.add(a);
            

            // Acumular la fecha para borrado de asistencias en DB (si lo deseas)
            fechasParaLimpiarEnDB.add(a.entrada.toLocalDate());

            // Buscar la cita del mismo usuario y misma fecha
            for (Usuario u : usuarios) {
                if (u.nombre.equals(a.usuario)) {
                    Iterator<Cita> it = u.citas.iterator();
                    while (it.hasNext()) {
                        Cita c = it.next();

                        // Cita del mismo día que la asistencia
                        if (c.fecha.equals(a.entrada.toLocalDate())) {
                            int citaId = db.getCitaIdPorQR(c.qr);

                            // Eliminar cita SOLO si su hora ya pasó
                            LocalDateTime horaCita = LocalDateTime.of(c.fecha, c.hora);
                            if (ahora.isAfter(horaCita)) {
                                if (citaId != -1) {
                                    db.eliminarCitaPorId(citaId);
                                    System.out.println("[INFO] Cita eliminada en DB por asistencia vencida: " + c.qr);
                                    devolverCandado();
                                }
                                it.remove(); // eliminar de memoria del usuario
                            }
                        }
                    }
                }
            }
        }
    }

    // Eliminar asistencias vencidas de memoria (usa la lista 'vencidas')
    if (!vencidas.isEmpty()) {
        asistencias.removeAll(vencidas);
    }

    
    // Evita usar 'a' fuera del for: recorre las fechas acumuladas.
    for (java.time.LocalDate f : fechasParaLimpiarEnDB) {
        db.eliminarAsistenciasPorFecha(f);
    }

    
}


    
@Override
public Cita agendar(Usuario u, java.time.LocalDate fecha, java.time.LocalTime hora, boolean candado) {
    if (bloqueos.contains(fecha) || db.getBloqueosSemana().contains(fecha)) {
        throw new RuntimeException("Día bloqueado");
    }
    if (fecha.equals(java.time.LocalDate.now()) && hora.isBefore(java.time.LocalTime.now())) {
        throw new RuntimeException("No se puede agendar en una hora que ya pasó");
    }

    // Lógica para candado
    
if (candado) {
    if (candadosTotales <= 0) {
        JOptionPane.showMessageDialog(null,
            "No hay candados disponibles, la cita se agendará sin candado.");
        candado = false;
    } else {
        candadosTotales--; // Reservar candado
        db.updateCandadosTotales(candadosTotales); //  Persistir cambio
    }
}


    String qr = java.util.UUID.randomUUID().toString();
    Cita c = new Cita(fecha, hora, candado, qr);
    u.citas.add(c);
    citas.add(c);
    guardarCitaNueva(u, c);
    return c;
}




@Override
public void bloquearDia(LocalDate fecha) {
    // Sincronizar memoria con DB antes de eliminar
    sincronizarCitasDesdeDB();

    int candadosDevueltos = 0;

    // 1) Contar candados y eliminar citas en memoria
    for (Usuario u : usuarios) {
        java.util.Iterator<Cita> it = u.citas.iterator();
        while (it.hasNext()) {
            Cita c = it.next();
            if (c.fecha.equals(fecha)) {
                if (c.candado) candadosDevueltos++;
                it.remove(); // eliminar cita
            }
        }
    }

    java.util.Iterator<Cita> itGlobal = citas.iterator();
    while (itGlobal.hasNext()) {
        Cita c = itGlobal.next();
        if (c.fecha.equals(fecha)) {
            if (c.candado) candadosDevueltos++;
            itGlobal.remove();
        }
    }

    // 2) Actualizar stock en memoria y DB
    candadosTotales += candadosDevueltos;
    db.updateCandadosTotales(candadosTotales);

    // 3) Guardar bloqueo en DB
    try {
        db.insertBloqueo(fecha.toString());
        System.out.println("[INFO] Bloqueo guardado en DB para fecha: " + fecha);
    } catch (Exception e) {
        System.err.println("[ERROR] Fallo al guardar bloqueo en DB: " + e.getMessage());
    }

    // 4) Eliminar TODAS las citas de esa fecha en DB y devolver candados
    int eliminadas = 0;
    try {
        for (Usuario u : usuarios) {
            java.util.List<Cita> citasDB = db.getCitasPorUsuario(u.nombre);
            for (Cita c : citasDB) {
                if (fecha.equals(c.fecha)) {
                    if (c.candado) candadosDevueltos++;
                    int citaId = db.getCitaIdPorQR(c.qr);
                    if (citaId != -1) {
                        db.eliminarCitaPorId(citaId);
                        eliminadas++;
                    }
                }
            }
        }
    } catch (Exception ex) {
        System.err.println("[ERROR] Fallo al eliminar citas por fecha en DB: " + ex.getMessage());
    }

    System.out.println("[INFO] Total de citas eliminadas en DB para " + fecha + ": " + eliminadas);

    // 5) Sincronizar memoria <-- DB
    sincronizarCitasDesdeDB();
    limpiarAsistenciasSinCita();

    System.out.println("[INFO] Candados devueltos por bloqueo: " + candadosDevueltos + " | Stock actual: " + candadosTotales);
}




public void reiniciarCandados(int valorInicial) {
    candadosTotales = valorInicial;
    db.updateCandadosTotales(valorInicial);
    System.out.println("[INFO] Candados reiniciados a: " + valorInicial);
}


private void limpiarAsistenciasSinCita() {
    asistencias.removeIf(a -> {
        Usuario u = usuarios.stream()
            .filter(us -> us.nombre.equals(a.usuario))
            .findFirst().orElse(null);
        return u == null || u.citas.isEmpty();
    });
}


@Override
public String escanearQR(String qr) {
    sincronizarCitasDesdeDB(); // Actualiza memoria

    // Buscar cita en DB
    Cita citaDB = db.getCitaPorQR(qr);
    if (citaDB == null) {
        return "QR inválido"; // No existe en DB ni en memoria
    }
    
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime inicio = LocalDateTime.of(citaDB.fecha, citaDB.hora);
    LocalDateTime ventanaInicio = inicio.minusMinutes(toleranciaMinutos);
    LocalDateTime ventanaFin = inicio.plusMinutes(toleranciaMinutos);

    if (now.isBefore(ventanaInicio) || now.isAfter(ventanaFin)) {
        return "QR fuera de rango";
    }

    // Registrar asistencia en DB
    String usuario = buscarUsuarioPorQR(qr);
    int usuarioId = db.getUsuarioId(usuario);
    LocalDateTime entrada = now;
    LocalDateTime salida = entrada.plusMinutes(duracionAsistenciaMinutos);
    db.insertAsistencia(usuarioId, entrada, salida);

    // Agregar asistencia en memoria para métricas
    asistencias.add(new Asistencia(usuario, entrada, salida));

    //  Marcar cita como completada (NO eliminar)
    int citaId = db.getCitaIdPorQR(qr);
    if (citaId != -1) {
        db.marcarCitaCompletada(citaId); // Persistir estado completado
    }

    return "Entrada registrada para " + usuario +
           " (salida automática a las " + salida.toLocalTime() + ")";
}



@Override
public Cita reagendarDia(Usuario u, LocalDate fecha, String nuevaHoraStr, boolean candadoSolicitado) {
    // Sincronizar memoria antes de buscar
    sincronizarCitasDesdeDB();

    if (bloqueos.contains(fecha) || db.getBloqueosSemana().contains(fecha)) {
        throw new RuntimeException("No se puede reagendar: el día está bloqueado");
    }

    // Buscar cita en memoria
    Cita cita = null;
    for (Cita c : u.citas) {
        if (c.fecha.equals(fecha)) {
            cita = c;
            break;
        }
    }
    if (cita == null) {
        System.err.println("[WARN] No se encontró cita en memoria para reagendar");
        return null;
    }

    // Actualizar hora en memoria
    cita.hora = LocalTime.parse(nuevaHoraStr);

    //  Lógica para candado y persistencia
    if (candadoSolicitado && !cita.candado) {
        if (candadosTotales <= 0) {
            throw new RuntimeException("No hay candados disponibles para reagendar");
        }
        candadosTotales--; // Reservar candado
        db.updateCandadosTotales(candadosTotales); // Persistir cambio
        cita.candado = true;
    } else if (!candadoSolicitado && cita.candado) {
        candadosTotales++; // Devolver candado
        db.updateCandadosTotales(candadosTotales); // Persistir cambio
        cita.candado = false;
    }

    // Persistir cambios en DB usando QR actual
    int citaId = db.getCitaIdPorQR(cita.qr);
    if (citaId != -1) {
        db.updateCita(citaId, cita.fecha.toString(), cita.hora.toString(), cita.qr,
                      cita.candado, cita.notificar, cita.minutosAntes);
    }

    // Regenerar QR y actualizar en DB
    String nuevoQR = java.util.UUID.randomUUID().toString();
    cita.qr = nuevoQR;
    if (citaId != -1) {
        db.updateCita(citaId, cita.fecha.toString(), cita.hora.toString(), cita.qr,
                      cita.candado, cita.notificar, cita.minutosAntes);
    }

    return cita;
}

    private String buscarUsuarioPorQR(String qr) {
        for (Usuario u : usuarios) {
            for (Cita c : u.citas) {
                if (c.qr.equals(qr)) return u.nombre;
            }
        }
        sincronizarCitasDesdeDB();
        return null;
    }


public void limpiarCitasVencidas() {
    // 0) Traer asistencias frescas para que el blindaje funcione
    sincronizarAsistenciasDesdeDB();

    LocalDate hoy = LocalDate.now();
    LocalTime ahora = LocalTime.now();

    for (Usuario u : usuarios) {
        Iterator<Cita> it = u.citas.iterator();
        while (it.hasNext()) {
            Cita c = it.next();

            boolean vencidaPorHora = c.fecha.isBefore(hoy)
                    || (c.fecha.equals(hoy) && c.hora.isBefore(ahora));

            if (!vencidaPorHora) {
                continue; // la cita todavía no vence por hora
            }

            // A) ¿Hay asistencia ACTIVA para ese día? -> NO eliminar aún (esperar a que venza la asistencia)
            boolean asistenciaActivaEseDia = asistencias.stream().anyMatch(a ->
                    a.usuario.equals(u.nombre)
                    && a.entrada.toLocalDate().equals(c.fecha)
                    && LocalDateTime.now().isBefore(a.salida) // aún dentro (no vencida)
            );

            if (asistenciaActivaEseDia) {
                System.out.println("[DEBUG] Cita no eliminada por asistencia activa (usuario="
                        + u.nombre + ", fecha=" + c.fecha + ")");
                continue;
            }

            // B) ¿Hubo asistencia vencida ese día?
            boolean huboAsistenciaVencidaEseDia = asistencias.stream().anyMatch(a ->
                    a.usuario.equals(u.nombre)
                    && a.entrada.toLocalDate().equals(c.fecha)
                    && LocalDateTime.now().isAfter(a.salida) // asistencia ya vencida
            );

            // C) ¿Hubo alguna asistencia ese día? (si no, es NO-SHOW)
            boolean huboAlgunaAsistenciaEseDia = asistencias.stream().anyMatch(a ->
                    a.usuario.equals(u.nombre)
                    && a.entrada.toLocalDate().equals(c.fecha)
            );

            // D) Si la cita tenía candado:
            //    - Hubo asistencia vencida -> devolver
            //    - NO hubo ninguna asistencia -> devolver (NO-SHOW)
            if (c.candado && (huboAsistenciaVencidaEseDia || !huboAlgunaAsistenciaEseDia)) {
                // Devolver candado al stock (memoria + DB)
                devolverCandado();
                // Marcar la cita como sin candado para evitar doble devolución en otras rutinas
                c.candado = false;

                // Persistir cambio en la cita antes de eliminarla
                int citaIdPre = db.getCitaIdPorQR(c.qr);
                if (citaIdPre != -1) {
                    db.updateCita(
                            citaIdPre,
                            c.fecha.toString(),
                            c.hora.toString(),
                            c.qr,
                            /* candado */ false,
                            c.notificar,
                            c.minutosAntes
                    );
                }

                System.out.println("[INFO] Candado devuelto en limpieza (" 
                        + (huboAsistenciaVencidaEseDia ? "asistencia vencida" : "NO-SHOW")
                        + ") usuario=" + u.nombre + ", fecha=" + c.fecha);
            }

            // E) Eliminar cita de memoria
            it.remove();

            // F) Eliminar cita en DB
            int citaId = db.getCitaIdPorQR(c.qr);
            if (citaId != -1) {
                db.eliminarCitaPorId(citaId);
                System.out.println("[INFO] Cita vencida eliminada en DB: " + c.qr);
            }
        }
    }

    // 5) Re-sincronizar con DB tras la limpieza
    sincronizarCitasDesdeDB();
    System.out.println("[INFO] Limpieza ejecutada: citas vencidas eliminadas.");
}



public void guardarCitaNueva(Usuario u, Cita c) {
    int usuarioId = db.getUsuarioId(u.nombre);
    // Verificar si ya existe una cita para este usuario en la misma fecha
    if (!db.existeCitaParaUsuarioEnFecha(usuarioId, c.fecha.toString())) {
        db.insertCita(usuarioId, c.fecha.toString(), c.hora.toString(), c.candado, c.qr, c.notificar, c.minutosAntes);
        System.out.println("[INFO] Cita insertada en DB para " + u.nombre + " en " + c.fecha);
    } else {
        System.out.println("[INFO] Cita ya existente en DB para " + u.nombre + " en " + c.fecha + ". No se inserta duplicado.");
    }
}

// Dentro de GimnasioPersistente.java
@Override
public void limpiarBloqueosAntiguos() {
    java.time.LocalDate lunesActual = java.time.LocalDate.now()
            .with(java.time.DayOfWeek.MONDAY);

    // 1) Limpiar en memoria
    bloqueos.removeIf(d -> d.isBefore(lunesActual));

    // 2) Limpiar en DB
    try {
        // Trae TODOS los bloqueos almacenados
        java.util.List<java.time.LocalDate> bloqueosDB = db.getBloqueosSemana();
        for (java.time.LocalDate d : bloqueosDB) {
            if (d.isBefore(lunesActual)) {
                db.eliminarBloqueo(d.toString());
            }
        }
    } catch (Exception ex) {
        System.err.println("[WARN] Fallo al limpiar bloqueos antiguos en DB: " + ex.getMessage());
    }

    // 3) Sincronizar memoria desde DB para mantener consistencia
    // (No imprescindible, pero útil si otras partes leen 'bloqueos' desde DB más adelante)
    // Nota: si tienes un método específico para sincronizar bloqueos, utilízalo aquí.
    // En su defecto, recarga a 'bloqueos' desde DB.
    bloqueos.clear();
    bloqueos.addAll(db.getBloqueosSemana());
}


    /**
     * Método corregido: flujo seguro
     * 1. Actualiza la cita en DB con el QR actual.
     * 2. Regenera el QR y actualiza nuevamente en DB.
     */
    public void guardarCita(Cita c) {
        try {
            int citaId = db.getCitaIdPorQR(c.qr);
            if (citaId != -1) {
                // Paso 1: Actualizar datos con QR actual
                db.updateCita(citaId,
                    c.fecha.toString(),
                    c.hora.toString(),
                    c.qr,
                    c.candado,
                    c.notificar,
                    c.minutosAntes
                );

                // Paso 2: Regenerar QR y actualizar en DB
                String nuevoQR = UUID.randomUUID().toString();
                c.qr = nuevoQR;
                db.updateCita(citaId,
                    c.fecha.toString(),
                    c.hora.toString(),
                    c.qr,
                    c.candado,
                    c.notificar,
                    c.minutosAntes
                );
            } else {
                System.err.println("[WARN] No se encontró la cita en DB para guardar.");
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] Fallo al guardar cita en DB: " + ex.getMessage());
        }
        sincronizarCitasDesdeDB();
    }

    public void actualizarQRSeguro(Cita c) {
        String nuevoQR = UUID.randomUUID().toString();
        try {
            int citaId = db.getCitaIdPorQR(c.qr);
            if (citaId != -1) {
                db.updateCita(citaId,
                    c.fecha.toString(),
                    c.hora.toString(),
                    nuevoQR,
                    c.candado,
                    c.notificar,
                    c.minutosAntes
                );
                c.qr = nuevoQR; 
            } else {
                System.err.println("[WARN] No se encontró la cita en DB para actualizar QR.");
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] Fallo al actualizar QR en DB: " + ex.getMessage());
        }
    }


    
@Override
public boolean desbloquearDia(LocalDate fecha) {
    boolean result = super.desbloquearDia(fecha); // intenta en memoria
    if (!result) {
        // Verificar en DB si está bloqueado
        if (db.getBloqueosSemana().contains(fecha)) {
            db.eliminarBloqueo(fecha.toString());
            System.out.println("[INFO] Desbloqueo aplicado solo en DB para fecha: " + fecha);
            return true;
        }
        System.out.println("[WARN] Intento de desbloqueo fallido: el día no estaba bloqueado ni en memoria ni en DB.");
        return false;
    }
    // Si estaba en memoria, también eliminar en DB
    db.eliminarBloqueo(fecha.toString());
    System.out.println("[INFO] Desbloqueo persistente aplicado para fecha: " + fecha);
    return true;
}

@Override
public List<LocalDate> diasBloqueadosYPasadosSemanaActual() {
    List<LocalDate> resultado = new ArrayList<>();
    LocalDate hoy = LocalDate.now();

    // Bloqueos desde DB
    resultado.addAll(db.getBloqueosSemana());

    // Agregar días pasados si no están en DB
    for (LocalDate d : semanaActualLunesADomingo()) {
        if (d.isBefore(hoy) && !resultado.contains(d)) {
            resultado.add(d);
        }
    }
    return resultado;
}

@Override
public int reservasSemana() {
    return db.contarReservasSemana();
}




public void eliminarCitaPorQR(String qr) {
    int citaId = db.getCitaIdPorQR(qr);
    if (citaId != -1) {
        db.eliminarCitaPorId(citaId);
    }
}


@Override
public void devolverCandado() {
    candadosTotales++;
    db.updateCandadosTotales(candadosTotales); // Persistir
    System.out.println("[INFO] Candado devuelto y guardado en DB. Stock actual: " + candadosTotales);
}


@Override
public void procesarSalidasYDevolverCandados() {
    // 1) Traer asistencias frescas desde DB
    sincronizarAsistenciasDesdeDB();

    LocalDateTime ahora = LocalDateTime.now();
    System.out.println("[DEBUG] Procesando asistencias. Ahora=" + ahora + " (total=" + asistencias.size() + ")");

    Iterator<Asistencia> it = asistencias.iterator();

    while (it.hasNext()) {
        Asistencia a = it.next();

        boolean asistenciaVencida = ahora.isAfter(a.salida);
        System.out.println("[DEBUG] Asistencia usuario=" + a.usuario
                + " entrada=" + a.entrada + " salida=" + a.salida
                + " vencida=" + asistenciaVencida);

        // 2) Si ya pasó la hora de salida, la asistencia venció
        if (asistenciaVencida) {
            // 2.1 Buscar la cita del usuario para ese mismo día
            Usuario usuario = usuarios.stream()
                    .filter(us -> us.nombre.equals(a.usuario))
                    .findFirst()
                    .orElse(null);

            Cita citaCandado = null;
            if (usuario != null) {
                // Refrescar las citas del usuario desde DB para asegurar estado consistente
                usuario.citas.clear();
                usuario.citas.addAll(db.getCitasPorUsuario(usuario.nombre));

                for (Cita c : usuario.citas) {
                    if (c.fecha.equals(a.entrada.toLocalDate())) {
                        citaCandado = c;
                        break;
                    }
                }
            }

            // 2.2 Devolver candado SOLO si la cita lo tenía (y no fue devuelto antes)
            if (citaCandado != null && citaCandado.candado) {
                // incrementa stock en memoria + persiste en DB
                devolverCandado();

                // Marcar la cita como ya sin candado para evitar dobles devoluciones
                citaCandado.candado = false;

                // Persistir el cambio en la cita
                int citaId = db.getCitaIdPorQR(citaCandado.qr);
                if (citaId != -1) {
                    db.updateCita(
                            citaId,
                            citaCandado.fecha.toString(),
                            citaCandado.hora.toString(),
                            citaCandado.qr,
                            /* candado */ false,
                            citaCandado.notificar,
                            citaCandado.minutosAntes
                    );
                }

                System.out.println("[INFO] Candado devuelto por salida automática (usuario=" + a.usuario + ")");
            } else {
                System.out.println("[DEBUG] No se devolvió candado (cita no encontrada o ya sin candado) para usuario="
                        + a.usuario + ", fecha=" + a.entrada.toLocalDate());
            }

            // 2.3 Eliminar la asistencia vencida para que no se reprocese (borrado PRECISO)
            it.remove();

    
        }
    }

    // 3) Re-sincronizar asistencias tras la limpieza
    sincronizarAsistenciasDesdeDB();
}






}