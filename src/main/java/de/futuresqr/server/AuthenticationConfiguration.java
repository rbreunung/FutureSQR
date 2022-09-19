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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.JdbcUserDetailsManagerConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AuthenticationConfiguration {

	private static final String ROLE_ADMIN = "ADMIN";
	private static final String ROLE_USER = "USER";

	@Autowired
	void authService(DataSource dataSource, AuthenticationManagerBuilder builder, PasswordEncoder encoder)
			throws Exception {

		boolean setInitialUsers = false;
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

		EncodedResource encodedScript = new EncodedResource(
				resourceLoader.getResource(JdbcDaoImpl.DEFAULT_USER_SCHEMA_DDL_LOCATION));
		try {
			ScriptUtils.executeSqlScript(dataSource.getConnection(), encodedScript);
			setInitialUsers = true;
		} catch (Exception e) {
			log.info("User DB existing. Skip creating default user.");
			log.trace("no init", e);
		}

		JdbcUserDetailsManagerConfigurer<AuthenticationManagerBuilder> jdbcUserDetailsManagerConfigurer = builder
				.jdbcAuthentication().dataSource(dataSource);
		if (setInitialUsers) {
			UserDetails user = User.builder().username("user").password(encoder.encode("password")).roles(ROLE_USER)
					.build();
			UserDetails admin = User.builder().username("admin").password(encoder.encode("admin"))
					.roles(ROLE_USER, ROLE_ADMIN).build();
			jdbcUserDetailsManagerConfigurer.getUserDetailsService().createUser(admin);
			jdbcUserDetailsManagerConfigurer.getUserDetailsService().createUser(user);
		}
	}
}
