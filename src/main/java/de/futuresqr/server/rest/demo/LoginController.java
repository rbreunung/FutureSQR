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

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * This implements a custom login controller using default methods of Spring
 * Security.
 * 
 * @author Robert Breunung
 */
@RestController
@Slf4j
public class LoginController {

	@Autowired
	AuthenticationConfiguration authenticationConfiguration;

	@PostMapping(path = "/rest/login")
	ResponseEntity<String> postLogin(@RequestParam(name = "username", required = false) final String username,
			@RequestParam(name = "password", required = false) final String password,
			@RequestParam(name = "_csrf", required = false) final String csrf, final HttpServletRequest request,
			final CsrfToken tokenContainer) throws Exception {

		log.trace("user {} pass {} csrf {}", username, password, csrf);

		if (tokenContainer != null && tokenContainer.getToken() != null) {

			final String foundToken;
			if (csrf != null) {
				foundToken = csrf;
			} else {
				foundToken = request.getHeader(tokenContainer.getHeaderName());
			}
			if (tokenContainer.getToken().equals(foundToken)) {
				log.trace("CSRF match");
			} else {
				return ResponseEntity.status(403).body("Invalid authentication");
			}

		} else {
			log.warn("No CSRF required for {}", username);
		}

		UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(username, password);
		Authentication auth = authenticationConfiguration.getAuthenticationManager().authenticate(authReq);
		SecurityContext sc = SecurityContextHolder.getContext();
		sc.setAuthentication(auth);
		HttpSession session = request.getSession(true);
		session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);

		return ResponseEntity.ok(String.format("user %s pass %s csrf %s", username, password, csrf));
	}
}
