package local.activitytracker.finance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
public class FinancialAnalysisController {
    private final FinancialAnalysisService analysisService;

    public FinancialAnalysisController(FinancialAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/api/finance/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/api/finance/months")
    public List<FinanceResponse.MonthOption> months() {
        try {
            return analysisService.availableMonths();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read the configured monthly financial documents directory.",
                    exception
            );
        }
    }

    @GetMapping("/api/finance")
    public FinanceResponse analyze(@RequestParam String month) {
        try {
            return analysisService.analyze(YearMonth.parse(month));
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Month must use the yyyy-MM format.", exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not analyze the configured monthly financial documents.",
                    exception
            );
        }
    }
}
