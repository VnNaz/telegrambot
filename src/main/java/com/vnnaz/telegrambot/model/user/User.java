package com.vnnaz.telegrambot.model.user;

import com.vnnaz.telegrambot.model.task.Task;
import com.vnnaz.telegrambot.model.userevent.UserEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"first_name", "last_name"})
        }
)
@Setter
@Getter
@NoArgsConstructor
public class User {

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Task> tasks;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserEvent> userEvents;

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "register_at")
    private Date registerAt;

    @Column(name = "username")
    private String username;

    public User(Long chatId, String firstName, String lastName, Date registerAt, String username) {
        this.chatId = chatId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registerAt = registerAt;
        this.username = username;
        tasks = new ArrayList<>();
    }
    public String getData() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", registerAt=" + registerAt +
                ", username='" + username + '\'' +
                '}';
    }
}
