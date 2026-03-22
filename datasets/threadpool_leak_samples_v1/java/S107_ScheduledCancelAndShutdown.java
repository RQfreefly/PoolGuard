package sample.dataset;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class S107_ScheduledCancelAndShutdown {
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, new NamedThreadFactory("sched-stop"));
    private ScheduledFuture<?> future;

    public void start() {
        future = scheduler.scheduleAtFixedRate(() -> System.out.println("tick"), 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
        scheduler.shutdown();
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
