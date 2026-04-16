package com.turno.los.dto.response;

import com.turno.los.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response body for GET /api/v1/loans/status-count
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusCountResponse {
    private Map<ApplicationStatus, Long> counts;
    private long total;
}
