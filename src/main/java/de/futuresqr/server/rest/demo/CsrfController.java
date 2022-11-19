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

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * This controller provides on request the CSRF token belonging to the current
 * session.
 * 
 * @author Robert Breunung
 */
@RestController
@Slf4j
public class CsrfController {

	private HttpSessionCsrfTokenRepository tokenRepo = new HttpSessionCsrfTokenRepository();

	@GetMapping("/rest/user/csrf")
	public CsrfToken csrf(HttpServletRequest request, HttpServletResponse response) {

		CsrfToken repoToken = tokenRepo.loadToken(request);
		if (repoToken != null) {
			log.info("Existing token: {}", repoToken.getToken());
			return repoToken;
		}
		HttpSession session = request.getSession(false);
		if (session == null) {
			session = request.getSession();
		}
		repoToken = tokenRepo.generateToken(request);
		tokenRepo.saveToken(repoToken, request, response);
		log.info("New token: {}", repoToken.getToken());
		return repoToken;
	}
}
