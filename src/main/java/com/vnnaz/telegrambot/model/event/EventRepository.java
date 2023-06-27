package com.vnnaz.telegrambot.model.event;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Date;
import java.util.List;

@Repository
public interface EventRepository extends CrudRepository<Event, Long> {
    @Query("from Event e where e.date=:eventDate")
    public List<Event> findByDate(@Param("eventDate") Date date);
    @Query(nativeQuery = true, value = "select * from event where event_id IN (select event_event_id from user_event where user_chat_id =:chatID)")
    public List<Event> findByChatId(@Param("chatID") Long chatId);
}
