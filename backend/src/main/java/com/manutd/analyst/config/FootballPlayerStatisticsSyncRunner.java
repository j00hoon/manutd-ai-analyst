package com.manutd.analyst.config;

import com.manutd.analyst.dto.FootballPlayerStatisticsSyncResult;
import com.manutd.analyst.service.FootballPlayerStatisticsSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "football.player-statistics.sync.enabled", havingValue = "true")
public class FootballPlayerStatisticsSyncRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(FootballPlayerStatisticsSyncRunner.class);
    private final FootballPlayerStatisticsSyncService syncService;
    private final int syncLimit;

    public FootballPlayerStatisticsSyncRunner(
            FootballPlayerStatisticsSyncService syncService,
            @Value("${football.player-statistics.sync.limit}") int syncLimit) {
        this.syncService = syncService;
        this.syncLimit = syncLimit;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            FootballPlayerStatisticsSyncResult result =
                    syncService.synchronizeRecentPlayerStatistics(syncLimit);
            LOGGER.info("Player statistics synchronization completed: requested={}, complete={}, partial={}, unavailable={}, failed={}",
                    result.requested(), result.complete(), result.partial(), result.unavailable(), result.failed());
        } catch (RuntimeException exception) {
            LOGGER.error("Player statistics synchronization failed; existing records were preserved", exception);
        }
    }
}
