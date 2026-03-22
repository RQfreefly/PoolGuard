package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S104_CustomThreadFactoryAndClose {
    public void runTask() {
        ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("biz-worker"));
        try {
            executor.submit(() -> System.out.println("biz"));
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
