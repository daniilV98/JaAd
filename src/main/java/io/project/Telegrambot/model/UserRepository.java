package io.project.Telegrambot.model;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> { // подключенная база данных к таблице данных по юзеру
}