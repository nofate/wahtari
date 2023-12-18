package wahtari.http;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.Settings;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import wahtari.data.InMemoryDao;
import wahtari.data.StatsService;

import java.util.Optional;


@Component
public class IngestHandler implements HttpHandler {

    private final DslJson<Object> dslJson;
    private StatsService statsService;
    private InMemoryDao dao;

    public IngestHandler(@Autowired StatsService statsService, @Autowired InMemoryDao dao) {
        this.statsService = statsService;
        this.dao = dao;

        dslJson = new DslJson<>(Settings.withRuntime().includeServiceLoader());
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {

        if (!exchange.getRequestMethod().equals(Methods.POST)) {
            exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
            return;
        }

        // validate JSON
        MessageDto msg = parseMessage(exchange);
        if(msg == null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("Unprocessable JSON");
            return;
        }

        // validate fields
        if (!msg.validate()) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("Missing mandatory fields");

            Integer customerId = msg.getCustomerId();
            if (customerId != null && dao.customerExists(customerId)) {
                statsService.markInvalid(msg.getTimestamp(), customerId);
            }
            return;
        }

        // validate customer
        Optional<Boolean> active = dao.customerActive(msg.getCustomerId());
        if (active.isEmpty()) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("Customer not found");
            return;
        } else if (active.get()) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("Customer is not active");
            statsService.markInvalid(msg.getTimestamp(), msg.getCustomerId());
            return;
        }

        // validate IP
        int ip = ipV4StringToInt(msg.getRemoteIp());
        if (dao.addressBlacklisted(ip)) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("IP address blacklisted");
            statsService.markInvalid(msg.getTimestamp(), msg.getCustomerId());
            return;
        }

        // validate UA
        String ua = exchange.getRequestHeaders().getFirst(Headers.USER_AGENT);
        if (ua != null && dao.userAgentBlacklisted(ua)) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("UserAgent blacklisted");
            statsService.markInvalid(msg.getTimestamp(), msg.getCustomerId());
            return;
        }

        statsService.markValid(msg.getTimestamp(), msg.getCustomerId());
        handleValidMessage(msg);
    }

    private void handleValidMessage(MessageDto msg) {
        // message processor stub
    }

    private MessageDto parseMessage(HttpServerExchange exchange) {
        try {
            return dslJson.deserialize(MessageDto.class, exchange.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }

    private static int ipV4StringToInt(String ipAddress) {
        String[] ipSegments = ipAddress.split("\\.");
        int result = 0;

        for (int i = 0; i < 4; i++) {
            result |= (Integer.parseInt(ipSegments[i]) << ((3 - i) * 8));
        }
        return result;
    }
}
