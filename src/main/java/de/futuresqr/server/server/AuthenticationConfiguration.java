package de.futuresqr.server.server;

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
			UserDetails user = User.builder().username("user").password(encoder.encode("password")).roles("USER")
					.build();
			UserDetails admin = User.builder().username("admin").password(encoder.encode("admin")).roles("USER")
					.build();
			jdbcUserDetailsManagerConfigurer.getUserDetailsService().createUser(admin);
			jdbcUserDetailsManagerConfigurer.getUserDetailsService().createUser(user);
		}
	}
}
