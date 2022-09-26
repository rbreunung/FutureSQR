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

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import de.futuresqr.server.data.FsqrUserDetailsManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Unit tests for {@link UserManagementController}.
 * 
 * @author Robert Breunung
 */
@WebMvcTest(UserManagementController.class)
public class UserManagementControllerTest {

	@TestConfiguration
	static class RequiresBeans {

		@Bean
		PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}
	}

	@Autowired
	private MockMvc mvc;

	@MockBean
	private FsqrUserDetailsManager userManager;

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = "ADMIN")
	public void testRestAddUser_validCall_returnStatus() throws Exception {

		mvc.perform(post(URI.create("/rest/user/add")).param("username", "alter").param("password", "newPassword")
				.with(csrf())).andDo(print())
				// assert
				.andExpect(status().is(204));
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = "ADMIN")
	public void testRestAddUser_validCall_userCreated() throws Exception {

		mvc.perform(post(URI.create("/rest/user/add")).param("username", "alter").param("password", "newPassword")
				.with(csrf()));

		verify(userManager).createUser(Mockito.notNull());
	}
}
