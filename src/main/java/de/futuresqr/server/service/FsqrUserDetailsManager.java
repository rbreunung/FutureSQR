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
package de.futuresqr.server.service;

import static de.futuresqr.server.model.backend.PersistenceUser.toUserDetails;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.restdata.UserRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Productive user details manager for FSQR internal authentication.
 * 
 * @author Robert Breunung
 */
@Service
@Slf4j
public class FsqrUserDetailsManager implements UserDetailsManager {

	public static final String PREFIX_ROLE = "ROLE_";
	public static final String ROLE_ADMIN = "ADMIN";
	public static final String ROLE_USER = "USER";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private void setPasswordEncoder(PasswordEncoder encoder) {
		if (userRepository.count() == 0) {
			log.info("Empty user repository. Set default users.");
			PersistenceUser user = PersistenceUser.builder().loginName("user").password(encoder.encode("password"))
					.grantedAuthorities(Arrays.stream(new String[] { PREFIX_ROLE + ROLE_USER }).collect(toSet()))
					.displayName("Otto Normal").avatarId(UUID.randomUUID()).email("user@mindscan.local").build();
			userRepository.save(user);
			user = PersistenceUser.builder().loginName("admin").password(encoder.encode("admin")).grantedAuthorities(
					Arrays.stream(new String[] { PREFIX_ROLE + ROLE_USER, PREFIX_ROLE + ROLE_ADMIN }).collect(toSet()))
					.displayName("Super Power").avatarId(UUID.randomUUID()).email("admin@mindscan.local").build();
			userRepository.save(user);
		}
	}

	@Override
	public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
		Slice<PersistenceUser> userSlice = userRepository.findByLoginName(username);
		if (userSlice.isEmpty()) {
			throw new UsernameNotFoundException(String.format("\"%s\" not found.", username));
		}

		PersistenceUser dbUser = userSlice.iterator().next();

		return toUserDetails(dbUser);
	}

	@Override
	public void createUser(UserDetails user) {
		PersistenceUser dbUser = PersistenceUser.fromUserDetails(user);
		userRepository.save(dbUser);
	}

	@Override
	public void updateUser(UserDetails user) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteUser(String username) {
		throw new UnsupportedOperationException("User shall not be deleted by intention for traceability purpose.");
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		Slice<PersistenceUser> slice = userRepository.findByLoginName(username);

		Assert.isTrue(slice.hasContent(), "User not in database: " + username);
		PersistenceUser user = slice.iterator().next();
		Assert.isTrue(user.getPassword().equals(oldPassword), "Old password does not match");

		user.setPassword(newPassword);
		userRepository.save(user);
	}

	@Override
	public boolean userExists(String username) {

		Slice<PersistenceUser> userSlice = userRepository.findByLoginName(username);
		return userSlice.hasContent();
	}

}
