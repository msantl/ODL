package com.example.mystats;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.example.statscollector.IStatsCollector;
import com.example.statscollector.Data;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * MyStats
 *  statisticke podatke koje prikuplja StatsCollector te ih nadopunjuje
 *  podacima o topologiji mreze
 */
public class MyStats{
    /* log - za spremanje bitnih dogadaja u datoteku s izvjestajem */
    private static final Logger log = LoggerFactory.getLogger(MyStats.class);

    public MyStats() { }

    /* init - metoda koja se poziva nakon ucitavanja modula u OSGi radni okvir*/
    void init() {
        log.debug("INIT called!");
    }

    /* destroy - metoda koja se poziva nakon micanja modula iz OSGi radnog okvira */
    void destroy() {
        log.debug("DESTROY called!");
    }

    /* start - metoda koja se poziva nakon pokretanja modula u OSGi radnom okviru*/
    void start() {
        log.debug("START called!");
        getFlowStatistics();
    }

    /* stop - metoda koja se poziva nakon zaustavljanja modula u OSGi radnom okviru*/
    void stop() {
        log.debug("STOP called!");
    }

    /* pomocna metoda koja ispisuje sadrzaj Mape bez obzira na tipove podataka */
    void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        System.out.println("--------------------");
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            try {
                System.out.println(pairs.getKey() + " = " + pairs.getValue());
            } catch (Exception e) {
                System.out.println("n/a");
            }
        }
        System.out.println("--------------------");
    }

    /* getFlowStatistics - glavna metoda ove klase. Koristi sucelje klase
     * StatsCollector kako bi dohvatila prikupljene podatke. Nakon toga te
     * podatke nadopunjuje informacijama o topologiji mreze. */
    void getFlowStatistics() {
        /* naziv grupe OF komutatora */
        String containerName = "default";

        /* struktura podataka u koju spremamo sve podatke */
        Map<Node, List<Data> > edge = new HashMap();
        /* struktura podataka u koju spremamo statisticke podatke */
        Map<Long, Map<Node, List<Data> > > res;

        /* dohvacamo instancu klase TopologyManager koja pruza sucelje za
         * dohvacanje trenutne topologije mreze */
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
            .getInstance(ITopologyManager.class, containerName, this);

        /* dohvacamo instancu klase StatsCollector koja pruza sucelje za
         * dohvacanje prikupljenih statistickih podataka */
        IStatsCollector statsCollector = (IStatsCollector) ServiceHelper
            .getInstance(IStatsCollector.class, containerName, this);

        /* provjeri da je StatsCollector pokrenut*/
        if (statsCollector != null) {
            /* dohvati prikupljene podatke */
            res = statsCollector.getStats();

            /* uzmi najnoviji podatak */
            Long latest = null;
            for (Long timestamp : res.keySet()) {
                if (latest == null || timestamp > latest) {
                    edge = res.get(timestamp);
                    latest = timestamp;
                }
            }

            System.out.println("Time: " + latest);
        } else {
            /* ispisi pogresku i izadi */
            System.out.println("StatsCollector not present!");
            return;
        }

        /* dohvati trenutnu topologiju mreze */
        Map<Node,Set<Edge>> topology = topologyManager.getNodeEdges();

        /* za svaki cvor za koji imamo prikupljene statisticke podatke */
        for (Node key : edge.keySet()) {
            /* za svakog neposrednog susjeda cvora*/
            for (Edge e : topology.get(key)) {
                // get topology information about that node

                /* dohvati podatke njihovoj povezanosti */
                NodeConnector tail_nc = e.getTailNodeConnector();
                NodeConnector head_nc = e.getHeadNodeConnector();

                Node tail = tail_nc.getNode();
                Node head = head_nc.getNode();

                /* osvjezi strukturu podataka s informacijom o poveznasnoti */
                List<Data> neighbours = edge.get(head);
                for (Data n : neighbours) {
                    if (head_nc.equals(n.getNodeConnector())) {
                        n.setNode(tail);
                    }
                }
            }
        }

        /* za svaki cvor za koji imamo prikupljene statisticke podatke */
        for (Node key : edge.keySet()) {
            /* za svaki zapis koji imamo za taj covr */
            for (Data n : edge.get(key)) {
                /* dohvati listu hostova koji su spojeni na neki od portova */
                List<Host> hosts = topologyManager
                    .getHostsAttachedToNodeConnector(n.getNodeConnector());

                /* osvjezi strukturu podataka s informacijom o hostovima */
                if (hosts != null) {
                    n.setHosts(hosts);
                }
            }
        }

        /* ispisi prikupljene informacije */
        for (Node key : edge.keySet()) {
            System.out.println("Switch: " + key.toString());

            for (Data data : edge.get(key)) {
                if (data.getNode() != null) {
                    System.out.println("\tNode: " + data.getNode().toString());
                }
                if (data.getHosts() != null) {
                    System.out.print("\tHosts: ");
                    for (Host h : data.getHosts()) {
                        System.out.print(h.getNetworkAddressAsString() + ", ");
                    }
                    System.out.println("");
                }

                System.out.println("\tNodeConnector: " +
                        data.getNodeConnector().getNodeConnectorIdAsString());
                System.out.println("\tPacketDrop: " + data.getPacketDrop());
                System.out.println("\tPacketSent: " + data.getPacketSent());

                System.out.println("\tTx count: " + data.getTxCount());
                System.out.println("\tRx count: " + data.getRxCount());
                System.out.println("\tBandwidth: " + data.getBandwidth());
                System.out.println("");
            }
        }

        /* TODO run A* on collected data */
        /* TODO install a new flow using flowManager */

        return;
    }
}
