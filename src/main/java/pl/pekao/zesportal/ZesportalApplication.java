package pl.pekao.zesportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZesportalApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZesportalApplication.class, args);
	}

}
