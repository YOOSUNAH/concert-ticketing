package com.concertticketing.domain.concert.repository;

import com.concertticketing.domain.concert.entity.ConcertRef;

import java.util.Optional;

public interface ConcertRefRepository {

    ConcertRef save(ConcertRef concertRef);

    Optional<ConcertRef> findById(Long id);

    long count();
}
