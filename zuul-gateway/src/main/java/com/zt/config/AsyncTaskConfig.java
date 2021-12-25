package com.zt.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AsyncTaskConfig {

    @Bean
    public ExecutorService routingTaskExecutor() {
        final ThreadPoolExecutor executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
                Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        return TtlExecutors.getTtlExecutorService(executorService);
    }

}
