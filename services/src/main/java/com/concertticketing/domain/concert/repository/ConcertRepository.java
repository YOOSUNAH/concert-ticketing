package com.concertticketing.domain.concert.repository;

import com.concertticketing.domain.concert.entity.Concert;

import java.util.List;
import java.util.Optional;

public interface ConcertRepository {

    Concert save(Concert concert);

    List<Concert> findAll(int page, int size);

    Optional<Concert> findById(Long concertId);

    long count();
}
