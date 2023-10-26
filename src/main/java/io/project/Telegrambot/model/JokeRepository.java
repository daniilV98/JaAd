package io.project.Telegrambot.model;

import org.springframework.data.repository.CrudRepository;

public interface JokeRepository extends CrudRepository<Joke, Integer> { // подключенная база к таблице уведомлений
}