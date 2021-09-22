package ncu.cc.proxyz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProxyzApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProxyzApplication.class, args);
	}

}
