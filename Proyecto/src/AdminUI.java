import javax.swing.JOptionPane;

public class AdminUI extends javax.swing.JFrame {
    private final Gimnasio gym;

    // Cabecera de métricas
    private javax.swing.JLabel lblReservas;
    private javax.swing.JLabel lblDentro;

    // Contenedor central
    private javax.swing.JPanel panelCentral;

    // Paneles
    private javax.swing.JPanel panelInicio;
    private javax.swing.JPanel panelBloqueo;
    private javax.swing.JPanel panelEscaneo;
    private javax.swing.JPanel panelDesbloqueo;

    // Checkboxes bloqueo
    private javax.swing.JCheckBox chkLunes, chkMartes, chkMiercoles, chkJueves, chkViernes, chkSabado, chkDomingo;
    private javax.swing.JButton btnAceptarBloqueo, btnVolverBloqueo;

    // Checkboxes desbloqueo
    private javax.swing.JCheckBox chkLunesDes, chkMartesDes, chkMiercolesDes, chkJuevesDes, chkViernesDes, chkSabadoDes, chkDomingoDes;
    private javax.swing.JButton btnAplicarDesbloqueo, btnVolverDesbloqueo;

    // Escaneo
    private javax.swing.JTextField tfQR;
    private javax.swing.JButton btnEscanear, btnVolverScan;

    public AdminUI(Gimnasio gym) {
        super("Panel Administrador");
        this.gym = gym;
        initUI();
        mostrarInicio();
        refrescarMetricas();
    }

    private void initUI() {
        setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 360);
        setLocationRelativeTo(null);
        setLayout(new java.awt.BorderLayout(8, 8));

        // Cabecera
        var header = new javax.swing.JPanel(new java.awt.GridLayout(1, 2, 8, 8));
        lblReservas = new javax.swing.JLabel("# Reservas semana (L–D): 0", javax.swing.SwingConstants.LEFT);
        lblDentro = new javax.swing.JLabel("# En proceso (dentro ahora): 0", javax.swing.SwingConstants.LEFT);
        header.add(lblReservas);
        header.add(lblDentro);
        add(header, java.awt.BorderLayout.NORTH);

        panelCentral = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
        add(panelCentral, java.awt.BorderLayout.CENTER);

        // Panel INICIO
        panelInicio = new javax.swing.JPanel(new java.awt.GridLayout(1, 3, 16, 16));
        var btnBloquear = new javax.swing.JButton("Bloquear días");
        var btnDesbloquear = new javax.swing.JButton("Desbloquear días");
        var btnScan = new javax.swing.JButton("Escanear");
        btnBloquear.setFont(btnBloquear.getFont().deriveFont(16f));
        btnDesbloquear.setFont(btnDesbloquear.getFont().deriveFont(16f));
        btnScan.setFont(btnScan.getFont().deriveFont(16f));
        panelInicio.add(btnBloquear);
        panelInicio.add(btnDesbloquear);
        panelInicio.add(btnScan);
        btnBloquear.addActionListener(e -> mostrarBloqueo());
        btnDesbloquear.addActionListener(e -> mostrarDesbloqueo());
        btnScan.addActionListener(e -> mostrarEscaneo());

        // Panel BLOQUEO
        panelBloqueo = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
        var filaLabels = new javax.swing.JPanel(new java.awt.GridLayout(1, 7, 4, 4));
        filaLabels.add(new javax.swing.JLabel("Lunes", javax.swing.SwingConstants.CENTER));
        filaLabels.add(new javax.swing.JLabel("Martes", javax.swing.SwingConstants.CENTER));
        filaLabels.add(new javax.swing.JLabel("Miércoles", javax.swing.SwingConstants.CENTER));
        filaLabels.add(new javax.swing.JLabel("Jueves", javax.swing.SwingConstants.CENTER));
        filaLabels.add(new javax.swing.JLabel("Viernes", javax.swing.SwingConstants.CENTER));
        filaLabels.add(new javax.swing.JLabel("Sábado", javax.swing.SwingConstants.CENTER));
        filaLabels.add(new javax.swing.JLabel("Domingo", javax.swing.SwingConstants.CENTER));
        var filaChecks = new javax.swing.JPanel(new java.awt.GridLayout(1, 7, 4, 4));
        chkLunes = new javax.swing.JCheckBox();
        chkMartes = new javax.swing.JCheckBox();
        chkMiercoles = new javax.swing.JCheckBox();
        chkJueves = new javax.swing.JCheckBox();
        chkViernes = new javax.swing.JCheckBox();
        chkSabado = new javax.swing.JCheckBox();
        chkDomingo = new javax.swing.JCheckBox();
        filaChecks.add(chkLunes);
        filaChecks.add(chkMartes);
        filaChecks.add(chkMiercoles);
        filaChecks.add(chkJueves);
        filaChecks.add(chkViernes);
        filaChecks.add(chkSabado);
        filaChecks.add(chkDomingo);
        var filaAccion = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        btnAceptarBloqueo = new javax.swing.JButton("Aceptar");
        btnVolverBloqueo = new javax.swing.JButton("Volver");
        filaAccion.add(btnAceptarBloqueo);
        filaAccion.add(btnVolverBloqueo);
        panelBloqueo.add(filaLabels, java.awt.BorderLayout.NORTH);
        panelBloqueo.add(filaChecks, java.awt.BorderLayout.CENTER);
        panelBloqueo.add(filaAccion, java.awt.BorderLayout.SOUTH);
        btnAceptarBloqueo.addActionListener(e -> aplicarBloqueosSeleccionados());
        btnVolverBloqueo.addActionListener(e -> mostrarInicio());
        
        // Panel DESBLOQUEO
        panelDesbloqueo = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
        var filaLabelsDes = new javax.swing.JPanel(new java.awt.GridLayout(1, 7, 4, 4));
        filaLabelsDes.add(new javax.swing.JLabel("Lunes", javax.swing.SwingConstants.CENTER));
        filaLabelsDes.add(new javax.swing.JLabel("Martes", javax.swing.SwingConstants.CENTER));
        filaLabelsDes.add(new javax.swing.JLabel("Miércoles", javax.swing.SwingConstants.CENTER));
        filaLabelsDes.add(new javax.swing.JLabel("Jueves", javax.swing.SwingConstants.CENTER));
        filaLabelsDes.add(new javax.swing.JLabel("Viernes", javax.swing.SwingConstants.CENTER));
        filaLabelsDes.add(new javax.swing.JLabel("Sábado", javax.swing.SwingConstants.CENTER));
        filaLabelsDes.add(new javax.swing.JLabel("Domingo", javax.swing.SwingConstants.CENTER));
        var filaChecksDes = new javax.swing.JPanel(new java.awt.GridLayout(1, 7, 4, 4));
        chkLunesDes = new javax.swing.JCheckBox();
        chkMartesDes = new javax.swing.JCheckBox();
        chkMiercolesDes = new javax.swing.JCheckBox();
        chkJuevesDes = new javax.swing.JCheckBox();
        chkViernesDes = new javax.swing.JCheckBox();
        chkSabadoDes = new javax.swing.JCheckBox();
        chkDomingoDes = new javax.swing.JCheckBox();
        filaChecksDes.add(chkLunesDes);
        filaChecksDes.add(chkMartesDes);
        filaChecksDes.add(chkMiercolesDes);
        filaChecksDes.add(chkJuevesDes);
        filaChecksDes.add(chkViernesDes);
        filaChecksDes.add(chkSabadoDes);
        filaChecksDes.add(chkDomingoDes);
        var filaAccionDes = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        btnAplicarDesbloqueo = new javax.swing.JButton("Aplicar desbloqueo");
        btnVolverDesbloqueo = new javax.swing.JButton("Volver");
        filaAccionDes.add(btnAplicarDesbloqueo);
        filaAccionDes.add(btnVolverDesbloqueo);
        panelDesbloqueo.add(filaLabelsDes, java.awt.BorderLayout.NORTH);
        panelDesbloqueo.add(filaChecksDes, java.awt.BorderLayout.CENTER);
        panelDesbloqueo.add(filaAccionDes, java.awt.BorderLayout.SOUTH);
        btnAplicarDesbloqueo.addActionListener(e -> aplicarDesbloqueosSeleccionados());
        btnVolverDesbloqueo.addActionListener(e -> mostrarInicio());

        // Panel ESCANEO
        panelEscaneo = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
        var filaQR = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        tfQR = new javax.swing.JTextField(28);
        btnEscanear = new javax.swing.JButton("Escanear QR");
        btnVolverScan = new javax.swing.JButton("Volver");
        filaQR.add(new javax.swing.JLabel("QR token: "));
        filaQR.add(tfQR);
        filaQR.add(btnEscanear);
        filaQR.add(btnVolverScan);
        panelEscaneo.add(filaQR, java.awt.BorderLayout.NORTH);
       
    

btnEscanear.addActionListener(e -> {
    String token = tfQR.getText().trim();
    String msg = gym.escanearQR(token);
    JOptionPane.showMessageDialog(this, msg);

    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).sincronizarAsistenciasDesdeDB(); // NUEVO
    }

    refrescarMetricas();
});



        btnVolverScan.addActionListener(e -> mostrarInicio());
    }

    private void mostrarInicio() {
        panelCentral.removeAll();
        panelCentral.add(panelInicio, java.awt.BorderLayout.CENTER);
        panelCentral.revalidate();
        panelCentral.repaint();
        refrescarMetricas();
    }

    private void mostrarBloqueo() {
        panelCentral.removeAll();
        panelCentral.add(panelBloqueo, java.awt.BorderLayout.CENTER);
        panelCentral.revalidate();
        panelCentral.repaint();
        refrescarMetricas();
    }

    private void mostrarDesbloqueo() {
        panelCentral.removeAll();
        panelCentral.add(panelDesbloqueo, java.awt.BorderLayout.CENTER);
        panelCentral.revalidate();
        panelCentral.repaint();
        refrescarMetricas();
    }

    private void mostrarEscaneo() {
        panelCentral.removeAll();
        panelCentral.add(panelEscaneo, java.awt.BorderLayout.CENTER);
        panelCentral.revalidate();
        panelCentral.repaint();
        refrescarMetricas();
    }

    private void aplicarBloqueosSeleccionados() {
        var semana = gym.semanaActualLunesADomingo();
        boolean[] seleccion = new boolean[] {
            chkLunes.isSelected(), chkMartes.isSelected(), chkMiercoles.isSelected(),
            chkJueves.isSelected(), chkViernes.isSelected(), chkSabado.isSelected(), chkDomingo.isSelected()
        };
        int bloqueados = 0;
        for (int i = 0; i < semana.size(); i++) {
            if (seleccion[i]) {
                gym.bloquearDia(semana.get(i));
                bloqueados++;
            }
        }
        refrescarMetricas();
        javax.swing.JOptionPane.showMessageDialog(this, "Bloqueados " + bloqueados + " día(s) de la semana actual.");
        refrescarMetricas();
    }

    private void aplicarDesbloqueosSeleccionados() {
        var semana = gym.semanaActualLunesADomingo();
        boolean[] seleccion = new boolean[] {
            chkLunesDes.isSelected(), chkMartesDes.isSelected(), chkMiercolesDes.isSelected(),
            chkJuevesDes.isSelected(), chkViernesDes.isSelected(), chkSabadoDes.isSelected(), chkDomingoDes.isSelected()
        };
        int desbloqueados = 0;
        for (int i = 0; i < semana.size(); i++) {
            if (seleccion[i]) {
                if (gym.desbloquearDia(semana.get(i))) {
                    desbloqueados++;
                }
            }
        }
        javax.swing.JOptionPane.showMessageDialog(this, "Desbloqueados " + desbloqueados + " día(s) de la semana actual.");
        refrescarMetricas();
    }



private void refrescarMetricas() {
   

    // Mantén tus limpiezas actuales
    if (gym instanceof GimnasioPersistente) {
        ((GimnasioPersistente) gym).limpiarBloqueosAntiguos();
        ((GimnasioPersistente) gym).limpiarCitasVencidas();
        ((GimnasioPersistente) gym).sincronizarCitasDesdeDB();
        ((GimnasioPersistente) gym).sincronizarAsistenciasDesdeDB();
    } else {
        gym.limpiarBloqueosAntiguos();
        gym.eliminarCitasPasadas();
        gym.eliminarCitasInvalidas();
    }

    // 1) Actualizar métricas en pantalla
    lblReservas.setText("# Reservas semana (L–D): " + gym.reservasSemana());
    lblDentro.setText("# En proceso (dentro ahora): " + gym.usuariosDentro());
}


    
}
