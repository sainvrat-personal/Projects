package local.activitytracker.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
public class DashboardController {
    private final WeeklyActivityScanner scanner;
    private final WeeklyActivityRolloverService rolloverService;

    public DashboardController(
            WeeklyActivityScanner scanner,
            WeeklyActivityRolloverService rolloverService
    ) {
        this.scanner = scanner;
        this.rolloverService = rolloverService;
    }

    @GetMapping("/api/health")
    @Operation(summary = "Check service health", description = "Returns a basic health status for the dashboard service.")
    @ApiResponse(responseCode = "200", description = "Service is running")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/api/rollover")
    @Operation(
            summary = "Run weekly rollover",
            description = "Creates the current week's sheet if needed and archives valid sheets from earlier weeks."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rollover completed"),
            @ApiResponse(responseCode = "500", description = "Could not process the weekly activity sheet")
    })
    public Map<String, String> rollover() {
        try {
            rolloverService.processCurrentWeek();
            return Map.of("status", "ok", "message", "Weekly activity sheet is up to date.");
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not process the weekly activity sheet: " + exception.getMessage(),
                    exception
            );
        }
    }

    @GetMapping("/api/dashboard")
    @Operation(
            summary = "Get dashboard activity",
            description = "Scans weekly Markdown activity files and returns summary data, optionally filtered by date."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard data returned"),
            @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    public DashboardResponse dashboard(
            @RequestParam(required = false)
            @Parameter(description = "Inclusive start date in ISO format (yyyy-MM-dd)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @Parameter(description = "Inclusive end date in ISO format (yyyy-MM-dd)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        try {
            return scanner.scan(from, to);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
