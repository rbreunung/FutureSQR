package de.futuresqr.server.server.rest.login;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Slf4j
public class CsrfControllerTest {

	@Autowired
	private TestRestTemplate webclient;

	@LocalServerPort
	int serverPort;
	
	@Test
	public void test() {
		ResponseEntity<String> entity = webclient.getForEntity("http://localhost:" + serverPort + "/rest/login/csrf",
				String.class);
		
		log.info(entity.getBody());
	}
}
