package com.concertticketing.domain.concert.repository;

import com.concertticketing.domain.concert.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConcertRepository extends JpaRepository<Concert, Long> {
}
