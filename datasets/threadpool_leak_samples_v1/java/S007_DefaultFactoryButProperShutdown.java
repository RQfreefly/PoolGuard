package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S007_DefaultFactoryButProperShutdown {
    public void executeSafely() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.submit(() -> System.out.println("safe shutdown"));
        } finally {
            executor.shutdown();
        }
    }
}
