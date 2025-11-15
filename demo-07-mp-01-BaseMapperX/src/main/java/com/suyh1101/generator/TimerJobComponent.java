package com.suyh1101.generator;

import com.suyh1101.generator.plusv5.SuyhIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author suyh
 * @since 2025-11-15
 */
@Component
@RequiredArgsConstructor
public class TimerJobComponent {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final SuyhIdGenerator idGenerator = new SuyhIdGenerator(LocalDate.of(2025, 1, 1));

    @PostConstruct
    public void init() {
        // 初始化定时任务调度执行器
        scheduledExecutorService.scheduleWithFixedDelay(idGenerator::resetRelativeMs, 11, 11, TimeUnit.SECONDS);
    }
}
