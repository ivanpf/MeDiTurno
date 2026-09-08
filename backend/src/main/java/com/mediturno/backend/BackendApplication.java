package com.mediturno.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Deja disponible el algoritmo para hashear y verificar contrasenas.
	 *
	 * Al marcarlo como @Bean, Spring crea una sola instancia y se la pasa a
	 * quien la necesite (AuthController y DatosDemo) sin que haya que armarla
	 * en cada lugar.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
