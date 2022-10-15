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

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.frontend.FrontendUser;
import de.futuresqr.server.restdata.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This controller provides information about the current authenticated user.
 * 
 * @author Robert Breunung
 */
@RestController
public class UserInfoController {

	@Autowired
	private UserRepository userRepository;

	@GetMapping("/rest/user/info")
	ResponseEntity<FrontendUser> getUserInfo(HttpServletRequest request) {
		String remoteUser = request.getRemoteUser();
		if (remoteUser != null) {
			Slice<PersistenceUser> findByLoginName = userRepository.findByLoginName(remoteUser);
			if (!findByLoginName.isEmpty()) {
				PersistenceUser user = findByLoginName.iterator().next();
				return ResponseEntity.ok().contentType(APPLICATION_JSON).body(FrontendUser.fromPersistenceUser(user));
			}

		}

		return ResponseEntity.notFound().build();
	}
}
