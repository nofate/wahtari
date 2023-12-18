package wahtari.http;

import com.dslplatform.json.*;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@CompiledJson
public class ReportDto {

    @JsonAttribute(name = "total")
    ReportItemDto total = new ReportItemDto(0, 0);

    @JsonAttribute(name = "hourly")
    Map<Instant, ReportItemDto> hourly = new LinkedHashMap<>();

    public ReportItemDto getTotal() {
        return total;
    }

    public Map<Instant, ReportItemDto> getHourly() {
        return hourly;
    }

    public void setTotal(ReportItemDto total) {
        this.total = total;
    }

    public void setHourly(Map<Instant, ReportItemDto> hourly) {
        this.hourly = hourly;
    }

    public void addReportItem(Instant hour, ReportItemDto item) {
        ReportItemDto reportItemDto = hourly.computeIfAbsent(hour, h -> new ReportItemDto(0, 0));
        reportItemDto.requests += item.requests;
        reportItemDto.invalid += item.invalid;

        total.invalid += item.invalid;
        total.requests += item.requests;
    }

    @CompiledJson
    public static class ReportItemDto {
        @JsonAttribute(name = "requests")
        long requests;
        @JsonAttribute(name = "invalid")
        long invalid;

        public ReportItemDto(long requests, long invalid) {
            this.requests = requests;
            this.invalid = invalid;
        }

        public long getRequests() {
            return requests;
        }

        public long getInvalid() {
            return invalid;
        }
    }

    @JsonConverter(target = Instant.class)
    public static abstract class LocalTimeConverter {
        public static Instant read(JsonReader reader) throws IOException {
            if (reader.wasNull()) return null;
            return Instant.parse(reader.readSimpleString());
        }
        public static void write(JsonWriter writer, Instant value) {
            if (value == null) {
                writer.writeNull();
            } else {
                writer.writeString(value.toString());
            }
        }
    }

}
