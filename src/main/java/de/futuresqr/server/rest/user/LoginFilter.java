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

import java.io.IOException;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import de.futuresqr.server.model.frontend.UserProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter overrides the default
 * {@link UsernamePasswordAuthenticationFilter} in order to read user and
 * password from multi part form.
 *
 * @author Robert Breunung
 */
@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

	@Override
	protected String obtainPassword(HttpServletRequest request) {
		try {
			byte[] bytes = request.getPart(UserProperties.PASSWORD).getInputStream().readAllBytes();
			return new String(bytes);
		} catch (IOException | ServletException e) {
			log.error("Cannot read password from submit.", e);
		}
		return null;
	}

	@Override
	protected String obtainUsername(HttpServletRequest request) {
		try {
			byte[] bytes = request.getPart(UserProperties.LOGIN_NAME).getInputStream().readAllBytes();
			return new String(bytes);
		} catch (IOException | ServletException e) {
			log.error("Cannot read login name from submit.", e);
		}
		return null;
	}

}
