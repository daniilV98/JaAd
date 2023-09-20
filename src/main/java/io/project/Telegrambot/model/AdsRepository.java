package io.project.Telegrambot.model;

import org.springframework.data.repository.CrudRepository;

public interface AdsRepository extends CrudRepository<Ads, Long> { // подключенная база к таблице уведомлений
}