package com.theology.tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignments")
@PrimaryKeyJoinColumn(name = "work_item_id")
@DiscriminatorValue("ASSIGNMENT")
@Getter
@Setter
@NoArgsConstructor
public class Assignment extends WorkItem {

    private String description;
}
