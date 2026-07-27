package com.monitoring.alerts.controller;

import com.monitoring.alerts.model.AlertHistory;
import com.monitoring.alerts.repository.AlertHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlertHistoryControllerTest {
    @Test
    void returnsAnEmptyPageWithoutSubListFailure() {
        AlertHistoryRepository repository = mock(AlertHistoryRepository.class);
        when(repository.findByFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var response = new AlertHistoryController(repository).getAlertHistory(5, 20, null, null, null, null);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
    }
}
