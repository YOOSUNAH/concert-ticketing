package com.concertticketing.domain.concert.entity;

import com.concertticketing.domain.schedule.entity.Schedule;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "concerts")
public class Concert {

    @Id
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
    private List<Schedule> schedules;

    protected Concert() {
    }

    public Concert(Long id, String title, String artist, String description, String venue,
                   String posterUrl, String thumbnailUrl,
                   LocalDate startDate, LocalDate endDate,
                   int maxTicketsPerPerson, ConcertStatus status) {
        this.id = id;
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
        this.schedules = new ArrayList<>();
    }

    public void addSchedule(Schedule schedule) {
        if (this.schedules == null) {
            this.schedules = new ArrayList<>();
        }
        this.schedules.add(schedule);
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

    public List<Schedule> getSchedules() {
        return schedules == null ? List.of() : schedules;
    }
}
