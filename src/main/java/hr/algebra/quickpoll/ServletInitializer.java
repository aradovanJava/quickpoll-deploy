package hr.algebra.quickpoll;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Required when packaging the application as a WAR for deployment to an
 * external servlet container (e.g. Apache Tomcat).
 *
 * Without this class, the WAR will deploy but Spring Boot will fail to
 * wire itself into the host container's servlet context.
 *
 * It is harmless to keep this class even when running as an executable JAR.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(QuickpollApplication.class);
    }
}
