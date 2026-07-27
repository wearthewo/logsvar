package com.monitoring.security.controller;

import com.monitoring.security.entity.SecurityIncident;
import com.monitoring.security.repository.SecurityIncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityControllerTest {
    @Test
    void forwardsResolvedFilterToDatabaseQuery() {
        SecurityIncidentRepository repository = mock(SecurityIncidentRepository.class);
        when(repository.findByFilters(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var result = new SecurityController(repository).getIncidents(null, true, 0, 20);
        assertThat(result.content()).isEmpty();
        verify(repository).findByFilters(isNull(), eq(true), any(Pageable.class));
    }
}
