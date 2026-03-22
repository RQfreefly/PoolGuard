package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S012_CreateInWhileLoop {
    public void pollAndRun() {
        int i = 0;
        while (i < 5) {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> System.out.println("poll"));
            i++;
        }
    }
}
