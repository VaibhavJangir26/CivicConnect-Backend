package com.bluewave.civicconnect;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CivicConnectApplication {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(CivicConnectApplication.class, args);
    }
}