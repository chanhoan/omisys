package com.omisys.promotion.server.application.service;

import com.omisys.promotion.server.domain.model.Event;
import com.omisys.promotion.server.domain.repository.EventRepository;
import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import com.omisys.promotion.server.presentation.request.EventRequest;
import com.omisys.promotion.server.presentation.response.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Long createEvent(EventRequest.Create request, Long userId) {
        Event event = Event.create(request);
        eventRepository.save(event);
        return event.getId();
    }

    public Long updateEvent(Long eventId, EventRequest.Update request, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.EVENT_NOT_FOUND));
        event.update(request);
        eventRepository.save(event);
        return event.getId();
    }

    @Transactional(readOnly = true)
    public Page<EventResponse.Get> getEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findAll(pageable);
        return events.map(EventResponse.Get::from);
    }

    @Transactional(readOnly = true)
    public EventResponse.Get getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.EVENT_NOT_FOUND));
        return EventResponse.Get.from(event);
    }

    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.EVENT_NOT_FOUND));
        eventRepository.delete(event);
    }

}