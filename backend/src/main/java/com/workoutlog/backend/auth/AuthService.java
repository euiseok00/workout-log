package com.workoutlog.backend.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.auth.dto.AuthResponse;
import com.workoutlog.backend.auth.dto.AuthUserResponse;
import com.workoutlog.backend.auth.dto.LoginIdAvailabilityResponse;
import com.workoutlog.backend.auth.dto.LoginRequest;
import com.workoutlog.backend.auth.dto.SignupRequest;
import com.workoutlog.backend.common.error.DuplicateLoginIdException;
import com.workoutlog.backend.common.error.InvalidCredentialsException;
import com.workoutlog.backend.common.error.RequestValidationException;
import com.workoutlog.backend.security.JwtProperties;
import com.workoutlog.backend.user.User;
import com.workoutlog.backend.user.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			JwtProperties jwtProperties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		validatePasswordBytes(request.password());
		if (userRepository.existsByLoginId(request.loginId())) {
			throw new DuplicateLoginIdException();
		}

		User user;
		try {
			user = userRepository.saveAndFlush(new User(request.loginId(), passwordEncoder.encode(request.password())));
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateLoginIdException();
		}
		return createAuthResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		validatePasswordBytes(request.password());
		User user = userRepository.findByLoginId(request.loginId()).orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return createAuthResponse(user);
	}

	@Transactional(readOnly = true)
	public LoginIdAvailabilityResponse checkLoginIdAvailability(String loginId) {
		return new LoginIdAvailabilityResponse(loginId, !userRepository.existsByLoginId(loginId));
	}

	private AuthResponse createAuthResponse(User user) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.subject(user.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(jwtProperties.accessTokenTtl()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AuthResponse(token, "Bearer", jwtProperties.accessTokenTtl().toSeconds(), AuthUserResponse.from(user));
	}

	private void validatePasswordBytes(String password) {
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new RequestValidationException("password", "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
		}
	}
}
