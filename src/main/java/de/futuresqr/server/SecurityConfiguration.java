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
package de.futuresqr.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import de.futuresqr.server.rest.demo.LoginHandler;
import de.futuresqr.server.rest.user.LoginConfigurer;
import de.futuresqr.server.restdata.UserRepository;
import de.futuresqr.server.service.FsqrUserDetailsManager;

/**
 * Main configuration for Spring Security.
 * 
 * @author Robert Breunung
 */
@Configuration
public class SecurityConfiguration {

	private static final String PATH_RESTDATA = "/restdata/**";
	private static final String PATH_REST = "/rest/**";
	public static final String PATH_REST_USER_AUTHENTICATE = "/rest/user/authenticate";

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests() // authorization section
				// rest login area
				.antMatchers("/rest/login/**", "/rest/user/csrf", "/rest/user/info", "/rest/user/reauthenticate").permitAll()
				// demo end point for SayHello.java
				.antMatchers("/rest/say-hello").permitAll()
				// user repository area
				.antMatchers(PATH_REST).authenticated()
				// plain data repository area
				.antMatchers(PATH_RESTDATA).hasRole(FsqrUserDetailsManager.ROLE_ADMIN);
		http.apply(new LoginConfigurer<>()).loginProcessingUrl(PATH_REST_USER_AUTHENTICATE) //
				.successHandler(authenticationSuccessHandler(null));
		http.logout().logoutUrl("/rest/user/logout") //
				.logoutSuccessUrl("/rest/user/info");
		http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
		DefaultSecurityFilterChain build = http.build();
		return build;
	}

	@Bean
	AuthenticationSuccessHandler authenticationSuccessHandler(@NonNull UserRepository userRepository) {
		return new LoginHandler(userRepository);
	}

	@Bean
	PasswordEncoder paswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
