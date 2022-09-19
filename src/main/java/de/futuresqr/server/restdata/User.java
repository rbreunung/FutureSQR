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
package de.futuresqr.server.restdata;

import static lombok.AccessLevel.PRIVATE;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model for the user of the FutureSQR application.
 * 
 * @author Robert Breunung
 */
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor
@Builder
@Data
@Entity
//table name 'user' is reserved in databases
@Table(name = "fsqrUser", uniqueConstraints = { @UniqueConstraint(columnNames = "loginName"),
		@UniqueConstraint(columnNames = "email") })
public class User {

	private boolean banned;
	private Instant bannedDate;
	@Builder.Default
	private Instant createdDate = Instant.now();
	private String displayName;
	private String email;
	@Builder.Default
	private Instant lastChangeDate = Instant.now();
	@Column(name = "loginName")
	private String loginName;
	private String password;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID uuid;
}
