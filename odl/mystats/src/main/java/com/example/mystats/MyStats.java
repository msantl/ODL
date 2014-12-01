package com.example.mystats;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyStats{
	private static final Logger log = LoggerFactory.getLogger(MyStats.class);

	public MyStats() {

	}

	void init() {
		log.debug("INIT called!");
	}

	void destroy() {
		log.debug("DESTROY called!");
	}

	void start() {
		log.debug("START called!");
		getFlowStatistics();
	}

	void stop() {
		log.debug("STOP called!");
	}

	void getFlowStatistics() {
		String containerName = "default";
		IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
			.getInstance(IStatisticsManager.class, containerName, this);

		ISwitchManager switchManager = (ISwitchManager) ServiceHelper
			.getInstance(ISwitchManager.class, containerName, this);

		for (Node node : switchManager.getNodes()) {
			List<NodeConnectorStatistics> stat = statsManager
				.getNodeConnectorStatistics(node);

			System.out.println("Node: " + node);
			for (NodeConnectorStatistics ncs : stat) {
				long packet_loss = ncs.getReceiveDropCount() + ncs.getTransmitDropCount();
				long rx = ncs.getReceivePacketCount();
				long tx = ncs.getTransmitPacketCount();
				System.out.println("\tNode connector " + ncs.getNodeConnector().getNodeConnectorIdAsString());
				System.out.println("\t\tPacket loss " + packet_loss);
				System.out.println("\t\tReceived " + rx);
				System.out.println("\t\tTransmitted " + tx);
			}
			System.out.println();
		}
	}
}
