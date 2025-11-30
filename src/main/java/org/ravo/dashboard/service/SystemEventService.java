package org.ravo.dashboard.service;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ravo.dashboard.dto.SystemEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SystemEventService {

    private static final int MAX_EVENTS = 10;

    private final List<SystemEventDto> recentSystemEvents = Collections.synchronizedList(new LinkedList<>());

    public List<SystemEventDto> getRecentSystemEvents() {
        return recentSystemEvents;
    }

    @KafkaListener(topics = "ravo.system.events", groupId = "dashboard-service")
    public void consumeSystemEvent(SystemEventDto event) {

        log.info("Received system event: {}", event);
        synchronized (recentSystemEvents) {

            // 리스트가 10개 이상이면 가장 오래된 이벤트 삭제
            if (recentSystemEvents.size() >= MAX_EVENTS) {
                recentSystemEvents.remove(0); // FIFO
            }

            recentSystemEvents.add(event);
        }
    }
}
