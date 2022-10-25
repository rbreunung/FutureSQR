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

import static de.futuresqr.server.model.frontend.UserProperties.DISPLAY_NAME;
import static de.futuresqr.server.model.frontend.UserProperties.EMAIL;
import static de.futuresqr.server.model.frontend.UserProperties.LOGIN_NAME;
import static de.futuresqr.server.model.frontend.UserProperties.PASSWORD;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.backend.PersistenceUser.PersistenceUserBuilder;
import de.futuresqr.server.model.frontend.FrontendUser;
import de.futuresqr.server.model.frontend.SimpleUserDto;
import de.futuresqr.server.model.frontend.UserProperties;
import de.futuresqr.server.restdata.UserRepository;
import de.futuresqr.server.service.FsqrUserDetailsManager;
import jakarta.annotation.security.RolesAllowed;

/**
 * This class implements basic use cases.
 * 
 * @author Robert Breunung
 */
@RestController
@RequestMapping("/rest/user")
public class UserManagementController {

	@Autowired
	private PasswordEncoder encoder;
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserRepository userRepo;

	@GetMapping("/adminUserList")
	List<FrontendUser> getAdminUserList() {
		List<PersistenceUser> page = userRepo.findAll();
		return page.stream().map(FrontendUser::fromPersistenceUser).toList();
	}

	@GetMapping({ "/simpleList" })
	List<SimpleUserDto> getSimpleUserList() {
		List<PersistenceUser> page = userRepo.findAll();
		return page.stream().map(SimpleUserDto::fromPersistenceUser).toList();
	}

	@RolesAllowed(FsqrUserDetailsManager.ROLE_ADMIN)
	@PostMapping("/add")
	ResponseEntity<FrontendUser> postAddUser(@RequestPart(LOGIN_NAME) String loginName,
			@RequestPart(PASSWORD) String password, @RequestPart(EMAIL) String email,
			@RequestPart(DISPLAY_NAME) String displayName) {
		Assert.hasLength(password, "Password requires content.");
		Assert.isTrue(userRepo.findByLoginName(loginName).isEmpty(), "User with same login exists.");

		HashSet<String> roles = new HashSet<>();
		roles.add(FsqrUserDetailsManager.PREFIX_ROLE + FsqrUserDetailsManager.ROLE_USER);

		PersistenceUserBuilder userBuilder = PersistenceUser.builder().loginName(loginName)
				.password(encoder.encode(password)).grantedAuthorities(roles).email(email).displayName(displayName);
		PersistenceUser persistenceUser = userBuilder.build();
		persistenceUser = userRepo.save(persistenceUser);

		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
	}

	@RolesAllowed(FsqrUserDetailsManager.ROLE_ADMIN)
	@PostMapping("/edit")
	ResponseEntity<FrontendUser> postEditUser(@RequestPart(UserProperties.UUID) String uuid,
			@RequestPart(DISPLAY_NAME) Optional<String> displayName) {

		PersistenceUser persistenceUser = userRepo.getReferenceById(UUID.fromString(uuid));

		displayName.ifPresent(persistenceUser::setDisplayName);
		persistenceUser.setLastChangeDate(Instant.now());

		persistenceUser = userRepo.save(persistenceUser);

		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
	}

	@RolesAllowed(FsqrUserDetailsManager.ROLE_ADMIN)
	@PostMapping({ "/ban" })
	ResponseEntity<FrontendUser> postBanUser(@RequestPart(UserProperties.UUID) String uuid) {

		PersistenceUser persistenceUser = userRepo.getReferenceById(UUID.fromString(uuid));
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		persistenceUser = setBanned(true, persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
	}

	@RolesAllowed(FsqrUserDetailsManager.ROLE_ADMIN)
	@PostMapping({ "/unban" })
	ResponseEntity<FrontendUser> postUnbanUser(@RequestPart(UserProperties.UUID) String uuid) {

		PersistenceUser persistenceUser = userRepo.getReferenceById(UUID.fromString(uuid));
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		persistenceUser = setBanned(false, persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
	}

	@PostMapping({ "/updateEmail" })
	ResponseEntity<FrontendUser> postUpdateContact(@RequestParam(UserProperties.UUID) String uuid,
			@RequestParam(EMAIL) String email) {

		PersistenceUser persistenceUser = userRepo.getReferenceById(UUID.fromString(uuid));
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		persistenceUser.setEmail(email);
		PersistenceUser updatedPersistenceUser = userRepo.save(persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(updatedPersistenceUser));
	}

	@PostMapping({ "/updateDisplayName" })
	ResponseEntity<FrontendUser> postUpdateDisplayName(@RequestParam(UserProperties.UUID) String uuid,
			@RequestParam(DISPLAY_NAME) String displayName) {

		final PersistenceUser persistenceUser = userRepo.getReferenceById(UUID.fromString(uuid));
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		persistenceUser.setDisplayName(displayName);
		PersistenceUser updatedPersistenceUser = userRepo.save(persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(updatedPersistenceUser));
	}

	/**
	 * Update ban state and store to persistence.
	 */
	private PersistenceUser setBanned(boolean newBanned, PersistenceUser user) {
		user.setBanned(newBanned);
		Instant bannedDate = Instant.now();
		user.setBannedDate(bannedDate);
		user.setLastChangeDate(bannedDate);
		return userRepo.save(user);
	}

}
