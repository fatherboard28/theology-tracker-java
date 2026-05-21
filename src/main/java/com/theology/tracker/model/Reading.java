package com.theology.tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "readings")
@PrimaryKeyJoinColumn(name = "work_item_id")
@DiscriminatorValue("READING")
@Getter
@Setter
@NoArgsConstructor
public class Reading extends WorkItem {

    @Column(nullable = false)
    private String source;

    private String author;
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingFormat format = ReadingFormat.PHYSICAL_BOOK;
}
