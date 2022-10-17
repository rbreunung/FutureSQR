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
package de.futuresqr.server.model.frontend;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.futuresqr.server.model.backend.PersistenceUser;
import de.futuresqr.server.model.backend.PersistenceUser.PersistenceUserBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class is represents the contract between backend and frontend how to
 * represent a user in the administration area.
 * 
 * @author Robert Breunung
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Data
@NoArgsConstructor
public class FrontendUser {

	private static Pattern uuidPattern = Pattern
			.compile("[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}");

	private UUID uuid;
	private String loginName;
	private String password;
	private String displayName;
	private String[] vcsNames;
	private String avatarId;
	private String contactEmail;
	private boolean banned;

	public static FrontendUser fromPersistenceUser(PersistenceUser user) {
		return builder().uuid(user.getUuid()).loginName(user.getLoginName()).displayName(user.getDisplayName())
				.avatarId(user.getAvatarId() == null ? null : user.getAvatarId().toString())
				.contactEmail(user.getEmail()).banned(user.isBanned()).build();
	}

	public PersistenceUser toPersistenceUser() {
		PersistenceUserBuilder userBuilder = PersistenceUser.builder().uuid(uuid).loginName(loginName)
				.displayName(displayName).avatarId(null).email(contactEmail).banned(banned);
		if (avatarId != null && !avatarId.isBlank()) {
			Matcher matcher = uuidPattern.matcher(avatarId);
			if (matcher.find()) {
				userBuilder.avatarId(UUID.fromString(matcher.group()));
			}
		}
		return userBuilder.build();
	}
}
