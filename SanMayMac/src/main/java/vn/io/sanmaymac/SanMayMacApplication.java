package vn.io.sanmaymac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SanMayMacApplication {

    public static void main(String[] args) {
        SpringApplication.run(SanMayMacApplication.class, args);
    }

}
