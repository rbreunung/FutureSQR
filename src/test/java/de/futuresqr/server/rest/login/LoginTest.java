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
package de.futuresqr.server.rest.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Demonstration tests for the security login using CSRF with header and form
 * data.
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
	public void getLoginTest_validRequestWithAuthentication_messageReplied() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(csrfData);
		HttpHeaders header = getHeader(null, sessionId);
		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);
		sessionId = getNewSetCookieContent(postResponse);
		final String randomMessage = UUID.randomUUID().toString();
		uri = getPostTestMessageUri(null, randomMessage);
		header = getJsonHeader(null, sessionId);

		postResponse = webclient.exchange(uri, HttpMethod.GET, new HttpEntity<>(header), String.class);

		log.trace("Header : {}", postResponse.getHeaders().toString());
		log.trace("Body   : {}", postResponse.getBody());

		assertTrue(postResponse.getStatusCode().is2xxSuccessful(), "Answer shall be successful (ok).");
		assertEquals(randomMessage, postResponse.getBody(), "Answer shall return the message.");
	}

	@Test
	public void getUser_validRequestWithAuthentication_statusOk() {

		String sessionId = getLoginSessionId();
		String uri = "http://localhost:" + serverPort + "/rest/user";
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).header(COOKIE, sessionId).accept(APPLICATION_JSON)
				.build();

		ResponseEntity<String> postResponse = webclient.exchange(requestEntity, String.class);

		log.trace("Header : {}", postResponse.getHeaders().toString());
		log.trace("Body   : {}", postResponse.getBody());
		assertTrue(postResponse.getStatusCode().is2xxSuccessful(), "Answer shall be successful (ok).");
		assertTrue(postResponse.getBody().contains("\"user\" : ["), "Answer shall return the JSON.");
	}

	@Test
	public void postLogin_missingCsrf_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(null);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertEquals(403, postResponse.getStatusCode().value(),
				"Access shall be forbidden with unsafe authentication.");
	}

	@Test
	public void postLogin_missingCsrfTokenAndSessionCookie_statusForbidden() {

		String uri = getLoginUri(null);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(getHeader(null, null)),
				String.class);

		assertEquals(403, postResponse.getStatusCode().value(),
				"Access shall be forbidden with unsafe authentication.");
	}

	@Test
	public void postLogin_missingSessionCookie_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String uri = getLoginUri(csrfData);
		HttpHeaders header = getHeader(csrfData, null);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "Access shall be forbidden with missing session.");
	}

	@Test
	public void postLogin_validRequestFormCsrfToken_authenticationRedirection() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(csrfData);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postResponse.getStatusCode().is3xxRedirection(), "User will be redirected to login success page.");
	}

	@Test
	public void postLogin_validRequestHeaderCsrfToken_authenticationRedirection() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getNewSetCookieContent(csrfEntity);
		String uri = getLoginUri(null);
		HttpHeaders header = getHeader(csrfData, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postResponse.getStatusCode().is3xxRedirection(), "User will be redirected to login success page.");
	}

	@Test
	public void postLoginTest_requestWithoutAuthentication_statusForbidden() {

		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(null, randomMessage);
		HttpHeaders sessionHeader = getHeader(null, null);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "User without cookie and CSRF token expected.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthentication_messageReplied() {

		String loginSessionId = getLoginSessionId();
		CsrfDto loginCsrf = getLoginCsrfToken(loginSessionId).getBody();
		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(loginCsrf, randomMessage);
		HttpHeaders sessionHeader = getHeader(null, loginSessionId);

		ResponseEntity<String> messageResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertTrue(messageResponse.getStatusCode().is2xxSuccessful(), "Answer shall be successful (ok).");
		assertEquals(randomMessage, messageResponse.getBody(), "Answer shall return the message.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthenticationWithoutCsrf_statusForbidden() {
		String loginSessionId = getLoginSessionId();

		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(null, randomMessage);
		HttpHeaders sessionHeader = getHeader(null, loginSessionId);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "User without cookie and CSRF token expected.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthenticationWithoutNewCookie_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		String firstSessionId = getNewSetCookieContent(csrfEntity);
		String loginUri = getLoginUri(csrfEntity.getBody());
		HttpHeaders header = getHeader(null, firstSessionId);
		String loginSessionId = getNewSetCookieContent(
				webclient.postForEntity(loginUri, new HttpEntity<>(header), String.class));
		CsrfDto loginCsrfToken = getLoginCsrfToken(loginSessionId).getBody();
		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(loginCsrfToken, randomMessage);
		HttpHeaders sessionHeader = getHeader(loginCsrfToken, firstSessionId);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "User without cookie and CSRF token expected.");
	}

	private ResponseEntity<CsrfDto> getCsfrEntity() {
		return webclient.getForEntity("http://localhost:" + serverPort + "/rest/login/csrf", CsrfDto.class);
	}

	private HttpHeaders getHeader(CsrfDto csrfData, String sessionId) {
		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		if (sessionId != null) {
			header.add(COOKIE, sessionId);
		}
		if (csrfData != null) {
			header.add(csrfData.getHeaderName(), csrfData.getToken());
		}
		return header;
	}

	private HttpHeaders getJsonHeader(CsrfDto csrfData, String sessionId) {
		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		header.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		if (sessionId != null) {
			header.add(COOKIE, sessionId);
		}
		if (csrfData != null) {
			header.add(csrfData.getHeaderName(), csrfData.getToken());
		}
		return header;
	}

	/**
	 * Get the CSRF token for a given session.
	 * 
	 * @param sessionId The session cookie.
	 * @return The response from the web server.
	 */
	private ResponseEntity<CsrfDto> getLoginCsrfToken(String sessionId) {
		HttpHeaders newHeader = getHeader(null, sessionId);
		ResponseEntity<CsrfDto> newCsrfEntity = webclient.exchange(
				"http://localhost:" + serverPort + "/rest/login/csrf", HttpMethod.GET, new HttpEntity<>(newHeader),
				CsrfDto.class);
		log.trace("Session and CSFR after login : {} {}", sessionId, newCsrfEntity.getBody());
		return newCsrfEntity;
	}

	/**
	 * Get a valid cookie session from a login process.
	 * 
	 * @return A cookie.
	 */
	private String getLoginSessionId() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		String firstSessionId = getNewSetCookieContent(csrfEntity);
		String loginUri = getLoginUri(csrfEntity.getBody());
		HttpHeaders header = getHeader(null, firstSessionId);
		ResponseEntity<String> postResponse = webclient.postForEntity(loginUri, new HttpEntity<>(header), String.class);
		log.trace("Session and CSFR before login : {} {}", firstSessionId, csrfEntity.getBody());
		String newSessionId = getNewSetCookieContent(postResponse);
		return newSessionId;
	}

	private String getLoginUri(CsrfDto csrfData) {
		UriBuilder builder = new DefaultUriBuilderFactory("http://localhost:" + serverPort + "/login").builder();
		builder.queryParam("username", "user").queryParam("password", "password");
		if (csrfData != null) {
			builder.queryParam(csrfData.getParameterName(), csrfData.getToken());
		}
		return builder.build().toString();
	}

	/**
	 * Get the cookie set by the server response.
	 */
	private String getNewSetCookieContent(ResponseEntity<?> entity) {
		List<String> list = entity.getHeaders().get("Set-Cookie");
		assertEquals(1, list.size(), "Expect a cookie set request.");
		log.trace("Cookie set by server: {}", list.toString());
		return list.get(0).split(";")[0];
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
