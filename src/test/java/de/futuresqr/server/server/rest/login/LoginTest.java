/**
 * MIT License
 *
 * Copyright (c) 2022 Robert Breunung
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.futuresqr.server.server.rest.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Demonstration tests for the security login using CSRF with header and form data.
 * 
 * @author Robert Breunung
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Slf4j
public class LoginTest {

	@LocalServerPort
	int serverPort;

	@Autowired
	private TestRestTemplate webclient;

	@Test
	public void postLogin_missingCsrf_statusForbidden() {
		ResponseEntity<CsrfDto> entity = getCsfrEntity();
		String sessionId = getNewSetCookieContent(entity);
		String uri = getUri(null);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> postForEntity = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertEquals(403, postForEntity.getStatusCode().value(),
				"Access shall be forbidden with unsafe authentication.");
	}

	@Test
	public void postLogin_missingCsrfTokenAndSessionCookie_statusForbidden() {

		String uri = getUri(null);

		ResponseEntity<String> postForEntity = webclient.postForEntity(uri, new HttpEntity<>(getHeader(null, null)),
				String.class);

		assertEquals(403, postForEntity.getStatusCode().value(),
				"Access shall be forbidden with unsafe authentication.");
	}

	@Test
	public void postLogin_missingSessionCookie_statusForbidden() {
		ResponseEntity<CsrfDto> entity = getCsfrEntity();
		CsrfDto csrfData = entity.getBody();
		String uri = getUri(csrfData);
		HttpHeaders header = getHeader(csrfData, null);

		ResponseEntity<String> postForEntity = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertEquals(403, postForEntity.getStatusCode().value(), "Access shall be forbidden with missing session.");
	}

	@Test
	public void postLogin_validRequestFormCsrfToken_authenticated() {
		ResponseEntity<CsrfDto> entity = getCsfrEntity();
		CsrfDto csrfData = entity.getBody();
		String sessionId = getNewSetCookieContent(entity);
		String uri = getUri(csrfData);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> postForEntity = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postForEntity.getStatusCode().is3xxRedirection(), "User will be redirected to login success page.");
	}

	@Test
	public void postLogin_validRequestHeaderCsrfToken_authenticationRedirection() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getUri(null);
		HttpHeaders header = getHeader(csrfData, sessionId);

		ResponseEntity<String> postForEntity = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postForEntity.getStatusCode().is3xxRedirection(), "User will be redirected to login success page.");
	}

	private ResponseEntity<CsrfDto> getCsfrEntity() {
		return webclient.getForEntity("http://localhost:" + serverPort + "/rest/login/csrf", CsrfDto.class);
	}

	private HttpHeaders getHeader(CsrfDto csrfData, String sessionId) {
		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		if (sessionId != null) {
			header.add(HttpHeaders.COOKIE, sessionId);
		}
		if (csrfData != null) {
			header.add(csrfData.getHeaderName(), csrfData.getToken());
		}
		return header;
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

	private String getUri(CsrfDto csrfData) {
		UriBuilder builder = new DefaultUriBuilderFactory("http://localhost:" + serverPort + "/login").builder();
		builder.queryParam("username", "user").queryParam("password", "password");
		if (csrfData != null) {
			builder.queryParam(csrfData.getParameterName(), csrfData.getToken());
		}
		return builder.build().toString();
	}
}
