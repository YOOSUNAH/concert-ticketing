package com.concertticketing.domain.user.repository;

import com.concertticketing.domain.user.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository delegate;

    public JpaUserRepositoryAdapter(SpringDataUserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public User save(User user) {
        return delegate.save(user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return delegate.findById(userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return delegate.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return delegate.existsByEmail(email);
    }
}
