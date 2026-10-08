package com.workoutlog.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

	boolean existsByLoginId(String loginId);

	Optional<User> findByLoginId(String loginId);
}
