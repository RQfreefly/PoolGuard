package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S019_HelperOwnsExecutorNoClose {
    public void invoke() {
        Helper helper = new Helper();
        helper.runAsync();
    }

    static class Helper {
        private final ExecutorService executor = Executors.newFixedThreadPool(2);

        void runAsync() {
            executor.submit(() -> System.out.println("helper"));
        }
    }
}
