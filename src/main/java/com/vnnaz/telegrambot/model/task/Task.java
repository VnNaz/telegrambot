package com.vnnaz.telegrambot.model.task;

import com.vnnaz.telegrambot.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "task_body")
    private String body;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "chat_id", nullable = false)
    private User user;

    public Task(String body, User user) {
        this.body = body;
        this.user = user;
    }
}

