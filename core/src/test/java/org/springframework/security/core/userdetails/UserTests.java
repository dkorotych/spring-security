/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.core.userdetails;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests {@link User}.
 *
 * @author Ben Alex
 * @author Ilya Starchenko
 */
public class UserTests {

	private static final List<GrantedAuthority> ROLE_12 = AuthorityUtils.createAuthorityList("ROLE_ONE", "ROLE_TWO");

	public static Stream<Arguments> testNewAuthoritiesShouldReplacePreviousAuthorities() {
		return Stream.of(
				Arguments.of((Object) new String[0]),
				Arguments.of((Object) new String[]{"B7", "C12", "role"}),
				Arguments.of((Object) new String[]{"A1"})
		);
	}

	@Test
	public void equalsReturnsTrueIfUsernamesAreTheSame() {
		User user1 = new User("rod", "koala", true, true, true, true, ROLE_12);
		assertThat(user1).isNotNull();
		assertThat(user1).isNotEqualTo("A STRING");
		assertThat(user1).isEqualTo(user1);
		assertThat(user1).isEqualTo((new User("rod", "notthesame", true, true, true, true, ROLE_12)));
	}

	@Test
	public void hashLookupOnlyDependsOnUsername() {
		User user1 = new User("rod", "koala", true, true, true, true, ROLE_12);
		Set<UserDetails> users = new HashSet<>();
		users.add(user1);
		assertThat(users).contains(new User("rod", "koala", true, true, true, true, ROLE_12));
		assertThat(users).contains(new User("rod", "anotherpass", false, false, false, false,
				AuthorityUtils.createAuthorityList("ROLE_X")));
		assertThat(users).doesNotContain(new User("bod", "koala", true, true, true, true, ROLE_12));
	}

	@Test
	public void testNoArgConstructorDoesntExist() {
		assertThatExceptionOfType(NoSuchMethodException.class)
				.isThrownBy(() -> User.class.getDeclaredConstructor((Class[]) null));
	}

	@Test
	public void testBuildUserWithNoAuthorities() {
		UserDetails user = User.builder().username("user").password("password").build();
		assertThat(user.getAuthorities()).isEmpty();
	}

	@Test
	public void testNullWithinUserAuthoritiesIsRejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> User.builder().username("user").password("password")
				.authorities((Collection<? extends GrantedAuthority>) null).build());
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(null);
		authorities.add(null);
		assertThatIllegalArgumentException().isThrownBy(
				() -> User.builder().username("user").password("password").authorities(authorities).build());

		assertThatIllegalArgumentException().isThrownBy(() -> User.builder().username("user").password("password")
				.authorities((GrantedAuthority[]) null).build());
		assertThatIllegalArgumentException().isThrownBy(() -> User.builder().username("user").password("password")
				.authorities(new GrantedAuthority[] { null, null }).build());

		assertThatIllegalArgumentException().isThrownBy(
				() -> User.builder().username("user").password("password").authorities((String[]) null).build());
		assertThatIllegalArgumentException().isThrownBy(() -> User.builder().username("user").password("password")
				.authorities(new String[] { null, null }).build());
	}

	@ParameterizedTest
	@MethodSource
	public void testNewAuthoritiesShouldReplacePreviousAuthorities(String[] authorities) {
		UserDetails parent = User.builder().username("user").password("password").authorities("A1", "A2", "B1").build();
		User.UserBuilder builder = User.withUserDetails(parent).authorities(authorities);
		UserDetails user = builder.build();
		assertThat(AuthorityUtils.authorityListToSet(user.getAuthorities())).containsOnly(authorities);
		user = builder.authorities(AuthorityUtils.createAuthorityList(authorities)).build();
		assertThat(AuthorityUtils.authorityListToSet(user.getAuthorities())).containsOnly(authorities);
		user = builder.authorities(AuthorityUtils.createAuthorityList(authorities).toArray(GrantedAuthority[]::new)).build();
		assertThat(AuthorityUtils.authorityListToSet(user.getAuthorities())).containsOnly(authorities);
	}

	@Test
	public void testNullValuesRejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> new User(null, "koala", true, true, true, true, ROLE_12));
		assertThatIllegalArgumentException().isThrownBy(() -> new User("rod", null, true, true, true, true, ROLE_12));
		List<GrantedAuthority> auths = AuthorityUtils.createAuthorityList("ROLE_ONE");
		auths.add(null);
		assertThatIllegalArgumentException().isThrownBy(() -> new User("rod", "koala", true, true, true, true, auths));
	}

	@Test
	public void testNullWithinGrantedAuthorityElementIsRejected() {
		List<GrantedAuthority> auths = AuthorityUtils.createAuthorityList("ROLE_ONE");
		auths.add(null);
		auths.add(new SimpleGrantedAuthority("ROLE_THREE"));
		assertThatIllegalArgumentException().isThrownBy(() -> new User(null, "koala", true, true, true, true, auths));
	}

	@Test
	public void testUserGettersSetter() {
		UserDetails user = new User("rod", "koala", true, true, true, true,
				AuthorityUtils.createAuthorityList("ROLE_TWO", "ROLE_ONE"));
		assertThat(user.getUsername()).isEqualTo("rod");
		assertThat(user.getPassword()).isEqualTo("koala");
		assertThat(user.isEnabled()).isTrue();
		assertThat(AuthorityUtils.authorityListToSet(user.getAuthorities())).contains("ROLE_ONE");
		assertThat(AuthorityUtils.authorityListToSet(user.getAuthorities())).contains("ROLE_TWO");
		assertThat(user.toString()).contains("rod");
	}

	@Test
	public void enabledFlagIsFalseForDisabledAccount() {
		UserDetails user = new User("rod", "koala", false, true, true, true, ROLE_12);
		assertThat(user.isEnabled()).isFalse();
	}

	@Test
	public void useIsSerializable() throws Exception {
		UserDetails user = new User("rod", "koala", false, true, true, true, ROLE_12);
		// Serialize to a byte array
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(user);
		out.close();
	}

	@Test
	public void withUserDetailsWhenAllEnabled() {
		User expected = new User("rob", "pass", true, true, true, true, ROLE_12);
		UserDetails actual = User.withUserDetails(expected).build();
		assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
		assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
		assertThat(actual.getAuthorities()).isEqualTo(expected.getAuthorities());
		assertThat(actual.isAccountNonExpired()).isEqualTo(expected.isAccountNonExpired());
		assertThat(actual.isAccountNonLocked()).isEqualTo(expected.isAccountNonLocked());
		assertThat(actual.isCredentialsNonExpired()).isEqualTo(expected.isCredentialsNonExpired());
		assertThat(actual.isEnabled()).isEqualTo(expected.isEnabled());
	}

	@Test
	public void withUserDetailsWhenAllDisabled() {
		User expected = new User("rob", "pass", false, false, false, false, ROLE_12);
		UserDetails actual = User.withUserDetails(expected).build();
		assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
		assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
		assertThat(actual.getAuthorities()).isEqualTo(expected.getAuthorities());
		assertThat(actual.isAccountNonExpired()).isEqualTo(expected.isAccountNonExpired());
		assertThat(actual.isAccountNonLocked()).isEqualTo(expected.isAccountNonLocked());
		assertThat(actual.isCredentialsNonExpired()).isEqualTo(expected.isCredentialsNonExpired());
		assertThat(actual.isEnabled()).isEqualTo(expected.isEnabled());
	}

	@Test
	public void withUserWhenDetailsPasswordEncoderThenEncodes() {
		UserDetails userDetails = User.withUsername("user").password("password").roles("USER").build();
		UserDetails withEncodedPassword = User.withUserDetails(userDetails).passwordEncoder((p) -> p + "encoded")
				.build();
		assertThat(withEncodedPassword.getPassword()).isEqualTo("passwordencoded");
	}

	@Test
	public void withUsernameWhenPasswordEncoderAndPasswordThenEncodes() {
		UserDetails withEncodedPassword = User.withUsername("user").password("password")
				.passwordEncoder((p) -> p + "encoded").roles("USER").build();
		assertThat(withEncodedPassword.getPassword()).isEqualTo("passwordencoded");
	}

	@Test
	public void withUsernameWhenPasswordAndPasswordEncoderThenEncodes() {
		// @formatter:off
		UserDetails withEncodedPassword = User.withUsername("user")
			.passwordEncoder((p) -> p + "encoded")
			.password("password")
			.roles("USER")
			.build();
		// @formatter:on
		assertThat(withEncodedPassword.getPassword()).isEqualTo("passwordencoded");
	}

	@Test
	public void withUsernameWhenPasswordAndPasswordEncoderTwiceThenEncodesOnce() {
		Function<String, String> encoder = (p) -> p + "encoded";
		// @formatter:off
		UserDetails withEncodedPassword = User.withUsername("user")
			.passwordEncoder(encoder)
			.password("password")
			.passwordEncoder(encoder)
			.roles("USER")
			.build();
		// @formatter:on
		assertThat(withEncodedPassword.getPassword()).isEqualTo("passwordencoded");
	}

}
