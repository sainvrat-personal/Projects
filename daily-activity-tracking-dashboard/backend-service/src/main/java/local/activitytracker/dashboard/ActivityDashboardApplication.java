package local.activitytracker.dashboard;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(info = @Info(
        title = "Activity Dashboard API",
        version = "1.0.0",
        description = "Local API for dashboard summaries and weekly activity rollover."
))
public class ActivityDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityDashboardApplication.class, args);
    }
}
