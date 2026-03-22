package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S009_MethodLocalCreateNoClose2 {
    public void syncUser() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() -> System.out.println("sync"));
    }
}
