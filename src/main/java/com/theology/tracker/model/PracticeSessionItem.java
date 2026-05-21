package com.theology.tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "practice_session_items")
@PrimaryKeyJoinColumn(name = "work_item_id")
@DiscriminatorValue("PRACTICE_SESSION")
@Getter
@Setter
@NoArgsConstructor
public class PracticeSessionItem extends WorkItem {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id")
    private Method method;

    private String scripturePassage;
    private Integer durationMinutes;
}
