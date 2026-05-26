package com.concertticketing.domain.concert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PostgreSQL용 콘서트 참조 엔티티.
 * Main API가 예매 시 필요한 최소 정보만 보유한다.
 * 표시용 필드(posterUrl, artist 등)는 MongoDB의 Concert가 담당.
 */
@Entity
@Table(name = "concerts")
public class ConcertRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private int maxTicketsPerPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConcertStatus status;

    protected ConcertRef() {
    }

    public ConcertRef(String title, String venue, int maxTicketsPerPerson, ConcertStatus status) {
        this.title = title;
        this.venue = venue;
        this.maxTicketsPerPerson = maxTicketsPerPerson;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getVenue() {
        return venue;
    }

    public int getMaxTicketsPerPerson() {
        return maxTicketsPerPerson;
    }

    public ConcertStatus getStatus() {
        return status;
    }

    public void updateInfo(String title, String venue, int maxTicketsPerPerson) {
        this.title = title;
        this.venue = venue;
        this.maxTicketsPerPerson = maxTicketsPerPerson;
    }

    public void changeStatus(ConcertStatus status) {
        this.status = status;
    }
}
