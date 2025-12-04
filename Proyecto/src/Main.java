


public class Main {


public static void main(String[] args) {
    DatabaseManager db = new DatabaseManager("gimnasio.db");
    GimnasioPersistente gym = new GimnasioPersistente(db);

   //gym.reiniciarCandados(80);
   
    new UI(gym).mostrarLogin();
   

}
}

