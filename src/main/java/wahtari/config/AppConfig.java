package wahtari.config;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import wahtari.data.InMemoryDao;
import wahtari.http.IngestHandler;
import wahtari.http.ReportHandler;


@Configuration
@EnableScheduling
public class AppConfig {
    public static final int IO_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    public static final int WORKER_THREADS = 100;

    @Value("${http.port}")
    private int port;

    @Value("${http.host}")
    private String host;

    @Bean(destroyMethod = "stop")
    @DependsOn("dao")
    public Undertow getUndertow(
            @Autowired IngestHandler ingestHandler,
            @Autowired ReportHandler reportHandler
    ) {
        PathHandler topLevelHandler = Handlers.path()
                .addExactPath("/report", new BlockingHandler(reportHandler))
                .addExactPath("/ingest", new BlockingHandler(ingestHandler));

        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(IO_THREADS)
                .setWorkerThreads(WORKER_THREADS)
                .setServerOption(UndertowOptions.ENABLE_STATISTICS, false)
                .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                .setHandler(topLevelHandler)
                .build();
        server.start();
        return server;
    }

    @Autowired InMemoryDao dao;

    @Scheduled(fixedDelayString = "${dao.reload.delayMs}")
    public void reload() {
        dao.reload();
    }
}
