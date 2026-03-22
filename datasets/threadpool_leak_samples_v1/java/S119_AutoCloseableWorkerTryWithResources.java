package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S119_AutoCloseableWorkerTryWithResources {
    public void doWork() {
        try (Worker worker = new Worker()) {
            worker.submit();
        }
    }

    static class Worker implements AutoCloseable {
        private final ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("worker-safe"));

        void submit() {
            executor.submit(() -> System.out.println("worker"));
        }

        @Override
        public void close() {
            executor.shutdown();
        }
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
