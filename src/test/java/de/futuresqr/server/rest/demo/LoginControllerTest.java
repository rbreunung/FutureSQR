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
package de.futuresqr.server.rest.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

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

import de.futuresqr.server.model.frontend.UserProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used by naming convention to test {@link LoginController}.
 * 
 * @author Robert Breunung
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Slf4j
public class LoginControllerTest {

	private static final String REST_PATH_CSRF = "/rest/user/csrf";

	private static final String MESSAGE = "message";

	@LocalServerPort
	int serverPort;

	@Autowired
	private TestRestTemplate webclient;

	@Test
	public void postLoginTest_misssingCsfr_messageReplied() {

		ResponseEntity<CsrfDto> csrfEntity = webclient
				.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF, CsrfDto.class);
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(null);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> loginResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(loginResponse.getStatusCode().is4xxClientError(), "User login should fail.");
	}

	@Test
	public void postLoginTest_validRequestWithCookieAuthentication_messageReplied() {

		ResponseEntity<CsrfDto> csrfEntity = webclient
				.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF, CsrfDto.class);
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(null);
		HttpHeaders header = getHeader(csrfData, sessionId);

		ResponseEntity<String> loginResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(loginResponse.getStatusCode().is2xxSuccessful(), "User login successful.");
	}

	@Test
	public void postLoginTest_validRequestWithUrlAuthentication_messageReplied() {

		ResponseEntity<CsrfDto> csrfEntity = webclient
				.getForEntity("http://localhost:" + serverPort + REST_PATH_CSRF, CsrfDto.class);
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(csrfData);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> loginResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(loginResponse.getStatusCode().is2xxSuccessful(), "User login successful.");
	}

	@Test
	public void postLoginTest_validRequestWithUrlAuthentication_sameCsrfToken() {

		final String getCsfrUri = "http://localhost:" + serverPort + REST_PATH_CSRF;
		ResponseEntity<CsrfDto> csrfEntity = webclient.getForEntity(getCsfrUri, CsrfDto.class);
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(csrfData);
		HttpHeaders header = getHeader(null, sessionId);

		webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		// Cookie and CSRF ID remain same in case of the manual end point
		ResponseEntity<CsrfDto> newCsrfEntity = webclient.exchange(getCsfrUri, GET, new HttpEntity<>(header),
				CsrfDto.class);
		assertTrue(newCsrfEntity.getBody().equals(csrfEntity.getBody()), "User with new Cookie.");
	}

	@Test
	public void postTest_authenticated_success() {

		final String getCsfrUri = "http://localhost:" + serverPort + REST_PATH_CSRF;
		ResponseEntity<CsrfDto> csrfResponse = webclient.getForEntity(getCsfrUri, CsrfDto.class);

		CsrfDto csrfData = csrfResponse.getBody();
		final String anonymousSession = getNewSetCookieContent(csrfResponse);
		String loginUri = getLoginUri(null);
		HttpHeaders header = getHeader(csrfData, anonymousSession);
		ResponseEntity<String> loginPost = webclient.postForEntity(loginUri, new HttpEntity<>(header), String.class);

		String sessionCookie = getNewSetCookieContent(loginPost);
		header = getHeader(csrfResponse.getBody(), sessionCookie, anonymousSession);
		ResponseEntity<String> response = webclient.exchange(getPostTestMessageUri(null, MESSAGE), POST,
				new HttpEntity<>(header), String.class);

		assertTrue(response.getBody().equals(MESSAGE), "Controller shall respond with same message.");
	}

	private HttpHeaders getHeader(CsrfDto csrfData, String... cookies) {
		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		for (String cookie : cookies) {
			if (cookie != null) {
				header.add(COOKIE, cookie);
			}
		}
		if (csrfData != null) {
			header.add(csrfData.getHeaderName(), csrfData.getToken());
		}
		return header;
	}

	private String getLoginUri(CsrfDto csrfData) {
		UriBuilder builder = new DefaultUriBuilderFactory("http://localhost:" + serverPort + "/rest/login").builder();
		builder.queryParam(UserProperties.LOGIN_NAME, "user").queryParam(UserProperties.PASSWORD, "password");
		if (csrfData != null) {
			builder.queryParam(csrfData.getParameterName(), csrfData.getToken());
		}
		return builder.build().toString();
	}

	/**
	 * Get the cookie set by the server response.
	 */
	private String getNewSetCookieContent(ResponseEntity<?> entity) {
		List<String> list = entity.getHeaders().get(SET_COOKIE);
		assertEquals(1, list.size(), "Expect a cookie set request.");
		log.info("Cookie set by server: {}", list.toString());
		return list.get(0);
	}

	private String getPostTestMessageUri(CsrfDto csrfData, String message) {
		UriBuilder builder = new DefaultUriBuilderFactory("http://localhost:" + serverPort + "/rest/test/post")
				.builder();
		if (csrfData != null) {
			builder.queryParam(csrfData.getParameterName(), csrfData.getToken());
		}
		if (message != null) {
			builder.queryParam("message", message);
		}
		return builder.build().toString();
	}
}
