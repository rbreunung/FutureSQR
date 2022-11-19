package de.futuresqr.server.rest.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

	@GetMapping({ "/login", "/rest/login" })
	String getLogin() {
		return "login";
	}
}
