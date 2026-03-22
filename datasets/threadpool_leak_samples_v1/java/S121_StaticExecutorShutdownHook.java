package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S121_StaticExecutorShutdownHook {
    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(2, new NamedThreadFactory("hook-safe"));

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EXECUTOR::shutdown));
    }

    public void submit() {
        EXECUTOR.submit(() -> System.out.println("hook"));
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
