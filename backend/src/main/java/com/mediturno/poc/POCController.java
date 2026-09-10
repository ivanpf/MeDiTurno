package com.mediturno.poc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class POCController {

	@Autowired
	private ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

	@GetMapping("/api/health")
	public Map<String, Object> health() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("estado", "OK");
		body.put("servicio", "MeDiTurno POC");
		return body;
	}

	@GetMapping("/api/dbcheck")
	public ResponseEntity<Map<String, Object>> dbCheck() {
		Map<String, Object> body = new LinkedHashMap<>();
		JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
		if (jdbcTemplate == null) {
			body.put("estado", "BD_NO_CONFIGURADA");
			body.put("detalle", "Faltan las variables DB_URL/DB_USER/DB_PASS");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
		}
		try {
			String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
			body.put("estado", "OK");
			body.put("base", "PostgreSQL");
			body.put("version", version);
			return ResponseEntity.ok(body);
		} catch (Exception ex) {
			body.put("estado", "ERROR_BD");
			body.put("detalle", ex.getMessage());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
		}
	}

}