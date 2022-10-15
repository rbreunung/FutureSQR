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
