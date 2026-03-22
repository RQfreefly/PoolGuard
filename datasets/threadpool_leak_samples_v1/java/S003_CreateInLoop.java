package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S003_CreateInLoop {
    public void batchProcess(int count) {
        for (int i = 0; i < count; i++) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> System.out.println("item"));
        }
    }
}
