package sample.dataset;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S011_CreateInForEachLoop {
    public void handle(List<String> items) {
        for (String item : items) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> System.out.println(item));
        }
    }
}
