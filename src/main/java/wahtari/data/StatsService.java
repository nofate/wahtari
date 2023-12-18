package wahtari.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    record StatKey(Instant hour, int customerId) {}
    record StatValue(AtomicLong requestCounter, AtomicLong invalidCounter) {
        static StatValue empty() {
            return new StatValue(new AtomicLong(), new AtomicLong());
        }
    }

    private volatile ConcurrentHashMap<StatKey, StatValue> snapshot;
    private DataSource dataSource;

    public StatsService(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
        snapshot = new ConcurrentHashMap<>();
    }

    public void markValid(long ts, int customerId) {
        Instant hour = Instant.ofEpochSecond(ts).truncatedTo(ChronoUnit.HOURS);
        StatKey key = new StatKey(hour, customerId);
        snapshot.computeIfAbsent(key, k -> StatValue.empty()).requestCounter().incrementAndGet();
    }

    public void markInvalid(long ts, int customerId) {
        Instant hour = Instant.ofEpochSecond(ts).truncatedTo(ChronoUnit.HOURS);
        StatKey key = new StatKey(hour, customerId);
        snapshot.computeIfAbsent(key, k -> StatValue.empty()).invalidCounter().incrementAndGet();
    }

    public List<HourlyInfo> getReport(LocalDate date, int customerId) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String sql = """
                select time, request_count, invalid_count from hourly_stats
                where customer_id=? and cast(time as date)=? 
                order by time
                """;
        List<HourlyInfo> results = jdbc.query(sql, (rs, i) -> new HourlyInfo(customerId,
                rs.getTimestamp(1).toInstant(), rs.getLong(2), rs.getLong(3)),
                customerId, date);

        // append snapshot values
        Instant currentHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        StatValue statValue = snapshot.get(new StatKey(currentHour, customerId));
        if (statValue != null) {
            results.add(new HourlyInfo(customerId, currentHour, statValue.requestCounter().get(), statValue.invalidCounter().get()));
        }

        return results;
    }

    @Scheduled(fixedDelayString = "${snapshot.delayMs:10000}")
    public void storeSnapshotJob() {
        try {
            storeSnapshot();
        } catch (Exception e) {
            log.error("failed to store stat snapshot", e);
        }
    }

    @Transactional
    public void storeSnapshot() {
        ConcurrentHashMap<StatKey, StatValue> snapshotToStore = snapshot;
        snapshot = new ConcurrentHashMap<>();

        if (log.isTraceEnabled()) {
            log.trace("Storing snapshot to DB: {}", snapshotToStore);
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String sql = """
                    insert into hourly_stats(customer_id, time, request_count, invalid_count)
                    values (?, ?, ?, ?)
                    on duplicate key update 
                    request_count = request_count + values(request_count),
                    invalid_count = invalid_count + values(invalid_count);
                    """;
        int[][] rowsAffected = jdbc.batchUpdate(sql, snapshotToStore.entrySet(), snapshotToStore.size(),
                (ps, arg) -> {
                    StatKey key = arg.getKey();
                    StatValue value = arg.getValue();
                    ps.setInt(1, key.customerId());
                    ps.setTimestamp(2, Timestamp.from(key.hour()));
                    ps.setLong(3, value.requestCounter().get());
                    ps.setLong(4, value.invalidCounter().get());
                });
        if (log.isDebugEnabled()) {
            log.debug("Stored snapshot to DB, rowsAffected: {}", Arrays.deepToString(rowsAffected));
        }
    }
}
