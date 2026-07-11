package com.concertticketing.domain.concert.repository;

import com.concertticketing.domain.concert.entity.ConcertRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConcertRefRepository extends JpaRepository<ConcertRef, Long> {
}
