package wahtari.data;

import java.time.Instant;

public record HourlyInfo(int customerId, Instant hour, long requestCount, long invalidCount) {}
