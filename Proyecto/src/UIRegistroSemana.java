
import java.util.ArrayList;
import java.util.List;

public class UIRegistroSemana extends javax.swing.JFrame {
    private final Gimnasio gym;
    private final String username;
    private final String password;

    private javax.swing.JComboBox<String> cbLunes, cbMartes, cbMiercoles, cbJueves, cbViernes, cbSabado, cbDomingo;
    private javax.swing.JCheckBox chkNotificar, chkCandado;
    private javax.swing.JSpinner spMinAntes;
    private javax.swing.JButton btnRegistrar;

    public UIRegistroSemana(Gimnasio gym, String username, String password) {
        super("Registro semanal");
        this.gym = gym; this.username = username.trim(); this.password = password.trim();
        initUI();
    }

    private List<String> horasValidas() {
        List<String> h = new ArrayList<>();
        h.add("06:00"); h.add("08:00"); h.add("10:00");
        h.add("12:00"); h.add("14:00"); h.add("16:00"); h.add("18:00"); h.add("20:00");
        return h;
    }

    private javax.swing.JComboBox<String> comboConHoras() {
        List<String> horas = horasValidas();
        return new javax.swing.JComboBox<>(horas.toArray(new String[0]));
    }

    private void initUI() {
        setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 280);
        setLocationRelativeTo(null);
        setLayout(new java.awt.GridLayout(5, 1, 8, 8));

        var filaEtiquetas = new javax.swing.JPanel(new java.awt.GridLayout(1, 7, 4, 4));
        filaEtiquetas.add(new javax.swing.JLabel("Lunes", javax.swing.SwingConstants.CENTER));
        filaEtiquetas.add(new javax.swing.JLabel("Martes", javax.swing.SwingConstants.CENTER));
        filaEtiquetas.add(new javax.swing.JLabel("Miércoles", javax.swing.SwingConstants.CENTER));
        filaEtiquetas.add(new javax.swing.JLabel("Jueves", javax.swing.SwingConstants.CENTER));
        filaEtiquetas.add(new javax.swing.JLabel("Viernes", javax.swing.SwingConstants.CENTER));
        filaEtiquetas.add(new javax.swing.JLabel("Sábado", javax.swing.SwingConstants.CENTER));
        filaEtiquetas.add(new javax.swing.JLabel("Domingo", javax.swing.SwingConstants.CENTER));
        add(filaEtiquetas);

        cbLunes = comboConHoras(); cbMartes = comboConHoras(); cbMiercoles = comboConHoras();
        cbJueves = comboConHoras(); cbViernes = comboConHoras(); cbSabado = comboConHoras(); cbDomingo = comboConHoras();

        var filaCombos = new javax.swing.JPanel(new java.awt.GridLayout(1, 7, 4, 4));
        filaCombos.add(cbLunes); filaCombos.add(cbMartes); filaCombos.add(cbMiercoles);
        filaCombos.add(cbJueves); filaCombos.add(cbViernes); filaCombos.add(cbSabado); filaCombos.add(cbDomingo);
        add(filaCombos);

        var filaNotificaciones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        chkNotificar = new javax.swing.JCheckBox("Notificar");
        spMinAntes = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(10, 0, 120, 5));
        filaNotificaciones.add(chkNotificar);
        filaNotificaciones.add(new javax.swing.JLabel(" ... minutos antes"));
        filaNotificaciones.add(spMinAntes);
        add(filaNotificaciones);

        var filaCandado = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        chkCandado = new javax.swing.JCheckBox("Candado");
        filaCandado.add(new javax.swing.JLabel("Candado: "));
        filaCandado.add(chkCandado);
        add(filaCandado);

        var filaRegistrar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        btnRegistrar = new javax.swing.JButton("Registrar semana");
        filaRegistrar.add(btnRegistrar);
        add(filaRegistrar);

        btnRegistrar.addActionListener(e -> registrarSemana());

        // Deshabilitar días pasados
        
    }


private void registrarSemana() {
    // Intentar obtener usuario existente
    Usuario u = gym.login(username, password);
    if (u == null) {
        u = gym.registrar(username, password);
        if (u == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No se pudo registrar el usuario.");
            return;
        }
    }

    var semana = gym.semanaActualLunesADomingo();
    String[] seleccion = new String[] {
        (String) cbLunes.getSelectedItem(), (String) cbMartes.getSelectedItem(), (String) cbMiercoles.getSelectedItem(),
        (String) cbJueves.getSelectedItem(), (String) cbViernes.getSelectedItem(), (String) cbSabado.getSelectedItem(),
        (String) cbDomingo.getSelectedItem()
    };

    boolean candado = chkCandado.isSelected();
    boolean notificar = chkNotificar.isSelected();
    int minAntes = ((Integer) spMinAntes.getValue());

    for (int i = 0; i < semana.size(); i++) {
        java.time.LocalDate fecha = semana.get(i);
        java.time.LocalTime hora = java.time.LocalTime.parse(seleccion[i]);

        //  Validaciones
        if (fecha.isBefore(java.time.LocalDate.now())) continue; // Día pasado
        if (fecha.equals(java.time.LocalDate.now()) && hora.isBefore(java.time.LocalTime.now())) continue; // Hora pasada
        if (gym.diasBloqueadosSemanaActual().contains(fecha)) continue; // Día bloqueado

        //  Evitar duplicados: ¿ya tiene cita para esta fecha?
        boolean yaTieneCita = u.citas.stream().anyMatch(c -> c.fecha.equals(fecha));
        if (yaTieneCita) continue;

        try {
            Cita c = gym.agendar(u, fecha, hora, candado);
            c.notificar = notificar;
            c.minutosAntes = minAntes;

            if (gym instanceof GimnasioPersistente) {
                ((GimnasioPersistente) gym).guardarCitaNueva(u, c);
            }

            if (notificar) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Notificar " + minAntes + " min antes de " + fecha + " " + hora);
            }
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No se pudo agendar " + fecha + ": " + ex.getMessage());
        }
    }

    //  Limpiar duplicadas (mantener solo una cita por fecha)
    java.util.Set<java.time.LocalDate> fechasUnicas = new java.util.HashSet<>();
    u.citas.removeIf(c -> !fechasUnicas.add(c.fecha));

    //  Limpieza completa después de registrar semana
    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).limpiarCitasVencidas();
    } else {
        gym.eliminarCitasPasadas();
        gym.eliminarCitasInvalidas();
    }

    javax.swing.JOptionPane.showMessageDialog(this, "Semana agendada para " + u.nombre);
    dispose();
}

}
