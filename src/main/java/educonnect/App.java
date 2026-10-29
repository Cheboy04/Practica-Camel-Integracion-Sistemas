package educonnect;

import org.apache.camel.main.Main;

public class App {

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new MatriculasRoute());
        main.configure().setDurationMaxIdleSeconds(5);
        main.run();
    }
}
