package com.concertticketing.domain.concert.repository;

import com.concertticketing.domain.concert.entity.Concert;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaConcertRepositoryAdapter implements ConcertRepository {

    private final SpringDataConcertRepository delegate;

    public JpaConcertRepositoryAdapter(SpringDataConcertRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Concert save(Concert concert) {
        return delegate.save(concert);
    }

    @Override
    public List<Concert> findAll(int page, int size) {
        return delegate.findAll(PageRequest.of(page, size, Sort.by("id"))).getContent();
    }

    @Override
    public Optional<Concert> findById(Long concertId) {
        return delegate.findById(concertId);
    }

    @Override
    public List<Concert> findAllByIds(List<Long> concertIds) {
        return delegate.findAllById(concertIds);
    }

    @Override
    public long count() {
        return delegate.count();
    }
}
