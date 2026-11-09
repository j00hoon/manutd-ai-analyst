package com.manutd.analyst.config;

import com.manutd.analyst.dto.FootballSyncResult;
import com.manutd.analyst.service.FootballSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@ConditionalOnProperty(name = "football.sync.enabled", havingValue = "true")
public class FootballSyncRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FootballSyncRunner.class);

    private final FootballSyncService footballSyncService;
    private final int syncLimit;

    public FootballSyncRunner(
            FootballSyncService footballSyncService,
            @Value("${football.sync.limit}") int syncLimit) {
        this.footballSyncService = footballSyncService;
        this.syncLimit = syncLimit;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            FootballSyncResult result = footballSyncService.synchronizeRecentMatches(syncLimit);
            LOGGER.info(
                    "Football synchronization completed: received={}, created={}, updated={}, skipped={}",
                    result.received(), result.created(), result.updated(), result.skipped());
        } catch (RuntimeException exception) {
            LOGGER.error("Football synchronization failed; existing database records were preserved", exception);
        }
    }
}
