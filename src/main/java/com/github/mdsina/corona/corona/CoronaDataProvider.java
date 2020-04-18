package com.github.mdsina.corona.corona;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Singleton
public class CoronaDataProvider {

    private final CoronaStatsClient coronaStatsClient;

    public Map<String, Map> getAllCountriesStat() {
        List<Map> stats = coronaStatsClient.getAllCountriesStat().blockingGet();
        Map worldStat = coronaStatsClient.getWorldStat().blockingGet();
        worldStat.put("country", "World");
        worldStat.put("countryInfo", Map.of(
            "iso2", "World",
            "iso3", "World",
            "flag", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Emoji_u1f30d.svg/1024px-Emoji_u1f30d.svg.png"
        ));
        stats.add(worldStat);

        var namedMap = new HashMap<>(stats.size());
        var iso2Map = new HashMap<>(stats.size());
        var iso3Map = new HashMap<>(stats.size());

        for (Map stat : stats) {
            namedMap.put(((String) stat.get("country")).toLowerCase(), stat);

            Map info = (Map) stat.get("countryInfo");
            Object iso2 = info.get("iso2");
            Object iso3 = info.get("iso3");

            if (iso2 != null) {
                iso2Map.put(((String) iso2).toLowerCase(), stat);
            }
            if (iso3 != null) {
                iso3Map.put(((String) iso3).toLowerCase(), stat);
            }
        }

        return Map.of(
            "named", namedMap,
            "iso2", iso2Map,
            "iso3", iso3Map
        );
    }
}