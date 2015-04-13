package com.example.statscollector;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public interface IStatsCollector {

    public Map<Long, Map<Node, List<Data> > > getStats();

    public Map<Node, List<Data> > getLatestStats();

    public List<String> getTest();
}
