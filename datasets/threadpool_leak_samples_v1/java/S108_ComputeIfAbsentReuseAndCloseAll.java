package sample.dataset;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S108_ComputeIfAbsentReuseAndCloseAll {
    private final Map<String, ExecutorService> pools = new ConcurrentHashMap<>();

    public void submit(String key, Runnable task) {
        ExecutorService executor = pools.computeIfAbsent(
                key,
                k -> Executors.newFixedThreadPool(2, new NamedThreadFactory("cache-" + k))
        );
        executor.submit(task);
    }

    public void closeAll() {
        for (ExecutorService executor : pools.values()) {
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
