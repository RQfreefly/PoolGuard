package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S117_ExceptionHandledWithFinallyClose {
    public void run(boolean fail) {
        ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("ex-safe"));
        try {
            if (fail) {
                throw new RuntimeException("boom");
            }
            executor.submit(() -> System.out.println("ok"));
        } finally {
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
