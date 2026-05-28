package com.concertticketing.domain.concert.repository;

import com.concertticketing.domain.concert.entity.Concert;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConcertRepository extends MongoRepository<Concert, Long> {

    /** scheduleId가 임베디드된 Concert 1건 조회 */
    Concert findFirstBySchedulesIdEquals(Long scheduleId);

    default List<Concert> findAll(int page, int size) {
        return findAll(PageRequest.of(page, size)).getContent();
    }

    default List<Concert> findAllByIds(List<Long> concertIds) {
        return findAllById(concertIds);
    }
}
