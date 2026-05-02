package com.concertticketing.domain.user.repository;

import com.concertticketing.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
