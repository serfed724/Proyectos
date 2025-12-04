

import java.util.List;

public class UI {
    Gimnasio gym;

    public UI(Gimnasio gym) { this.gym = gym; }

   
    
public void abrirMenuUsuario(Usuario u) {
    mostrarPantallaVasAIr(u); // Este método ya existe, pero es privado
}

public void mostrarLogin() {
    gym.eliminarCitasPasadas();
    
 gym.eliminarCitasPasadas();

    // Limpia bloqueos antiguos antes de mostrar avisos
    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).limpiarBloqueosAntiguos();
    } else {
        gym.limpiarBloqueosAntiguos();
    }

    String user = javax.swing.JOptionPane.showInputDialog("Usuario:");
    if (user == null || user.isBlank()) return;
    if (user != null) user = user.trim();
    String pass = javax.swing.JOptionPane.showInputDialog("Contraseña:");
    if (pass == null) return;
    if (pass != null) pass = pass.trim();

    Usuario u = gym.login(user, pass);
    if (u == null) {
        // Validación: ¿existe el usuario con otra contraseña?
        boolean conflicto = false;
        if (gym instanceof GimnasioPersistente) {
            conflicto = ((GimnasioPersistente) gym).getDb()
                .existeUsuarioConOtraPassword(user, pass);
        } else {
            conflicto = gym.validarUsuarioExistente(user, pass);
        }
        if (conflicto) {
            javax.swing.JOptionPane.showMessageDialog(null,
                "Usuario o contraseña incorrecta");
            return; // Detener flujo
        }
        // Si no hay conflicto, preguntar si quiere registrarse
        int opt = javax.swing.JOptionPane.showConfirmDialog(null,
            "Usuario nuevo.\n¿Registrar y agendar semana?");
        if (opt == javax.swing.JOptionPane.YES_OPTION) {
            new UIRegistroSemana(gym, user, pass).setVisible(true);
        }
        
        return;
    }

    //  Aviso para agendar semana (solo en login)
    if (!u.esAdmin && !tieneCitasEnSemanaActual(u)) {
        int opt = javax.swing.JOptionPane.showConfirmDialog(null,
            "No tienes citas en la semana actual.\n¿Quieres agendar la semana completa?",
            "Agendar semana",
            javax.swing.JOptionPane.YES_NO_OPTION);
        if (opt == javax.swing.JOptionPane.YES_OPTION) {
            new UIRegistroSemana(gym, u.nombre, u.password).setVisible(true);
            return;
        }
    }

    if ("admin".equalsIgnoreCase(u.nombre)) {
        new AdminUI(gym).setVisible(true);
        return;
    }

    mostrarPantallaVasAIr(u);
}

public void mostrarPantallaVasAIr(Usuario u) {
    // Mostrar aviso cada vez que entras al perfil
    mostrarAvisoBloqueos(u);
    imprimirCitasUsuario(u);
     gym.imprimirUsuariosDentro();

    // Conteo de personas dentro
    int dentro = gym.usuariosDentro();

    //  Sincronizar asistencias si hay persistencia
    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).sincronizarAsistenciasDesdeDB();
         ((GimnasioPersistente) gym).procesarSalidasYDevolverCandados();
    }

    // Verificar si el usuario está dentro (asistencia activa en memoria)
    boolean estaDentro = false;
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    for (Asistencia a : gym.asistencias) {
        if (a.usuario.equals(u.nombre) && a.estaDentro(now)) {
            estaDentro = true;
            break;
        }
    }

    //  Ajustar conteo si el usuario está dentro
    if (estaDentro && dentro > 0) {
        
if (gym instanceof GimnasioPersistente) {
           ((GimnasioPersistente) gym).procesarSalidasYDevolverCandados();
         }

        dentro--;
    }

    String mensaje = "Hay " + dentro + " personas en el gimnasio.\n¿Quieres ir?";
    String[] opciones = {"Sí", "Reagendar", "No ir"};
    int sel = javax.swing.JOptionPane.showOptionDialog(
        null, mensaje, "Opciones",
        javax.swing.JOptionPane.DEFAULT_OPTION,
        javax.swing.JOptionPane.INFORMATION_MESSAGE,
        null, opciones, opciones[0]
    );

    if (sel == 0) { // Sí: genera QR para la cita de HOY
        java.time.LocalDate hoy = java.time.LocalDate.now();
        Cita citaHoy = null;
        for (Cita c : u.citas) {
            if (c.fecha.equals(hoy)) {
                citaHoy = c;
                break;
            }
        }
        if (citaHoy == null) {
            javax.swing.JOptionPane.showMessageDialog(null,
                "No tienes una cita para hoy. Usa 'Reagendar' para crear una.");
            abrirMenuUsuario(u);
        }
        generarQRParaCita(citaHoy);
        javax.swing.JOptionPane.showMessageDialog(null,
            "Tu cita para hoy:\nFecha: " + citaHoy.fecha +
            "\nHora: " + citaHoy.hora +
            "\nQR: " + citaHoy.qr);
        return;
    } else if (sel == 1) { // Reagendar
        new UIReagendarDia(gym, u).setVisible(true);
        return;
    } else if (sel == 2) { // No ir: eliminar cita de HOY
        java.time.LocalDate hoy = java.time.LocalDate.now();
        Cita citaHoy = null;
        for (Cita c : u.citas) {
            if (c.fecha.equals(hoy)) {
                citaHoy = c;
                break;
            }
        }
        if (citaHoy != null) {
            u.citas.remove(citaHoy);
            gym.citas.remove(citaHoy);
            if (gym instanceof GimnasioPersistente) {
                ((GimnasioPersistente) gym).eliminarCitaPorQR(citaHoy.qr);
            }
            javax.swing.JOptionPane.showMessageDialog(null,
                "Tu cita para hoy ha sido eliminada.");
        } else {
            javax.swing.JOptionPane.showMessageDialog(null,
                "No tienes cita para hoy.");
        }
        return;
    }
}


// Devuelve true si el usuario tiene al menos una cita en la semana actual (L-D).
private boolean tieneCitasEnSemanaActual(Usuario u) {
    var semana = gym.semanaActualLunesADomingo(); // Lunes a Domingo de la semana actual
    for (Cita c : u.citas) {
        if (semana.contains(c.fecha)) {
            return true;
        }
    }
    return false;
}




private void imprimirCitasUsuario(Usuario u) {
    System.out.println("=== Citas del usuario: " + u.nombre + " ===");
    if (u.citas.isEmpty()) {
        System.out.println("No tiene ninguna cita.");
    } else {
        for (Cita c : u.citas) {
            System.out.println("------------------------------");
            System.out.println("Fecha: " + c.fecha);
            System.out.println("Hora: " + c.hora);
            System.out.println("QR: " + c.qr);
            
        }
        System.out.println("------------------------------");
    }
    System.out.println("Total: " + u.citas.size() + " citas.");
    System.out.println("======================================");
}


private void generarQRParaCita(Cita cita) {
    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).actualizarQRSeguro(cita);
    } else {
        gym.actualizarQR(cita);
    }
}
private void mostrarAvisoBloqueos(Usuario u) {
    // Si el gimnasio es persistente, usamos la DB como fuente principal
    List<java.time.LocalDate> bloqueados;
    if (gym instanceof GimnasioPersistente) {
        bloqueados = ((GimnasioPersistente) gym).diasBloqueadosYPasadosSemanaActual();
    } else {
        bloqueados = gym.diasBloqueadosYPasadosSemanaActual();
    }

    var cancelados = gym.diasCanceladosSemanaParaUsuario(u);

    if (!bloqueados.isEmpty() || !cancelados.isEmpty()) {
        StringBuilder sb = new StringBuilder("Aviso:\n");

        if (!bloqueados.isEmpty()) {
            sb.append("Días bloqueados o pasados esta semana:\n");
            for (java.time.LocalDate d : bloqueados) {
                sb.append(" - ").append(d)
                  .append(" (").append(d.getDayOfWeek()).append(")\n");
            }
        }

        if (!cancelados.isEmpty()) {
            sb.append("\nCitas eliminadas por bloqueo:\n");
            for (java.time.LocalDate d : cancelados) {
                sb.append(" - ").append(d)
                  .append(" (").append(d.getDayOfWeek()).append(")\n");
            }
        }

        javax.swing.JOptionPane.showMessageDialog(null, sb.toString());
    }
}


}
