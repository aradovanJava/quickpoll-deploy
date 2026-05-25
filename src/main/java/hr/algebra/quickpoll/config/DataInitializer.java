package hr.algebra.quickpoll.config;

import hr.algebra.quickpoll.repository.PollRepository;
import hr.algebra.quickpoll.service.PollService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Seeds two demo polls on first startup. Useful so that students see something
 * interesting immediately after deploying, regardless of which platform they
 * choose.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(PollRepository pollRepository, PollService pollService) {
        return args -> {
            if (pollRepository.count() > 0) {
                return;
            }
            pollService.create(
                    "Which Java version do you ship to production?",
                    List.of("Java 17", "Java 21", "Java 25", "Still on 11 (don't judge)")
            );
            pollService.create(
                    "Best way to deploy a Spring Boot app?",
                    List.of("Runnable JAR + cloud", "WAR on Tomcat", "Docker everywhere", "Serverless / Cloud Run")
            );
        };
    }
}
