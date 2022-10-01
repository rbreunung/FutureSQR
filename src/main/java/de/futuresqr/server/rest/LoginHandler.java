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
package de.futuresqr.server.rest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.frontend.FrontendUser;
import de.futuresqr.server.restdata.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This handler customizes the answer to the successful authenticated user.
 * 
 * @author Robert Breunung
 */
public class LoginHandler implements AuthenticationSuccessHandler {

	private final UserRepository userRepository;

	public LoginHandler(UserRepository userRepository) {
		Assert.notNull(userRepository, "User repository required.");
		this.userRepository = userRepository;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		response.setStatus(OK.value());
		response.setContentType(APPLICATION_JSON_VALUE);
		PersistenceUser persistenceUser = userRepository.findByLoginName(authentication.getName()).iterator().next();
		new ObjectMapper().writeValue(response.getOutputStream(), FrontendUser.fromPersistenceUser(persistenceUser));
	}

}
