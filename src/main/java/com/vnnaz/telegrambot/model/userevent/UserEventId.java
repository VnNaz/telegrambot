package com.vnnaz.telegrambot.model.userevent;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserEventId implements Serializable {
    @Column(name = "chat_id", nullable = false)
    private Long chatId;
    @Column(name = "event_id", nullable = false)
    private Long eventId;

}
