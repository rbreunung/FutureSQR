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

import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.backend.PersistenceUser.PersistenceUserBuilder;
import de.futuresqr.server.model.frontend.FrontendUser;
import de.futuresqr.server.model.frontend.SimpleUserDto;
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
	@Autowired
	private UserRepository userRepo;

	@GetMapping({ "/simplelist" })
	Map<UUID, SimpleUserDto> getSimpleUserList() {
		List<PersistenceUser> page = userRepo.findAll();
		Map<UUID, SimpleUserDto> returnPage = page.stream()
				.collect(toMap(PersistenceUser::getUuid, SimpleUserDto::fromPersistenceUser));
		return returnPage;
	}

	@GetMapping("/adminuserlist")
	List<FrontendUser> getAdminUserList() {
		List<PersistenceUser> page = userRepo.findAll();
		return page.stream().map(FrontendUser::fromPersistenceUser).toList();
	}

	@RolesAllowed(FsqrUserDetailsManager.ROLE_ADMIN)
	@PostMapping("/add")
	ResponseEntity<FrontendUser> postAddNewUser(@RequestParam("username") String username,
			@RequestParam(name = "password") String password, @RequestParam(name = "contactemail") String email,
			@RequestParam(name = "displayname") String displayName) {

		Assert.hasLength(password, "Password requires content.");

		HashSet<String> roles = new HashSet<>();
		roles.add("ROLE_" + FsqrUserDetailsManager.ROLE_USER);

		PersistenceUserBuilder userBuilder = PersistenceUser.builder().loginName(username)
				.password(encoder.encode(password)).grantedAuthorities(roles).email(email).displayName(displayName);
		PersistenceUser persistenceUser = userBuilder.build();
		persistenceUser = userRepo.save(persistenceUser);

		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
	}

	@RolesAllowed(FsqrUserDetailsManager.ROLE_ADMIN)
	@PostMapping({ "/ban", "/unban" })
	ResponseEntity<FrontendUser> postBanUnbanUser(@RequestParam("username") String username,
			RequestEntity<String> request) {

		PersistenceUser persistenceUser = getUser(username);
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		persistenceUser = setBanned(request.getUrl().getPath().contains("/ban"), persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
	}

	@PostMapping({ "/updatecontact" })
	ResponseEntity<FrontendUser> postUpdateContact(@RequestParam("username") String username,
			@RequestParam(name = "contactemail") String email) {

		PersistenceUser persistenceUser = getUser(username);
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		persistenceUser.setEmail(email);
		PersistenceUser updatedPersistenceUser = userRepo.save(persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(updatedPersistenceUser));
	}

	@PostMapping({ "/updatedisplayname", "/updateprofile" })
	ResponseEntity<FrontendUser> postUpdateProfile(@RequestParam("username") String username,
			@RequestParam(name = "displayname", required = false) Optional<String> displayName) {

		final PersistenceUser persistenceUser = getUser(username);
		if (persistenceUser == null) {
			return ResponseEntity.notFound().build();
		}
		displayName.ifPresent(persistenceUser::setDisplayName);
		PersistenceUser updatedPersistenceUser = userRepo.save(persistenceUser);
		return ResponseEntity.ok(FrontendUser.fromPersistenceUser(updatedPersistenceUser));
	}

	private PersistenceUser getUser(String username) {
		Slice<PersistenceUser> slice = userRepo.findByLoginName(username);
		return slice.isEmpty() ? null : slice.iterator().next();
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
