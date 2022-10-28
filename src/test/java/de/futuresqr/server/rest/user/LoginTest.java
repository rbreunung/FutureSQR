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
package de.futuresqr.server.rest.user;

import static de.futuresqr.server.SecurityConfiguration.PATH_REST_USER_AUTHENTICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpMethod.GET;
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

import de.futuresqr.server.model.frontend.UserProperties;
import de.futuresqr.server.rest.demo.CsrfDto;
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
		String sessionId = getFirstNewCookieContent(csrfEntity);
		String uri = getLoginUri(csrfData, true);
		HttpHeaders header = getHeader(null, sessionId);
		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);
		sessionId = postResponse.getHeaders().get(SET_COOKIE).stream().filter(s -> s.startsWith("J")).findAny().get();
		final String randomMessage = UUID.randomUUID().toString();
		uri = getPostTestMessageUri(null, randomMessage);
		header = getJsonHeader(null, sessionId);

		postResponse = webclient.exchange(uri, GET, new HttpEntity<>(header), String.class);

		log.trace("Header : {}", postResponse.getHeaders().toString());
		log.trace("Body   : {}", postResponse.getBody());

		assertTrue(postResponse.getStatusCode().is2xxSuccessful(), "Answer shall be successful (ok).");
		assertEquals(randomMessage, postResponse.getBody(), "Answer shall return the message.");
	}

	@Test
	public void getUser_validRequestWithAuthentication_statusOk() {

		HttpHeaders loginSessionHeader = getLoginSessionHeader();
		String uri = "http://localhost:" + serverPort + "/restdata/user";
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).headers(loginSessionHeader).accept(APPLICATION_JSON)
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
		String sessionId = getFirstNewCookieContent(csrfEntity);
		String uri = getLoginUri(null, true);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertEquals(403, postResponse.getStatusCode().value(),
				"Access shall be forbidden with unsafe authentication.");
	}

	@Test
	public void postLogin_missingCsrfTokenAndSessionCookie_statusForbidden() {

		String uri = getLoginUri(null, true);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(getHeader(null)),
				String.class);

		assertEquals(403, postResponse.getStatusCode().value(),
				"Access shall be forbidden with unsafe authentication.");
	}

	@Test
	public void postLogin_missingSessionCookie_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String uri = getLoginUri(csrfData, true);
		HttpHeaders header = getHeader(csrfData);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "Access shall be forbidden with missing session.");
	}

	@Test
	public void postLogin_validRequestFormCsrfToken_authenticationRedirection() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getFirstNewCookieContent(csrfEntity);
		String uri = getLoginUri(csrfData, true);
		HttpHeaders header = getHeader(null, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postResponse.getStatusCode().is2xxSuccessful(), "User will receive his user data object.");
	}

	@Test
	public void postLogin_validRequestHeaderCsrfToken_authenticationRedirection() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getFirstNewCookieContent(csrfEntity);
		String uri = getLoginUri(null, true);
		HttpHeaders header = getHeader(csrfData, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postResponse.getStatusCode().is2xxSuccessful(), "User will receive his user data object.");
	}

	@Test
	public void postLoginForm_validRequestHeaderCsrfToken_authenticationRedirection() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfData = csrfEntity.getBody();
		String sessionId = getFirstNewCookieContent(csrfEntity);
		String uri = getLoginUri(null, false);
		HttpHeaders header = getHeader(csrfData, sessionId);

		ResponseEntity<String> postResponse = webclient.postForEntity(uri, new HttpEntity<>(header), String.class);

		assertTrue(postResponse.getStatusCode().is2xxSuccessful(), "User will receive his user data object.");
	}

	@Test
	public void postLoginTest_requestWithoutAuthentication_statusForbidden() {

		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(null, randomMessage);
		HttpHeaders sessionHeader = getHeader(null);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "User without cookie and CSRF token expected.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthentication_messageReplied() {

		HttpHeaders loginSessionHeader = getLoginSessionHeader();
		CsrfDto loginCsrf = getLoginCsrfToken(loginSessionHeader).getBody();
		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(loginCsrf, randomMessage);

		ResponseEntity<String> messageResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(loginSessionHeader), String.class);

		assertTrue(messageResponse.getStatusCode().is2xxSuccessful(), "Answer shall be successful (ok).");
		assertEquals(randomMessage, messageResponse.getBody(), "Answer shall return the message.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthenticationWithoutCsrf_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfDto = csrfEntity.getBody();
		String firstSessionId = getFirstNewCookieContent(csrfEntity);
		String loginUri = getLoginUri(csrfDto, true);
		HttpHeaders header = getHeader(null, firstSessionId);

		ResponseEntity<String> postLoginResponse = webclient.postForEntity(loginUri, new HttpEntity<>(header),
				String.class);
		HttpHeaders sessionHeader = getSessionHeader(csrfDto, postLoginResponse, true, true, false);
		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(null, randomMessage);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "User without session cookie cannot post expected.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthenticationWithoutNewCsrfCookie_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfDto = csrfEntity.getBody();
		String firstSessionId = getFirstNewCookieContent(csrfEntity);
		String loginUri = getLoginUri(csrfDto, true);
		HttpHeaders header = getHeader(null, firstSessionId);

		ResponseEntity<String> postLoginResponse = webclient.postForEntity(loginUri, new HttpEntity<>(header),
				String.class);
		HttpHeaders sessionHeader = getSessionHeader(csrfDto, postLoginResponse, true, false, true);
		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(csrfDto, randomMessage);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(403, postResponse.getStatusCode().value(), "User without CSRF cookie cannot post expected.");
	}

	@Test
	public void postLoginTest_validRequestWithAuthenticationWithoutNewSessionCookie_statusForbidden() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		CsrfDto csrfDto = csrfEntity.getBody();
		String firstSessionId = getFirstNewCookieContent(csrfEntity);
		String loginUri = getLoginUri(csrfDto, true);
		HttpHeaders header = getHeader(null, firstSessionId);

		ResponseEntity<String> postLoginResponse = webclient.postForEntity(loginUri, new HttpEntity<>(header),
				String.class);
		HttpHeaders sessionHeader = getSessionHeader(csrfDto, postLoginResponse, false, true, true);
		final String randomMessage = UUID.randomUUID().toString();
		String postMessageUri = getPostTestMessageUri(csrfDto, randomMessage);

		ResponseEntity<String> postResponse = webclient.exchange(postMessageUri, HttpMethod.POST,
				new HttpEntity<>(sessionHeader), String.class);

		assertEquals(302, postResponse.getStatusCode().value(), "User without session cookie cannot post expected.");
	}

	private ResponseEntity<CsrfDto> getCsfrEntity() {
		return webclient.getForEntity("http://localhost:" + serverPort + "/rest/login/csrf", CsrfDto.class);
	}

	/**
	 * Get the cookie set by the server response.
	 */
	private String getFirstNewCookieContent(ResponseEntity<?> entity) {
		List<String> list = entity.getHeaders().get(SET_COOKIE);
		assertEquals(1, list.size(), "Expect a cookie set request.");
		log.trace("Cookie set by server: {}", list.toString());
		return list.get(0);
	}

	private HttpHeaders getHeader(CsrfDto csrfData, String... cookies) {
		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
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
	private ResponseEntity<CsrfDto> getLoginCsrfToken(HttpHeaders header) {
		ResponseEntity<CsrfDto> newCsrfEntity = webclient.exchange(
				"http://localhost:" + serverPort + "/rest/login/csrf", GET, new HttpEntity<>(header), CsrfDto.class);
		log.trace("Session and CSFR after login : {} {}", header, newCsrfEntity.getBody());
		return newCsrfEntity;
	}

	/**
	 * Get a valid cookie session from a login process.
	 * 
	 * @return A cookie.
	 */
	private HttpHeaders getLoginSessionHeader() {
		ResponseEntity<CsrfDto> csrfEntity = getCsfrEntity();
		String firstSessionId = getFirstNewCookieContent(csrfEntity);
		String loginUri = getLoginUri(csrfEntity.getBody(), true);
		HttpHeaders header = getHeader(null, firstSessionId);
		ResponseEntity<String> postResponse = webclient.postForEntity(loginUri, new HttpEntity<>(header), String.class);
		log.trace("Session and CSFR before login : {} {}", firstSessionId, csrfEntity.getBody());
		CsrfDto csrfDto = csrfEntity.getBody();
		header = getSessionHeader(csrfDto, postResponse);
		return header;
	}

	private String getLoginUri(CsrfDto csrfData, boolean loginParam) {
		UriBuilder builder = new DefaultUriBuilderFactory(
				"http://localhost:" + serverPort + PATH_REST_USER_AUTHENTICATE).builder();
		if (loginParam) {
			builder.queryParam(UserProperties.LOGIN_NAME, "admin").queryParam(UserProperties.PASSWORD, "admin");
		}
		if (csrfData != null) {
			builder.queryParam(csrfData.getParameterName(), csrfData.getToken());
		}
		return builder.build().toString();
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

	private HttpHeaders getSessionHeader(CsrfDto csrfData, ResponseEntity<?> loginResponse) {
		return getSessionHeader(csrfData, loginResponse, true, true, true);
	}

	private HttpHeaders getSessionHeader(CsrfDto csrfData, ResponseEntity<?> loginResponse, boolean addSessionCookie,
			boolean addCsrfCookie, boolean updateCsfrToken) {
		List<String> newCookies = loginResponse.getHeaders().get(SET_COOKIE);
		String sessionCookie = null, csrfCookie = null;
		for (String newCookie : newCookies) {
			String[] cookieSplit = newCookie.split(";");
			String[] cookieBody = cookieSplit[0].split("=");
			if (cookieSplit[0].startsWith("JS")) {
				sessionCookie = newCookie;
			} else if (cookieBody.length == 2 && cookieBody[1] != null && !cookieBody[1].isBlank()) {
				if (updateCsfrToken) {
					csrfData.setToken(cookieBody[1]);
				}
				csrfCookie = newCookie;
			} else {
				log.info("skip cookie remove: {}", newCookie);
			}
		}
		return getHeader(csrfData, addSessionCookie ? sessionCookie : null, addCsrfCookie ? csrfCookie : null);
	}
}
