package com.concertticketing.admin.api.repository;

import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.repository.ConcertRefRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaConcertRefRepositoryAdapter implements ConcertRefRepository {

    private final SpringDataConcertRefRepository delegate;

    public JpaConcertRefRepositoryAdapter(SpringDataConcertRefRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ConcertRef save(ConcertRef concertRef) {
        return delegate.save(concertRef);
    }

    @Override
    public Optional<ConcertRef> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public long count() {
        return delegate.count();
    }
}
