package com.ludvig.pvpkit.report;

import java.util.UUID;

public record Report(
        int id,
        UUID reporterId,
        String reporterName,
        UUID targetId,
        String targetName,
        String reason,
        long createdAt,
        ReportLocation location,
        ReportStatus status,
        String handledBy
) {
    public Report closed(String staffName) {
        return new Report(id, reporterId, reporterName, targetId, targetName, reason, createdAt,
                location, ReportStatus.CLOSED, staffName);
    }
}
