package com.concertticketing.domain.concert.entity;

import java.time.LocalDate;

public class Concert {

    private Long id;
    private String title;
    private String artist;
    private String description;
    private String venue;
    private String posterUrl;
    private String thumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxTicketsPerPerson;
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
