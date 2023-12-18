# How to run me

```bash
$ ./gradlew bootRun
```

This implementation bundles in-memory H2 database for demonstration purposes.
On each start it applies a Flyway migration `src/main/resources/db/migrations/V1_0__test_data.sql` with
the DDL schema provided to me and test data.

If you prefer to run the service with standalone DBMS feel free to change JDBC settings 
in `src/main/resources/application.properties` or override them using Java system properties
```bash
$ ./gradlew bootRun --args=--spring.datasource.url=...
```

# Design considerations

Since original problem statement didn't include any constraints apart from "billions of requests each day",
I take the liberty to interpret it at my own discretion as "design towards overall performance.

 - I expanded the requirement for blacklists of IP addresses to blacklists of subnets. 
 - I was unsure which timestamps to take in consideration for reporting: timestampID in incoming
   messages or actual time. I chose the former.  


## Validation service

This service must reject invalid messages within minimal timespan. Therefore, we cannot afford
to check several blacklists in database. Instead, it makes sense to load all required data
into JVM memory, refreshing it with a reasonable delay. This also allows to use effective 
data structures:

 - Hashmap for customers;
 - Radix-tree (PATRICIA-trie) for subnet blacklists. I keep all right and left edges of tree as a pair
   of primitive arrays and indices of terminal nodes as a bitmask in order to minimize memory footrpint
   and fragmentation. The tree is intentionally immutable.
 - Aho-Corasik automata for user agent blacklists. For this one I used mature open source implementation.

Reload interval is defined by the `dao.reload.delayMs` property.

Also, for performance reasons I didn't use Spring-Web.

## Reporting service

For the same reasons it's impossible to immediately per-request update request counters in DMBS.   
For each combination of customerID and hour I keep a pair of counters. They are dumped periodically
into database with an upsert (insert + on duplicate update) query. Time interval between save invocations
should be tuned (`snapshot.delayMs`) according to real workload and the tolerable amount of data that can be lost 
due to possible service failure.


# Further improvements

These ideas seemed to be out of the scope of the coding assessment:

- Undertow web server is quite handy for prototyping, but it can be outperformed by other frameworks. Benchmarking
  on a real-world workload is required.
- Possible data loss can be minimized by something like a write-ahead log or even a tiny disk merge-tree implementation 
  collecting counters on a SSD.


-----
_Wahtāri [Old High German] — Watchman_
