package com.yupi.springbootinit.config;

import org.bouncycastle.pqc.crypto.newhope.NHSecretKeyProcessor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.Wang
 * @create 2023-09-10-17:02
 */

@Configuration
public class ThreadPoolExcutorConfig {


    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        ThreadFactory threadFactory = new ThreadFactory(){
            private int count=1;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程"+count);
                count++;
                return thread;
            }
        };


        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,4,
                100, TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),threadFactory);

        return threadPoolExecutor;
    }
}
