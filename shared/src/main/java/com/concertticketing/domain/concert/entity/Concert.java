package com.concertticketing.domain.concert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "concerts")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String venue;

    private String posterUrl;
    private String thumbnailUrl;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int maxTicketsPerPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConcertStatus status;

    protected Concert() {
    }

    public Concert(String title, String artist, String description, String venue,
                   String posterUrl, String thumbnailUrl,
                   LocalDate startDate, LocalDate endDate,
                   int maxTicketsPerPerson, ConcertStatus status) {
        this.title = title;
        this.artist = artist;
        this.description = description;
        this.venue = venue;
        this.posterUrl = posterUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxTicketsPerPerson = maxTicketsPerPerson;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDescription() {
        return description;
    }

    public String getVenue() {
        return venue;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getMaxTicketsPerPerson() {
        return maxTicketsPerPerson;
    }

    public ConcertStatus getStatus() {
        return status;
    }
}
