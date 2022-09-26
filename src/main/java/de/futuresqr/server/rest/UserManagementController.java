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

import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

import java.time.Instant;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.frontend.FrontendUser;
import de.futuresqr.server.restdata.UserRepository;
import de.futuresqr.server.service.FsqrUserDetailsManager;

/**
 * This class implements basic use cases .
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

	@GetMapping({ "/", "/simplelist" })
	ResponseEntity<Page<FrontendUser>> getAllUsers() {
		Page<PersistenceUser> page = userRepo.findAll(PageRequest.of(0, 20));
		Page<FrontendUser> returnPage = page.map(FrontendUser::fromPersistenceUser);
		return ResponseEntity.ok(returnPage);
	}

	@PostMapping("/{command}")
	ResponseEntity<FrontendUser> postAddNewUser(@PathVariable(name = "command") String command,
			@RequestParam("username") String username,
			@RequestParam(name = "password", required = false) String password,
			@RequestParam(name = "contactemail", required = false) String email,
			@RequestParam(name = "displayname", required = false) String displayName) {

		switch (command) {
		case "add": {
			Assert.hasLength(password, "Password requires content.");

			HashSet<String> roles = new HashSet<>();
			roles.add("ROLE_" + FsqrUserDetailsManager.ROLE_USER);

			PersistenceUser persistenceUser = PersistenceUser.builder().loginName(username)
					.password(encoder.encode(password)).grantedAuthorities(roles).email(email).displayName(displayName)
					.build();
			persistenceUser = userRepo.save(persistenceUser);

			return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
		}
		case "ban": {
			Slice<PersistenceUser> slice = userRepo.findByLoginName(username);
			if (slice.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			PersistenceUser persistenceUser = setBanned(true, slice);
			return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
		}
		case "unban": {
			Slice<PersistenceUser> slice = userRepo.findByLoginName(username);
			if (slice.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			PersistenceUser persistenceUser = setBanned(false, slice);
			return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
		}
		case "updatecontact":
		case "updateprofile":
		case "updatedisplayname": {
			Slice<PersistenceUser> slice = userRepo.findByLoginName(username);
			if (slice.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			// FIXME this may override existing parameters without intention
			PersistenceUser persistenceUser = slice.iterator().next();
			persistenceUser.setDisplayName(displayName);
			persistenceUser.setEmail(email);
			persistenceUser = userRepo.save(persistenceUser);
			return ResponseEntity.ok(FrontendUser.fromPersistenceUser(persistenceUser));
		}
		default:
			return ResponseEntity.status(NOT_IMPLEMENTED).build();
		}
	}

	private PersistenceUser setBanned(boolean newBanned, Slice<PersistenceUser> slice) {
		PersistenceUser persistenceUser = slice.iterator().next();
		persistenceUser.setBanned(newBanned);
		Instant bannedDate = Instant.now();
		persistenceUser.setBannedDate(bannedDate);
		persistenceUser.setLastChangeDate(bannedDate);
		persistenceUser = userRepo.save(persistenceUser);
		return persistenceUser;
	}

}
