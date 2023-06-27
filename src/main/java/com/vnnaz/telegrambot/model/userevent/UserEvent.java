package com.vnnaz.telegrambot.model.userevent;

import com.vnnaz.telegrambot.model.event.Event;
import com.vnnaz.telegrambot.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class UserEvent {
    @EmbeddedId
    private UserEventId eventId = new UserEventId();

    @ManyToOne(cascade = CascadeType.MERGE)
    @MapsId("chatId")
    private User user;

    @ManyToOne(cascade = CascadeType.MERGE)
    @MapsId("eventId")
    private Event event;

    @Column(name = "join_at")
    private Date joinAt;

}
