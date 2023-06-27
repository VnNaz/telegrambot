package com.vnnaz.telegrambot.model.event;

import com.vnnaz.telegrambot.model.userevent.UserEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import java.util.Date;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Temporal(TemporalType.DATE)
    @Column(name = "date")
    private Date date;
    @Column(name = "total_member", length = 3)
    private Long totalMember = 0L;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<UserEvent> userEvent;

    public Event(String name, Date date) {
        this.name = name;
        this.date = date;
    }


}
