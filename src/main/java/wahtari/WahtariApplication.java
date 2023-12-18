package wahtari;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {"wahtari"})
public class WahtariApplication {


    public static void main(String[] args) {
        SpringApplication.run(WahtariApplication.class, args);
    }

}
