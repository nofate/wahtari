package wahtari.http;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.PrettifyOutputStream;
import com.dslplatform.json.runtime.Settings;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import wahtari.data.HourlyInfo;
import wahtari.data.StatsService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Deque;
import java.util.List;


@Component
public class ReportHandler implements HttpHandler {

    private StatsService statsService;
    private DslJson<Object> dslJson ;

    public ReportHandler(@Autowired StatsService statsService) {
        this.statsService = statsService;
        this.dslJson = new DslJson<>(Settings.withRuntime().includeServiceLoader());
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String customerIdStr = getParam(exchange, "customerId");
        String dateStr = getParam(exchange, "date");
        if (customerIdStr == null || dateStr == null) {
            exchange.setStatusCode(400);
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(customerIdStr);
        } catch (NumberFormatException e) {
            exchange.setStatusCode(400);
            return;
        }


        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            exchange.setStatusCode(400);
            return;
        }

        List<HourlyInfo> report = statsService.getReport(date, customerId);
        ReportDto reportDto = new ReportDto();
        for (HourlyInfo hourlyInfo : report) {
            reportDto.addReportItem(hourlyInfo.hour(), new ReportDto.ReportItemDto(hourlyInfo.requestCount(), hourlyInfo.invalidCount()));
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        dslJson.serialize(reportDto, new PrettifyOutputStream(exchange.getOutputStream()));

    }

    private static String getParam(HttpServerExchange exchange, String param) {
        Deque<String> callbackParam = exchange.getQueryParameters().get(param);
        if (callbackParam == null) {
            return null;
        }
        return callbackParam.getFirst();
    }
}
