package com.theology.tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "papers")
@PrimaryKeyJoinColumn(name = "work_item_id")
@DiscriminatorValue("PAPER")
@Getter
@Setter
@NoArgsConstructor
public class Paper extends WorkItem {

    private String promptOrTopic;
    private Integer wordCountTarget;
    private String scoreOrGrade;
}
