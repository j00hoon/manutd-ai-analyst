package com.manutd.analyst.config;

import com.manutd.analyst.dto.FootballStatisticsSyncResult;
import com.manutd.analyst.service.FootballStatisticsSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "football.statistics.sync.enabled", havingValue = "true")
public class FootballStatisticsSyncRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FootballStatisticsSyncRunner.class);

    private final FootballStatisticsSyncService syncService;
    private final int syncLimit;

    public FootballStatisticsSyncRunner(
            FootballStatisticsSyncService syncService,
            @Value("${football.statistics.sync.limit}") int syncLimit) {
        this.syncService = syncService;
        this.syncLimit = syncLimit;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            FootballStatisticsSyncResult result = syncService.synchronizeRecentStatistics(syncLimit);
            LOGGER.info(
                    "Football statistics synchronization completed: requested={}, complete={}, partial={}, unavailable={}, failed={}",
                    result.requested(), result.complete(), result.partial(), result.unavailable(), result.failed());
        } catch (RuntimeException exception) {
            LOGGER.error("Football statistics synchronization failed; existing records were preserved", exception);
        }
    }
}
