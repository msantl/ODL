package com.example.statscollector;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

/* IStatsCollector - sucelje koje se pruza drugim paketima unutar
 * OpenDaylight-a*/
public interface IStatsCollector {
    /* getStats - metoda za dohvat prikupljenih statistickih podataka */
    public Map<Long, Map<Node, List<Data> > > getStats();
    /* getTest - metoda za ispitivanje ispravnosti sucelja */
    public List<String> getTest();
}
