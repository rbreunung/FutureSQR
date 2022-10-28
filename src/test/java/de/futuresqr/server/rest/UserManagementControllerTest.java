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

import static de.futuresqr.server.model.frontend.UserProperties.EMAIL;
import static de.futuresqr.server.model.frontend.UserProperties.LOGIN_NAME;
import static de.futuresqr.server.model.frontend.UserProperties.PASSWORD;
import static de.futuresqr.server.service.FsqrUserDetailsManager.ROLE_ADMIN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Slice;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.frontend.UserProperties;
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

	private static final String LOGIN_NAME_ADD = "alter";

	private static final String PATH_REST_USER_ADD = "/rest/user/add";
	private static final String PATH_REST_USER_BAN = "/rest/user/ban";
	private static final String PATH_REST_USER_UNBAN = "/rest/user/unban";

	private static final UUID USER_KNOWN_ID = UUID.randomUUID();
	private static final UUID USER_UNKNOWN_ID = UUID.randomUUID();

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
		knownUser.setUuid(USER_KNOWN_ID);
		Iterator<PersistenceUser> iterator = Arrays.asList(knownUser).iterator();
		Slice<PersistenceUser> slice = mock(Slice.class);
		when(slice.iterator()).thenReturn(iterator);

		Slice<PersistenceUser> emptySlice = mock(Slice.class);
		when(emptySlice.isEmpty()).thenReturn(true);

		when(userRepository.getReferenceById(eq(USER_KNOWN_ID))).thenReturn(knownUser);
		when(userRepository.findByLoginName(ArgumentMatchers.matches("unknown"))).thenReturn(emptySlice);
		when(userRepository.findByLoginName(ArgumentMatchers.matches(LOGIN_NAME_ADD))).thenReturn(emptySlice);
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestAddUser_validCall_returnStatusOk() throws Exception {

		URI uri = URI.create(PATH_REST_USER_ADD);
		mvc.perform(multipart(POST, uri)
				.part(createPart(LOGIN_NAME, LOGIN_NAME_ADD), createPart(PASSWORD, "newPassword"),
						createPart(EMAIL, "vailid@mail.tld"), createPart(UserProperties.DISPLAY_NAME, "alter ego"))
				.with(csrf()))
				// assert
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestAddUser_validCall_userSaved() throws Exception {

		URI uri = URI.create(PATH_REST_USER_ADD);
		mvc.perform(multipart(POST, uri)
				.part(createPart(LOGIN_NAME, LOGIN_NAME_ADD), createPart(PASSWORD, "newPassword"),
						createPart(EMAIL, "vailid@mail.tld"), createPart(UserProperties.DISPLAY_NAME, "alter ego"))
				.with(csrf()));

		verify(userRepository).save(notNull());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestBanUser_knownUser_returnStatusOk() throws Exception {

		URI uri = URI.create(PATH_REST_USER_BAN);
		mvc.perform(multipart(POST, uri).part(createUuidPart(USER_KNOWN_ID)).with(csrf()))
				// assert
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestBanUser_knownUser_userBanned() throws Exception {

		URI uri = URI.create(PATH_REST_USER_BAN);
		mvc.perform(multipart(POST, uri).part(createUuidPart(USER_KNOWN_ID)).with(csrf()));
		// assert
		assertTrue(knownUser.isBanned());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestBanUser_unknownUser_returnStatusNotFound() throws Exception {

		URI uri = URI.create(PATH_REST_USER_BAN);
		mvc.perform(multipart(POST, uri).part(createUuidPart(USER_UNKNOWN_ID)).with(csrf()))
				// assert
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestUnbanUser_knownUser_returnStatusOk() throws Exception {

		URI uri = URI.create(PATH_REST_USER_UNBAN);
		mvc.perform(multipart(POST, uri).part(createUuidPart(USER_KNOWN_ID)).with(csrf()))
				// assert
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestUnbanUser_knownUser_userBanned() throws Exception {

		URI uri = URI.create(PATH_REST_USER_UNBAN);
		mvc.perform(multipart(POST, uri).part(createUuidPart(USER_KNOWN_ID)).with(csrf()));
		// assert
		assertFalse(knownUser.isBanned());
	}

	@Test
	@WithMockUser(username = "admin", password = "admin", roles = ROLE_ADMIN)
	public void testRestUnbanUser_unknownUser_returnStatusNotFound() throws Exception {

		URI uri = URI.create(PATH_REST_USER_UNBAN);
		mvc.perform(multipart(POST, uri).part(createUuidPart(USER_UNKNOWN_ID)).with(csrf()))
				// assert
				.andExpect(status().isNotFound());
	}

	private MockPart createPart(String name, String content) {
		return new MockPart(name, content.getBytes());
	}

	private MockPart createUuidPart(UUID uuid) {
		return createPart(UserProperties.UUID, uuid.toString());
	}
}
