package de.futuresqr.server.rest.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

	private static final String REST_PATH_CSRF = "/rest/user/csrf";

	@LocalServerPort
	int serverPort;

	@Autowired
	private TestRestTemplate webclient;

	@Test
	public void getCsrf() {
		ResponseEntity<String> entity = webclient.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF,
				String.class);
		log.info(entity.getHeaders().toString());
		log.info(entity.getBody());
	}

	@Test
	public void getCsrf_validRequest_cookiePresent() {
		ResponseEntity<String> entity = webclient.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF,
				String.class);

		String result = getNewSetCookieContent(entity);
		assertTrue(result.matches("^XSRF-TOKEN=[a-f0-9\\-]+$"));
	}

	@Test
	public void getCsrf_validUrl_headerNamePresent() throws Exception {

		ResponseEntity<CsrfDto> entity = webclient.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF,
				CsrfDto.class);

		CsrfDto json = entity.getBody();
		assertNotNull(json.getHeaderName(), "CSRF header name expected");
	}

	@Test
	public void getCsrf_validUrl_paramNamePresent() throws Exception {

		ResponseEntity<CsrfDto> entity = webclient.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF,
				CsrfDto.class);

		CsrfDto json = entity.getBody();
		assertNotNull(json.getParameterName(), "CSRF param name expected");
	}

	@Test
	public void getCsrf_validUrl_tokenPresent() throws Exception {

		ResponseEntity<CsrfDto> entity = webclient.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF,
				CsrfDto.class);

		CsrfDto json = entity.getBody();
		assertNotNull(json.getToken(), "CSRF token expected");
	}

	/**
	 * Get the cookie set by the server response.
	 */
	private String getNewSetCookieContent(ResponseEntity<?> entity) {
		List<String> list = entity.getHeaders().get("Set-Cookie");
		assertEquals(1, list.size(), "Expect a cookie set request.");
		log.info("Cookie set by server: {}", list.toString());
		return list.get(0).split(";")[0];
	}
}
