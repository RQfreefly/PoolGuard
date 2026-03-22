package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class S106_BoundedQueueAndRejectedPolicy {
    public void handle() {
        ExecutorService executor = new ThreadPoolExecutor(
                4,
                8,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new NamedThreadFactory("bounded"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        try {
            executor.submit(() -> System.out.println("bounded"));
        } finally {
            executor.shutdown();
        }
    }

    static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger id = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-" + id.getAndIncrement());
        }
    }
}
