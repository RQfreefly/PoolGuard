package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class S025_ControllerLikeMethodCreatesPool {
    @GetMapping("/ping")
    public String ping() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> System.out.println("http"));
        return "pong";
    }
}
