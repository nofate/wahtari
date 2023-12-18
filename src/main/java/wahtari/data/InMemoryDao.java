package wahtari.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wahtari.data.util.AhoCorasickOptimized;
import wahtari.data.util.Cidr;
import wahtari.data.util.Ip4NetworkSet;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Component("dao")
public class InMemoryDao {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDao.class);

    record State (Map<Integer, Boolean> customersIndex, Ip4NetworkSet networkBlacklist, AhoCorasickOptimized userAgentBlacklist) {}

    private volatile State state;

    @Autowired
    private DataSource dataSource;

    public Optional<Boolean> customerActive(int id) {
        if (state == null) throw new IllegalStateException("Service is not ready");
        return Optional.ofNullable(state.customersIndex().get(id));
    }

    public boolean customerExists(int id) {
        if (state == null) throw new IllegalStateException("Service is not ready");
        return state.customersIndex().containsKey(id);
    }

    public boolean addressBlacklisted(int addr) {
        if (state == null) throw new IllegalStateException("Service is not ready");
        return state.networkBlacklist().contains(addr);
    }

    public boolean userAgentBlacklisted(String userAgent) {
        if (state == null) throw new IllegalStateException("Service is not ready");
        return state.userAgentBlacklist().match(userAgent);
    }

    @Transactional
    public void reload() {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Map<Integer, Boolean> customersIndex = reloadCustomers(jdbc);
            Ip4NetworkSet networkTrie = reloadNetworkBlacklist(jdbc);
            AhoCorasickOptimized userAgentTrie = reloadUserAgentBlacklist(jdbc);

            state = new State(customersIndex, networkTrie, userAgentTrie);
        } catch (Exception e) {
            log.error("Database reload failed", e);
        }
        log.info("Database reloaded");
    }

    private Map<Integer, Boolean> reloadCustomers(JdbcTemplate jdbc) {
        var customers = jdbc.query("select id, active from customer",
                (rs, i) -> new Customer(rs.getInt(1), rs.getBoolean(2)));
        return customers.stream().collect(Collectors.toUnmodifiableMap(Customer::id, Customer::active));
    }

    private Ip4NetworkSet reloadNetworkBlacklist(JdbcTemplate jdbc) {
        var cidrs = jdbc.query("select address, netmask_bits from ip_blacklist",
                (rs, i) -> new Cidr((int) rs.getLong(1), rs.getByte(2)));
        Ip4NetworkSet blacklist = new Ip4NetworkSet();
        for (Cidr cidr : cidrs) {
            blacklist.put(cidr);
        }
        return blacklist;
    }

    private AhoCorasickOptimized reloadUserAgentBlacklist(JdbcTemplate jdbc) {
        var userAgents = jdbc.query("select ua from ua_blacklist", SingleColumnRowMapper.newInstance(String.class));
        String[] arr = userAgents.toArray(new String[0]);
        return new AhoCorasickOptimized(arr);
    }
}
