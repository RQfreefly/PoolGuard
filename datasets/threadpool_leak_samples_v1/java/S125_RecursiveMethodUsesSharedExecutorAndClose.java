package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S125_RecursiveMethodUsesSharedExecutorAndClose {
    public void walk(int depth) {
        ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("rec-safe"));
        try {
            walkInner(depth, executor);
        } finally {
            executor.shutdown();
        }
    }

    private void walkInner(int depth, ExecutorService executor) {
        executor.submit(() -> System.out.println("depth=" + depth));
        if (depth > 0) {
            walkInner(depth - 1, executor);
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
