package sample.dataset;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S115_ComputeIfAbsentReuseAndCloseAll2 {
    private final Map<String, ExecutorService> pools = new ConcurrentHashMap<>();

    public void submit(String key) {
        ExecutorService executor = pools.computeIfAbsent(
                key,
                k -> Executors.newFixedThreadPool(2, new NamedThreadFactory("cache2-" + k))
        );
        executor.submit(() -> System.out.println(k(key)));
    }

    public void closeAll() {
        for (ExecutorService executor : pools.values()) {
            executor.shutdown();
        }
    }

    private String k(String key) {
        return key;
    }

    static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-" + idx.getAndIncrement());
        }
    }
}
