package io.project.Telegrambot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "usersDataTable")
@Data
public class User { //класс с данными юзера, при взаимодействии с ботом

    @Id
    private Long chatId;

    private String phoneNumber;

    private java.sql.Timestamp registeredAt;

    private String firstName;

    private String lastName;

    private String userName;

    private String bio;

    private String description;
}