package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S006_EarlyReturnMissShutdown {
    public void execute(String payload) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        if (payload == null || payload.isEmpty()) {
            return;
        }
        executor.submit(() -> System.out.println(payload));
        executor.shutdown();
    }
}
