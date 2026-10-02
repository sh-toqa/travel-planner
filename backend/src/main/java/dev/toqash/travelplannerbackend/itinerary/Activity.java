package dev.toqash.travelplannerbackend.itinerary;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id", nullable = false)
    private ItineraryDay day;

    // Order within the day, starting at 0.
    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CostLevel costLevel;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 200)
    private String placeName;

    private Double latitude;

    private Double longitude;

    // Pinned by the user: kept when the itinerary is regenerated.
    @Column(nullable = false)
    private boolean locked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivitySource source;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}