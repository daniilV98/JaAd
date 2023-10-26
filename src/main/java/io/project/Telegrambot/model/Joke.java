package io.project.Telegrambot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Joke { // класс для рассылки уведомлений

    @Column(length = 2550000)
    private String body;

    private String category;

    @Id
    private Integer id;

    private double rating;
}