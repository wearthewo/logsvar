package com.monitoring.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.backend.dto.EventDto;
import com.monitoring.backend.dto.EventEnvelope;
import com.monitoring.backend.dto.EventRequest;
import com.monitoring.backend.entity.MonitoringEvent;
import com.monitoring.backend.kafka.EventProducer;
import com.monitoring.backend.repository.MonitoringEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceTest {
    @Test
    void persistsAndPublishesTheSameCanonicalEnvelope() {
        MonitoringEventRepository repository = mock(MonitoringEventRepository.class);
        EventProducer producer = mock(EventProducer.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(producer.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        EventService service = new EventService(repository, producer, new ObjectMapper().findAndRegisterModules());

        String id = service.processEvent(new EventRequest(null, "checkout", EventDto.EventType.HTTP_REQUEST,
                null, Map.of("latencyMs", 2500, "statusCode", 503)));

        assertThat(id).isNotBlank();
        ArgumentCaptor<MonitoringEvent> stored = ArgumentCaptor.forClass(MonitoringEvent.class);
        ArgumentCaptor<EventEnvelope> published = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(repository).save(stored.capture());
        verify(producer).sendEvent(published.capture());
        assertThat(stored.getValue().getId()).isEqualTo(id);
        assertThat(published.getValue().id()).isEqualTo(id);
        assertThat(published.getValue().payload()).containsEntry("latencyMs", 2500);
    }
}
