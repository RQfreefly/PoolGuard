package sample.dataset;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class S113_SingletonWithPreDestroy {
    private final ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("singleton"));

    public void submit() {
        executor.submit(() -> System.out.println("task"));
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
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
