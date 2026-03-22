package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S111_VirtualThreadTryWithResources2 {
    public void execute() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> System.out.println("ok"));
        }
    }
}
