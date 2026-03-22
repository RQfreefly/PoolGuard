package sample.dataset;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class S105_PreDestroyShutdown {
    private final ExecutorService executor = Executors.newFixedThreadPool(4, new NamedThreadFactory("component"));

    public void submitTask() {
        executor.submit(() -> System.out.println("component task"));
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
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
