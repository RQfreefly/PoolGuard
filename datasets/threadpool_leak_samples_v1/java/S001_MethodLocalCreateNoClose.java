package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S001_MethodLocalCreateNoClose {
    public String handleRequest() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.submit(() -> System.out.println("work"));
        return "ok";
    }
}
