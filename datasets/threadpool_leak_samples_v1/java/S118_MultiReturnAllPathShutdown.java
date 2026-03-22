package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class S118_MultiReturnAllPathShutdown {
    public int handle(int code) {
        ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("ret-safe"));
        try {
            if (code < 0) {
                return -1;
            }
            if (code == 0) {
                return 0;
            }
            executor.submit(() -> System.out.println("run"));
            return 1;
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
