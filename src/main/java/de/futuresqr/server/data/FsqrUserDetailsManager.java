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
package de.futuresqr.server.data;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.GroupManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import de.futuresqr.server.restdata.User;
import de.futuresqr.server.restdata.UserRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Productive user details manager for FSQR internal authentication.
 * 
 * @author Robert Breunung
 */
@Service
@Slf4j
public class FsqrUserDetailsManager implements UserDetailsManager, GroupManager {

	public static final String ROLE_ADMIN = "ADMIN";
	private static final String ROLE_USER = "USER";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private void setPasswordEncoder(PasswordEncoder encoder) {
		if (userRepository.count() == 0) {
			log.info("Empty user repository. Set default users.");
			UserDetails user = org.springframework.security.core.userdetails.User.builder().username("user")
					.password(encoder.encode("password")).roles(ROLE_USER).build();
			createUser(user);
			user = org.springframework.security.core.userdetails.User.builder().username("admin")
					.password(encoder.encode("admin")).roles(ROLE_ADMIN, ROLE_USER).build();
			createUser(user);
		}
	}

	@Override
	public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
		Slice<User> userSlice = userRepository.findByLoginName(username);
		if (userSlice.isEmpty()) {
			throw new UsernameNotFoundException(String.format("\"%s\" not found.", username));
		}

		User dbUser = userSlice.iterator().next();
		UserBuilder builder = org.springframework.security.core.userdetails.User.builder();
		builder.username(dbUser.getLoginName()).password(dbUser.getPassword())
				.authorities(dbUser.getGrantedAuthorities().toArray(i -> new String[i]));
		// TODO implement expired properties
		return builder.build();
	}

	@Override
	public List<String> findAllGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findUsersInGroup(String groupName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createGroup(String groupName, List<GrantedAuthority> authorities) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteGroup(String groupName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renameGroup(String oldName, String newName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addUserToGroup(String username, String group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUserFromGroup(String username, String groupName) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<GrantedAuthority> findGroupAuthorities(String groupName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addGroupAuthority(String groupName, GrantedAuthority authority) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeGroupAuthority(String groupName, GrantedAuthority authority) {
		// TODO Auto-generated method stub

	}

	@Override
	public void createUser(UserDetails user) {
		User dbUser = User.builder().loginName(user.getUsername()).password(user.getPassword())
				.grantedAuthorities(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
				.build();
		userRepository.save(dbUser);
	}

	@Override
	public void updateUser(UserDetails user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteUser(String username) {
		// TODO Auto-generated method stub

	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean userExists(String username) {
		// TODO Auto-generated method stub
		return false;
	}

}
