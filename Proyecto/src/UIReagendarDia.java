
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class UIReagendarDia extends javax.swing.JDialog {
    private final Gimnasio gym;
    private final Usuario usuario;
    private javax.swing.JComboBox<String> cbDia;
    private javax.swing.JComboBox<String> cbHora;
    private javax.swing.JCheckBox chkNotificar;
    private javax.swing.JSpinner spMinAntes;
    private javax.swing.JCheckBox chkCandado;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton btnCancelar;

    public UIReagendarDia(Gimnasio gym, Usuario usuario) {
        super((java.awt.Frame) null, "Reagendar día", true);
        this.gym = gym;
        this.usuario = usuario;
        initUI();
    }

    private List<String> horasValidas() {
        List<String> h = new ArrayList<>();
        h.add("06:00"); h.add("08:00"); h.add("10:00");
        h.add("12:00"); h.add("14:00"); h.add("16:00"); h.add("18:00"); h.add("20:00");
        return h;
    }

    private List<String> diasSemana() {
        List<String> d = new ArrayList<>();
        d.add("Lunes"); d.add("Martes"); d.add("Miércoles");
        d.add("Jueves"); d.add("Viernes"); d.add("Sábado"); d.add("Domingo");
        return d;
    }

   
private void initUI() {
    setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
    setSize(560, 260);
    setLocationRelativeTo(null);
    setLayout(new java.awt.GridLayout(4, 1, 8, 8));

    // Fila 1: Día + Hora
    var filaDiaHora = new javax.swing.JPanel(new java.awt.GridLayout(1, 4, 8, 8));
    filaDiaHora.add(new javax.swing.JLabel("Día:", javax.swing.SwingConstants.RIGHT));
    cbDia = new javax.swing.JComboBox<>(diasSemana().toArray(new String[0]));
    filaDiaHora.add(cbDia);
    filaDiaHora.add(new javax.swing.JLabel("Hora de llegada:", javax.swing.SwingConstants.RIGHT));
    cbHora = new javax.swing.JComboBox<>(horasValidas().toArray(new String[0]));
    filaDiaHora.add(cbHora);
    add(filaDiaHora);

    // Fila 2: Notificar + minutos antes
    var filaNotif = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    chkNotificar = new javax.swing.JCheckBox("Notificar");
    spMinAntes = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(10, 0, 120, 5));
    filaNotif.add(chkNotificar);
    filaNotif.add(new javax.swing.JLabel(" ... minutos antes "));
    filaNotif.add(spMinAntes);
    add(filaNotif);

    // Fila 3: Candado
    var filaCandado = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    filaCandado.add(new javax.swing.JLabel("Candado: "));
    chkCandado = new javax.swing.JCheckBox("Candado");
    filaCandado.add(chkCandado);
    add(filaCandado);

    // Fila 4: Botones
    var filaBtns = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
    btnRegistrar = new javax.swing.JButton("Registrar");
    btnCancelar = new javax.swing.JButton("Cancelar");
    filaBtns.add(btnRegistrar);
    filaBtns.add(btnCancelar);
    add(filaBtns);

    // Lógica para actualizar horas según el día seleccionado
    cbDia.addActionListener(e -> {
        var semana = gym.semanaActualLunesADomingo();
        java.time.LocalDate fechaSeleccionada = mapDiaToFecha(semana, (String) cbDia.getSelectedItem());

        cbHora.removeAllItems();
        List<String> horasDisponibles = horasValidasParaDia(fechaSeleccionada);
        for (String h : horasDisponibles) {
            cbHora.addItem(h);
        }

        // Deshabilitar botón si no hay horas disponibles
        btnRegistrar.setEnabled(!horasDisponibles.isEmpty());

        // Opcional: mensaje si no hay horarios
        if (horasDisponibles.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "No hay horarios disponibles para el día seleccionado.");
        }
    });

    chkNotificar.addActionListener(e -> spMinAntes.setEnabled(chkNotificar.isSelected()));
    spMinAntes.setEnabled(chkNotificar.isSelected());
    btnRegistrar.addActionListener(e -> aplicarReagenda());
    btnCancelar.addActionListener(e -> dispose());
}

private void aplicarReagenda() {
    String diaStr = (String) cbDia.getSelectedItem();
    String horaStr = (String) cbHora.getSelectedItem();
    boolean candado = chkCandado.isSelected();
    boolean notificar = chkNotificar.isSelected();
    int minAntes = (Integer) spMinAntes.getValue();

    var semana = gym.semanaActualLunesADomingo();
    java.time.LocalDate fechaSeleccionada = mapDiaToFecha(semana, diaStr);

    // Validación ANTES de reagendar
    if (gym instanceof GimnasioPersistente) {
        if (((GimnasioPersistente) gym).estaBloqueado(fechaSeleccionada)) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se puede reagendar: el día está bloqueado.");
            return;
        }
        //  Sincronizar memoria antes de reagendar
        ((GimnasioPersistente) gym).sincronizarCitasDesdeDB();
    } else {
        if (gym.diasBloqueadosSemanaActual().contains(fechaSeleccionada)) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se puede reagendar: el día está bloqueado.");
            return;
        }
    }

    try {
        //  Reagendar la cita
        Cita c = gym.reagendarDia(usuario, fechaSeleccionada, horaStr, candado);
      

if (c == null) {
            c = gym.agendar(usuario, fechaSeleccionada, java.time.LocalTime.parse(horaStr), candado);
            if (gym instanceof GimnasioPersistente) {
                ((GimnasioPersistente) gym).guardarCitaNueva(usuario, c);
            }
            
        }



        //  Actualizar opciones
        c.notificar = notificar;
        c.minutosAntes = minAntes;

        //  Guardar cambios si es persistente (antes de regenerar QR)
        if (gym instanceof GimnasioPersistente) {
            ((GimnasioPersistente) gym).guardarCita(c);
        }

        //  Regenerar QR y actualizar en DB
        generarQRParaCita(c);
        if (gym instanceof GimnasioPersistente) {
            ((GimnasioPersistente) gym).guardarCita(c);
        }

        //  Mostrar confirmación con todos los datos
        javax.swing.JOptionPane.showMessageDialog(this,
            "Reagenda aplicada\n" +
            "Día: " + diaStr + " (" + fechaSeleccionada + ")\n" +
            "Hora: " + c.hora + "\nQR: " + c.qr +
            (candado ? "\nLocker: solicitado" : "")
        );

        if (notificar) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Se notificará " + minAntes + " min antes de " +
                fechaSeleccionada + " " + c.hora);
        }

        dispose(); //  Cerrar ventana para evitar flujo duplicado
    } catch (RuntimeException ex) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    }
}

private void generarQRParaCita(Cita cita) {
    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).actualizarQRSeguro(cita);
    } else {
        gym.actualizarQR(cita);
    }
}

    private java.time.LocalDate mapDiaToFecha(java.util.List<java.time.LocalDate> semana, String diaStr) {
        int idx;
        switch (diaStr) {
            case "Lunes": idx = 0; break;
            case "Martes": idx = 1; break;
            case "Miércoles": idx = 2; break;
            case "Jueves": idx = 3; break;
            case "Viernes": idx = 4; break;
            case "Sábado": idx = 5; break;
            case "Domingo": idx = 6; break;
            default: throw new IllegalArgumentException("Día inválido: " + diaStr);
        }
        return semana.get(idx);
    }

private List<String> horasValidasParaDia(java.time.LocalDate fecha) {
    List<String> horas = new ArrayList<>();
    java.time.LocalDate hoy = java.time.LocalDate.now();
    java.time.LocalTime ahora = java.time.LocalTime.now();

    // Si el día está bloqueado o ya pasó, no hay horas válidas
    boolean bloqueado = (gym instanceof GimnasioPersistente)
            ? ((GimnasioPersistente) gym).estaBloqueado(fecha)
            : gym.diasBloqueadosSemanaActual().contains(fecha);

    if (bloqueado || fecha.isBefore(hoy)) {
        return horas; // lista vacía
    }

    // Si es hoy, solo horas posteriores
    for (String h : List.of("06:00","08:00","10:00","12:00","14:00","16:00","18:00", "20:00")) {
        java.time.LocalTime hora = java.time.LocalTime.parse(h);
        if (!fecha.equals(hoy) || hora.isAfter(ahora)) {
            horas.add(h);
        }
    }
    return horas;
}



}
