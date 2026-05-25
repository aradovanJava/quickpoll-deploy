package hr.algebra.quickpoll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QuickPoll — a deliberately small Spring Boot app used to demonstrate
 * every deployment method covered in the "Java Web Application Deployment"
 * lecture: JAR, WAR, Docker, Docker Compose, and every major free cloud
 * platform.
 *
 * Run locally from IntelliJ by right-clicking this class → Run.
 */
@SpringBootApplication
public class QuickpollApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickpollApplication.class, args);
    }
}
