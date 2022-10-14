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

import static de.futuresqr.server.service.FsqrUserDetailsManager.ROLE_ADMIN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.rest.user.UserManagementController;
import de.futuresqr.server.restdata.UserRepository;

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

	private PersistenceUser knownUser;

	@Autowired
	private MockMvc mvc;

	@MockBean
	private UserRepository userRepository;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup() {
		when(userRepository.save(any())).then(returnsFirstArg());

		knownUser = PersistenceUser.builder().loginName("known").build();
		Iterator<PersistenceUser> iterator = Arrays.asList(knownUser).iterator();
		Slice<PersistenceUser> slice = mock(Slice.class);
		when(slice.iterator()).thenReturn(iterator);

		Slice<PersistenceUser> emptySlice = mock(Slice.class);
		when(emptySlice.isEmpty()).thenReturn(true);

		when(userRepository.findByLoginName(ArgumentMatchers.matches("known"))).thenReturn(slice);
		when(userRepository.findByLoginName(ArgumentMatchers.matches("unknown"))).thenReturn(emptySlice);
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestAddUser_validCall_returnStatusOk() throws Exception {

		mvc.perform(post(URI.create("/rest/user/add")).param("username", "alter").param("password", "newPassword")
				.param("contactemail", "vailid@mail.tld").param("displayname", "alter ego").with(csrf())).andDo(print())
				// assert
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestAddUser_validCall_userSaved() throws Exception {

		mvc.perform(post(URI.create("/rest/user/add")).param("username", "alter").param("password", "newPassword")
				.param("contactemail", "vailid@mail.tld").param("displayname", "alter ego").with(csrf()));

		verify(userRepository).save(notNull());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestBanUser_knownUser_returnStatusOk() throws Exception {

		mvc.perform(post(URI.create("/rest/user/ban")).param("username", "known").with(csrf()))
				// assert
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestBanUser_knownUser_userBanned() throws Exception {

		mvc.perform(post(URI.create("/rest/user/ban")).param("username", "known").with(csrf()));
		// assert
		assertTrue(knownUser.isBanned());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestBanUser_unknownUser_returnStatusNotFound() throws Exception {

		mvc.perform(post(URI.create("/rest/user/ban")).param("username", "unknown").with(csrf()))
				// assert
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestUnbanUser_knownUser_returnStatusOk() throws Exception {

		mvc.perform(post(URI.create("/rest/user/unban")).param("username", "known").with(csrf()))
				// assert
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestUnbanUser_knownUser_userBanned() throws Exception {

		mvc.perform(post(URI.create("/rest/user/unban")).param("username", "known").with(csrf()));
		// assert
		assertFalse(knownUser.isBanned());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestUnbanUser_unknownUser_returnStatusNotFound() throws Exception {

		mvc.perform(post(URI.create("/rest/user/unban")).param("username", "unknown").with(csrf()))
				// assert
				.andExpect(status().isNotFound());
	}
}
