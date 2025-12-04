
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class GymAppTests {
    private Gimnasio gym;
    private Usuario casual;

    @Before
    public void setUp() {
        gym = new Gimnasio();
        casual = gym.registrar("casual", "1234");
    }

    @Test
    public void loginDebeFuncionar() {
        Usuario u = gym.login("casual", "1234");
        assertNotNull(u);
        assertEquals("casual", u.nombre);
    }


@Test
public void registrarDebeEliminarEspacios() {
    Usuario u = new Usuario(" Juan  ", " clave  ");
    assertEquals("Juan", u.nombre);
    assertEquals("clave", u.password);
}


@Test
public void loginDebeFuncionarConEspacios() {
    Gimnasio gym = new Gimnasio();
    gym.registrar("Juan", "clave");
    Usuario u = gym.login(" Juan  ", " clave  ");
    assertNotNull(u);
    assertEquals("Juan", u.nombre);
}


@Test
public void persistenciaDebeGuardarSinEspacios() {
    DatabaseManager db = new DatabaseManager(":memory:");
    GimnasioPersistente gymP = new GimnasioPersistente(db);
    Usuario u = gymP.registrar("  Maria  ", "  clave  ");
    assertTrue(db.existeUsuario("Maria"));
}


@Test
public void agendarSemanaDebeCrearCitasValidas() {
    var semana = gym.semanaActualLunesADomingo();
    LocalTime horaSegura = LocalTime.now().plusHours(1);
    for (LocalDate d : semana) {
        if (!d.isBefore(LocalDate.now())) { // evitar días pasados
            gym.agendar(casual, d, horaSegura, false);
        }
    }
    assertTrue("Debe tener al menos 1 cita", casual.citas.size() >= 1);
}


@Test
public void reagendarDiaDebeActualizarHora() {
    LocalDate fechaSegura = LocalDate.now().plusDays(2); // siempre futuro
    gym.agendar(casual, fechaSegura, LocalTime.of(10, 0), false);
    Cita cambiada = gym.reagendarDia(casual, fechaSegura, "16:00", false);
    assertEquals(LocalTime.of(16, 0), cambiada.hora);
}

    @Test
    public void bloquearDiaDebeEliminarCitas() {
        LocalDate viernes = gym.semanaActualLunesADomingo().get(4);
        gym.agendar(casual, viernes, LocalTime.of(10, 0), false);
        gym.bloquearDia(viernes);
        assertTrue(gym.diasBloqueadosSemanaActual().contains(viernes));
        assertTrue(casual.citas.stream().noneMatch(c -> c.fecha.equals(viernes)));
    }

    
@Test
public void generarQRDebeCambiarToken() {
    LocalDate hoy = LocalDate.now();
    LocalTime horaSegura = LocalTime.now().plusHours(1); // siempre futura
    Cita c = gym.agendar(casual, hoy, horaSegura, false);
    String qrPrevio = c.qr;
    gym.actualizarQR(c);
    assertNotEquals(qrPrevio, c.qr);
}


    @Test
    public void escanearQRDebeRegistrarAsistencia() {
        LocalDate hoy = LocalDate.now();
        Cita c = gym.agendar(casual, hoy, LocalTime.now().plusMinutes(1), false);
        String msg = gym.escanearQR(c.qr);
        assertTrue(msg.startsWith("Entrada registrada"));
    }

   
@Test
public void diasPasadosDeberianEstarDeshabilitados() {
    var bloqueadosYPasados = gym.diasBloqueadosYPasadosSemanaActual();
    LocalDate hoy = LocalDate.now();
    LocalDate lunesActual = hoy.with(java.time.DayOfWeek.MONDAY);
    if (!hoy.equals(lunesActual)) { // Si no es lunes
        LocalDate diaPasado = hoy.minusDays(1); // Un día anterior dentro de la semana
        assertTrue(bloqueadosYPasados.contains(diaPasado));
    }
}


    // =====================  TESTS PARA PERSISTENCIA =====================
    @Test
    public void persistenciaRegistrarUsuarioDebeGuardarEnDB() {
        DatabaseManager db = new DatabaseManager(":memory:");
        GimnasioPersistente gymP = new GimnasioPersistente(db);
        Usuario u = gymP.registrar("juan", "clave");
        assertNotNull(u);
        assertTrue(db.existeUsuario("juan"));
    }

    @Test
    public void persistenciaAgendarDebeGuardarCita() {
        DatabaseManager db = new DatabaseManager(":memory:");
        GimnasioPersistente gymP = new GimnasioPersistente(db);
        Usuario u = gymP.registrar("ana", "clave");
        Cita c = gymP.agendar(u, LocalDate.now().plusDays(1), LocalTime.of(10, 0), false);
        List<Cita> citasDB = db.getCitasPorUsuario("ana");
        assertEquals(1, citasDB.size());
        assertEquals(c.fecha, citasDB.get(0).fecha);
    }

    @Test
    public void persistenciaReagendarDebeActualizarHoraYQR() {
        DatabaseManager db = new DatabaseManager(":memory:");
        GimnasioPersistente gymP = new GimnasioPersistente(db);
        Usuario u = gymP.registrar("maria", "clave");
        LocalDate fecha = LocalDate.now().plusDays(2);
        Cita c = gymP.agendar(u, fecha, LocalTime.of(8, 0), false);
        String qrPrevio = c.qr;
        Cita actualizado = gymP.reagendarDia(u, fecha, "16:00", false);
        assertEquals(LocalTime.of(16, 0), actualizado.hora);
        assertNotEquals(qrPrevio, actualizado.qr);
    }

    @Test
    public void persistenciaBloquearYDesbloquearDebeActualizarDB() {
        DatabaseManager db = new DatabaseManager(":memory:");
        GimnasioPersistente gymP = new GimnasioPersistente(db);
        LocalDate fecha = LocalDate.now().plusDays(3);
        gymP.bloquearDia(fecha);
        assertTrue(db.getBloqueosSemana().contains(fecha));
        boolean desbloqueado = gymP.desbloquearDia(fecha);
        assertTrue(desbloqueado);
        assertFalse(db.getBloqueosSemana().contains(fecha));
    }

   

    @Test
    public void persistenciaLimpiarCitasVencidasDebeEliminarEnDB() {
        DatabaseManager db = new DatabaseManager(":memory:");
        GimnasioPersistente gymP = new GimnasioPersistente(db);
        Usuario u = gymP.registrar("luis", "clave");
        LocalDate fechaPasada = LocalDate.now().minusDays(1);
        Cita c = new Cita(fechaPasada, LocalTime.of(10, 0), false, "qr-test");
        u.citas.add(c);
        db.insertCita(db.getUsuarioId("luis"), fechaPasada.toString(), "10:00", false, "qr-test", false, 10);
        gymP.limpiarCitasVencidas();
        assertEquals(0, db.getCitasPorUsuario("luis").size());

    }
    
@Test
public void citaDebeProgramarNotificacionCorrectamente() {
    LocalDate fecha = LocalDate.now().plusDays(1); // cita futura
    LocalTime hora = LocalTime.of(10, 0);
    Cita c = gym.agendar(casual, fecha, hora, false);
    c.notificar = true;
    c.minutosAntes = 10;

    long delay = gym.calcularDelay(c);
    assertTrue("El delay debe ser positivo", delay > 0);

    // Verificar que el delay sea aproximadamente (tiempo hasta cita - 10 min)
    long tiempoHastaCita = java.time.Duration.between(
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.of(fecha, hora)
    ).toMillis();
    assertTrue("El delay debe ser menor que el tiempo total hasta la cita",
        delay < tiempoHastaCita);

    // Simulación: si delay > 0 y notificar = true, se debería mostrar el menú (UI)
    assertTrue("Debe estar marcada para notificar", c.notificar);
}


@Test
public void registrarUsuarioDuplicadoDebeRetornarNull() {
    Gimnasio gym = new Gimnasio();
    gym.registrar("Juan", "clave");
    Usuario u = gym.registrar("Juan", "clave");
    assertNull(u);
}


@Test
public void loginConPasswordIncorrectoDebeRetornarNull() {
    Gimnasio gym = new Gimnasio();
    gym.registrar("Juan", "clave");
    Usuario u = gym.login("Juan", "otraClave");
    assertNull(u);
}


@Test(expected = RuntimeException.class)
public void agendarEnDiaBloqueadoDebeFallar() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Ana", "1234");
    java.time.LocalDate fecha = java.time.LocalDate.now().plusDays(1);
    gym.bloquearDia(fecha);
    gym.agendar(u, fecha, java.time.LocalTime.of(10, 0), false);
}


@Test
public void reagendarDebeCambiarQR() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Ana", "1234");
    java.time.LocalDate fecha = java.time.LocalDate.now().plusDays(1);
    Cita c = gym.agendar(u, fecha, java.time.LocalTime.of(10, 0), false);
    String qrAnterior = c.qr;
    Cita nueva = gym.reagendarDia(u, fecha, "12:00", false);
    assertNotEquals(qrAnterior, nueva.qr);
}


@Test
public void escanearQRFueraDeRangoDebeRetornarMensaje() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Luis", "clave");
    java.time.LocalDate fecha = java.time.LocalDate.now();
    java.time.LocalTime hora = java.time.LocalTime.now().plusHours(2);
    Cita c = gym.agendar(u, fecha, hora, false);
    String msg = gym.escanearQR(c.qr);
    assertTrue(msg.contains("fuera de rango"));
}


@Test
public void agendarConCandadoDebeReducirStock() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Pedro", "clave");
    int stockInicial = 80;
    Cita c = gym.agendar(u, java.time.LocalDate.now().plusDays(1), java.time.LocalTime.of(10, 0), true);
    assertTrue(gym.candadosTotales < stockInicial);
}


@Test
public void bloqueoYDesbloqueoDebeActualizarDB() {
    DatabaseManager db = new DatabaseManager(":memory:");
    GimnasioPersistente gymP = new GimnasioPersistente(db);
    java.time.LocalDate fecha = java.time.LocalDate.now().plusDays(2);
    gymP.bloquearDia(fecha);
    assertTrue(db.getBloqueosSemana().contains(fecha));
    gymP.desbloquearDia(fecha);
    assertFalse(db.getBloqueosSemana().contains(fecha));
}


@Test(expected = RuntimeException.class)
public void agendarEnHoraPasadaDebeFallar() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Carlos", "1234");
    java.time.LocalDate hoy = java.time.LocalDate.now();
    java.time.LocalTime horaPasada = java.time.LocalTime.now().minusMinutes(30);
    gym.agendar(u, hoy, horaPasada, false);
}





@Test
public void escanearQRInvalidoDebeRetornarMensaje() {
    Gimnasio gym = new Gimnasio();
    String msg = gym.escanearQR("qr-inexistente");
    assertEquals("QR inválido", msg);
}


@Test
public void persistenciaReagendarDebeActualizarDB() {
    DatabaseManager db = new DatabaseManager(":memory:");
    GimnasioPersistente gymP = new GimnasioPersistente(db);
    Usuario u = gymP.registrar("Maria", "clave");
    java.time.LocalDate fecha = java.time.LocalDate.now().plusDays(1);
    Cita c = gymP.agendar(u, fecha, java.time.LocalTime.of(10, 0), false);
    String qrAnterior = c.qr;
    Cita actualizado = gymP.reagendarDia(u, fecha, "16:00", false);
    assertNotEquals(qrAnterior, actualizado.qr);
    assertEquals(java.time.LocalTime.of(16, 0), actualizado.hora);
}


@Test
public void agendarConCandadoSinStockDebeAgendarSinCandado() {
    Gimnasio gym = new Gimnasio();
    gym.candadosTotales = 0; // Sin stock
    Usuario u = gym.registrar("Pedro", "clave");

    Cita c = gym.agendar(u, java.time.LocalDate.now().plusDays(1), java.time.LocalTime.of(10, 0), true);

    assertNotNull(c); // La cita debe crearse
    assertFalse(c.candado); // Debe agendarse sin candado
}


@Test
public void agendarConCandadoConStockDebeAsignarCandado() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Ana", "clave");
    int stockInicial = gym.candadosTotales;
    Cita c = gym.agendar(u, java.time.LocalDate.now().plusDays(1), java.time.LocalTime.of(10, 0), true);
    assertTrue(c.candado);
    assertEquals(stockInicial - 1, gym.candadosTotales);
}


@Test
public void agendarSinCandadoDebeFuncionarAunqueNoHayStock() {
    Gimnasio gym = new Gimnasio();
    gym.candadosTotales = 0;
    Usuario u = gym.registrar("Luis", "clave");
    Cita c = gym.agendar(u, java.time.LocalDate.now().plusDays(1), java.time.LocalTime.of(10, 0), false);
    assertNotNull(c);
    assertFalse(c.candado);
}


@Test(expected = RuntimeException.class)
public void reagendarEnDiaBloqueadoDebeFallar() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Ana", "clave");
    java.time.LocalDate fecha = java.time.LocalDate.now().plusDays(2);
    gym.agendar(u, fecha, java.time.LocalTime.of(10, 0), false);
    gym.bloquearDia(fecha);
    gym.reagendarDia(u, fecha, "12:00", false);
}

@Test
public void eliminarCitaDebeReducirReservasSemana() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Luis", "clave");
    java.time.LocalDate fecha = java.time.LocalDate.now(); // Hoy, dentro de la semana
    Cita c = gym.agendar(u, fecha, java.time.LocalTime.now().plusHours(1), false);
    assertEquals(1, gym.reservasSemana());
    u.citas.remove(c);
    gym.citas.remove(c);
    assertEquals(0, gym.reservasSemana());
}


@Test
public void calcularDelayDebeSerPositivo() {
    Gimnasio gym = new Gimnasio();
    Usuario u = gym.registrar("Ana", "clave");
    java.time.LocalDate fecha = java.time.LocalDate.now().plusDays(1);
    Cita c = gym.agendar(u, fecha, java.time.LocalTime.of(10, 0), false);
    c.notificar = true;
    c.minutosAntes = 15;
    long delay = gym.calcularDelay(c);
    assertTrue(delay > 0);

}






}
