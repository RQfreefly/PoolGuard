package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S015_ConditionalShutdownMissException {
    public void run(String payload) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        if (payload == null) {
            throw new IllegalArgumentException("payload");
        }
        executor.submit(() -> System.out.println(payload));
        executor.shutdown();
    }
}
