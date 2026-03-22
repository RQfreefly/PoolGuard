package sample.dataset;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class S122_ScheduledOneShotAndShutdown {
    public void runOnce() {
        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1, new NamedThreadFactory("oneshot-safe"));
        try {
            scheduler.schedule(() -> System.out.println("once"), 1, TimeUnit.SECONDS);
        } finally {
            scheduler.shutdown();
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
