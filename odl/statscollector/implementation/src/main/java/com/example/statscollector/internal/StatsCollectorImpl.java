package com.example.statscollector.internal;

import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Date;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.example.statscollector.IStatsCollector;
import com.example.statscollector.Data;

import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* StatsCollectorImpl - ostvarenje sucelja IStatsCollector. StatsCollectorImpl
 * pokrece dretvu koja svake dvije sekunde sprema statisticke podatke koje
 * dohvaca od upravljackog uredaja preko sucelja IStatisticsManager u lokalni
 * LRU cache od 1000 zapisa. Implementira metodu getStats koju onda mogu
 * pozivati drugi paketi unutar OpenDaylight-a kako bi dohvatili te podatke */
public class StatsCollectorImpl implements IStatsCollector, IObjectReader{
    /* log - za spremanje bitnih dogadaja u datoteku s izvjestajem */
    private static final Logger log = LoggerFactory.getLogger(StatsCollectorImpl.class);
    /* statsCollector - dretva koja je zaduzena za periodicki dohvat
     * statistickih podataka od upravljckog uredaja*/
    private Thread statsCollector;
    /* Cache - lokalni LRU cache */
    private final Map<Long, Map<Node, List<Data> > > Cache = Collections
        .synchronizedMap(new LRUCache<Long, Map<Node, List<Data> > >(1000));

    /* LRUCache - ostvarenje LRU prirucne memorije */
    private class LRUCache <K, V> extends LinkedHashMap <K, V> {
        /* capacity - kapacitet prirucne memorije */
        private final int capacity;
        public LRUCache(int capacity) {
            super(capacity+1, 1.0f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return (super.size() > this.capacity);
        }
    }

    /* pomocna metoda koja ispisuje sadrzaj Mape bez obzira na tipove podataka */
    void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        System.out.println("--------------------");
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
        }
        System.out.println("--------------------");
    }

    public StatsCollectorImpl() {}

    /* init - metoda koja se poziva nakon ucitavanja modula u OSGi radni okvir*/
    void init() {
        log.debug("INIT called!");
        /* inicijaliziraj dretvu statsCollector */
        statsCollector = new Thread(new StatsCollectorThread());
    }

    /* destroy - metoda koja se poziva nakon micanja modula iz OSGi radnog okvira */
    void destroy() {
        log.debug("DESTROY called!");
    }

    /* start - metoda koja se poziva nakon pokretanja modula u OSGi radnom okviru*/
    void start() {
        log.debug("START called!");
        /* pokreni dretvu statsCollector */
        statsCollector.start();
    }

    /* stop - metoda koja se poziva nakon zaustavljanja modula u OSGi radnom okviru*/
    void stop() {
        log.debug("STOP called!");
        /* zaustavi dretvu statsCollector */
        statsCollector.interrupt();
    }

    /* getCurrentStatistics - glavna metoda ove klase. Koristi se za dohvat
     * trenutnih statistickih podataka od upravljackog uredaja. Koristi sucelje
     * klasa StatsManager i SwitchManager kako bi prikupila podatke oko
     * postojecih cvorova i njihovih brojaca */
    Map<Node, List<Data> > getCurrentStatistics() {
        /* naziv grupe OF komutatora */
        String containerName = "default";
        /* naziv polja u kojem je spremljen podatak o propusnosti*/
        String propertyName = "bandwidth";

        /* struktura podataka u koju spremamo rezultat */
        Map<Node, List<Data> > edge = new HashMap();

        /* dohvacamo instancu klase StatisticsManager koja pruza sucelje za
         * dohvat statistickih podataka */
        IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
            .getInstance(IStatisticsManager.class, containerName, this);

        /* dohvacamo instancu klase SwitchManager  koja pruza sucelje za dohvat
         * informacija o postojecim komutatorima */
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
            .getInstance(ISwitchManager.class, containerName, this);

        /* za svaki komutator u mrezi */
        for (Switch swc : switchManager.getNetworkDevices()) {
            /* dohvati statisticke podatke o njemu */
            Node node = swc.getNode();
            // get stats about every nodeconnector from that node
            List<NodeConnectorStatistics> stat = statsManager
                .getNodeConnectorStatistics(node);

            Map<NodeConnector, Data> node_data = new HashMap();

            /* za svaki port mreznog komutatora */
            for (NodeConnector nc : swc.getNodeConnectors()) {
                /* dohvati podatke o nazivnim vrijednostima */
                Map<String,Property> mp =
                    switchManager.getNodeConnectorProps(nc);

                /* spremi rezultat u strukturu podataka Data */
                Data data = new Data();

                // update node connector entry
                data.setNodeConnector(nc);
                // update bandwidth entry
                data.setBandwidth(mp.get(propertyName).getStringValue());

                node_data.put(nc, data);
            }

            /* za svaki port i pripadajuce statisticke podatke */
            for (NodeConnectorStatistics ncs : stat) {
                /* dohvati postojeci zapis */
                Data data = node_data.get(ncs.getNodeConnector());

                if (data == null) {
                    /* ignoriraj port na kojem je upravljacki uredaj */
                    continue;
                }

                /* dohvati i spremi statisticke podatke */
                long sent = ncs.getReceivePacketCount() + ncs.getTransmitPacketCount();
                long drop = ncs.getReceiveDropCount() + ncs.getTransmitDropCount();

                // update packet drop entry
                data.setPacketDrop(drop);
                // update packet sent entry
                data.setPacketSent(sent);

                // update transmit byte count entry
                data.setTxCount(ncs.getTransmitByteCount());
                // update receive byte count entry
                data.setRxCount(ncs.getReceiveByteCount());

                node_data.put(ncs.getNodeConnector(), data);
            }

            /* pohrani prikupljene podatke o mreznom komutatoru u rezultat */
            List<Data> list_data = new ArrayList();

            for (NodeConnector key : node_data.keySet()) {
                // move full stats info in a list
                list_data.add(node_data.get(key));
            }
            // update result map
            edge.put(node, list_data);
        }
        return edge;
    }

    /* StatsCollectorThread - dretva zaduzena za periodicko prikupljanje
     * statistickih podataka */
    private class StatsCollectorThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    /* dohvati trenutnu vremensku oznaku */
                    Date date = new java.util.Date();
                    /* dohvati statisticke podatke */
                    Map<Node, List<Data> > res = getCurrentStatistics();
                    /* pohrani rezultat u prirucnu memoriju */
                    Cache.put(date.getTime(), res);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.out.println("Something went wrong");
                }
            }
        }
    }

    /* getStats - sucelje za dohvat statistickih podataka */
    @Override
    public Map<Long, Map<Node, List<Data> > > getStats() {
        return new HashMap<Long, Map<Node, List<Data> > >(Cache);
    }

    /* getTest - sucelje za ispitivanje ispravnosti */
    @Override
    public List<String> getTest() {
        List<String> ret = new ArrayList();

        ret.add("test1");
        ret.add("test2");
        ret.add("test3");

        return ret;
    }

    /* readObject - sucelje koje omogucuje prijenost podataka prema drugima
     * klasama */
    @Override
    public Object readObject(ObjectInputStream ois)
        throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

}
