package com.nearnow.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @EnableAsync turns on Spring's @Async support project-wide.
 *
 * A dedicated, BOUNDED thread pool is defined here on purpose — Spring's
 * default @Async executor (SimpleAsyncTaskExecutor) creates a brand new
 * thread for every single call, with no upper limit. Under real load,
 * that's an easy way to exhaust server memory. A small, named,
 * fixed-size pool is the correct default for a background task like
 * "send a notification" — this is NOT over-engineering (Kafka/RabbitMQ
 * would be), it's Spring's own recommended baseline for @Async.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notif-");
        executor.initialize();
        return executor;
    }
}
