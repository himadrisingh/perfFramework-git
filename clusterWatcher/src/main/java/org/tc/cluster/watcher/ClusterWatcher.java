package org.tc.cluster.watcher;

import static org.tc.cluster.watcher.util.ClusterWatcherProperties.LOG;

import java.util.List;

import javax.mail.MessagingException;
import javax.management.ObjectName;

import org.tc.cluster.watcher.logger.StatsLogger;
import org.tc.cluster.watcher.logger.TerracottaStatsLogger;
import org.tc.cluster.watcher.mail.Mail;
import org.tc.cluster.watcher.notification.DGCNotificationListener;
import org.tc.cluster.watcher.notification.OperatorEventsNotificationListener;
import org.tc.cluster.watcher.util.ClusterWatcherProperties;
import org.tc.cluster.watcher.util.Utils;

public class ClusterWatcher implements Runnable {
	private static final String NL = "\n";

	private final ClusterWatcherProperties props;
	private final StatsLogger tcStats;
	private final DGCNotificationListener dgcListener;
	private final OperatorEventsNotificationListener opsEventsListener;

	private int lowTxrProbeCount, clusterDownProbeCount,
			missingClientsProbeCount, serverDownProbeCount;
	private List<MirrorGroup> clusterList;
	private Mail mail;

	public ClusterWatcher(ClusterWatcherProperties p) {
		props = p;
		tcStats = new TerracottaStatsLogger(p.getTerracottaStatsLog());

		int count = 0;
		do {
			this.clusterList = Utils.getHostAndJMXStringFromTcConfig(props
					.getTcConfig());
			if (this.clusterList == null) {
				LOG.error("Cluster is not up. Retrying in 5 secs...");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while (clusterList == null && count++ < props.getInitialRetries());

		if (clusterList == null)
			throw new IllegalStateException("Cluster is not up.");

		for (MirrorGroup mg : clusterList)
			LOG.info(mg);

		ServerStat serverStat = getActiveCoordinator();
		dgcListener = DGCNotificationListener.create(props.getDgcStatsLog());
		opsEventsListener = OperatorEventsNotificationListener.create(
				serverStat, props.getOpsStatsLog());
		serverStat.registerDgcNotificationListener(dgcListener);
		serverStat.registerEventNotificationListener(opsEventsListener);

		try {
			mail = new Mail(props.getSmtpServer(), props.getRecipients());
			if (props.getSmtpPasswd() != null
					&& props.getSmtpUsername() != null)
				mail.setAuthentication(props.getSmtpUsername(),
						props.getSmtpPasswd());
		} catch (IllegalArgumentException e) {
			LOG.error(String.format(
					"Initialization failed for SMTP: %s & RECIPIENTS: %s",
					props.getSmtpServer(), props.getRecipients()));
		}
		LOG.info("ClusterWatcher initialized. Cluster: ");
	}

	private void shutdown() {
		tcStats.close();
		LOG.removeAllAppenders();
		for (MirrorGroup mg : clusterList)
			for (ServerStat s : mg.servers())
				s.shutdown();
		LOG.info("ClusterWatcher exited.");
	}

	public void run() {
		int mail_interval = 1;
		while (true) {
			try {
				Thread.sleep(props.getProbeInterval());
			} catch (InterruptedException e) {
				shutdown();
				return;
			}
			if (!allServerOnline()) {
				LOG.fatal("ALL SERVERS ARE NOT ONLINE. Count: "
						+ serverDownProbeCount++ + " , Threshold: "
						+ props.getServerDownMaxCheck());
				check(serverDownProbeCount == props.getServerDownMaxCheck(),
						"One of the servers is down. Threshold of "
								+ props.getServerDownMaxCheck());

			}
			ServerStat serverStat = getActiveCoordinator();
			if (serverStat != null) {
				if (clusterDownProbeCount != 0){
					serverStat.registerDgcNotificationListener(dgcListener);
					serverStat.registerEventNotificationListener(opsEventsListener);
					clusterDownProbeCount = 0;
				}
				LOG.info(serverStat + " is ACTIVE-COORDINATOR");
				try {
					checkClients(serverStat);
					checkLowTxnRate(serverStat);
					tcStats.logStats(serverStat);
				} catch (NotConnectedException e) {
					LOG.debug(e.getMessage());
					LOG.error(e.getLocalizedMessage());
				}
			} else {
				if (!props.isCheckClusterDown()
						&& clusterDownProbeCount == props
								.getClusterDownMaxCheck()) {
					LOG.fatal("Cluster down check is not enabled. "
							+ "To enable add property. cluster.down.check : true");
					shutdown();
					return;
				}
				clusterDownProbeCount++;
				LOG.warn("No ACTIVE-COORDINATOR found !!! Count: "
						+ clusterDownProbeCount + ", Threshold: "
						+ props.getClusterDownMaxCheck());
				check(clusterDownProbeCount == props.getClusterDownMaxCheck(),
						"Cant find Active-Coordinator. Threshold of "
								+ props.getClusterDownMaxCheck());
			}
			checkMaxActiveServers();
			long i = props.getMailInterval() * 1000 / props.getProbeInterval();
			if (i == 0)
				i = 10;
			if (mail_interval++ % i == 0)
				sendMail("OK");
		}
	}

	/**
	 * Search for active co-ordinator
	 *
	 * @return {@link ServerStat} active coordinator
	 */

	private ServerStat getActiveCoordinator() {
		for (MirrorGroup group : clusterList) {
			ServerStat activeCoordinator = group.getActiveCoordinator();
			if (activeCoordinator != null){
				return activeCoordinator;
			}
		}
		return null;
	}

	private boolean allServerOnline() {
		for (MirrorGroup group : clusterList) {
			if (!group.isAllServerOnline())
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	private void checkMaxActiveServers() {
		for (MirrorGroup group : clusterList) {
			// check for at most one active server per mirror group
			int clusterCheckProbeCount = group.getClusterCheckProbeCount();
			check(clusterCheckProbeCount == props.getOneActiveMaxCheck(),
					String.format("%s health failure: %s active server(s) ",
							group, group.getActiveServerCount()));
		}
	}

	private void checkLowTxnRate(ServerStat serverStat)
			throws NotConnectedException {
		if (!props.isCheckWriteTps())
			return;
		long txr = serverStat.getDsoMbean().getTransactionRate();
		if (txr >= props.getLowTxnThreshold())
			lowTxrProbeCount = 0;
		else {
			lowTxrProbeCount++;
			LOG.warn(String.format("Low-Txn-Rate (%d tps): Curr: %d , MAX: %d",
					txr, lowTxrProbeCount, props.getLowTxnMaxCheck()));
		}
		check(lowTxrProbeCount == props.getLowTxnMaxCheck(),
				"Transaction rate goes below threshold of "
						+ props.getLowTxnThreshold());
	}

	private void checkClients(ServerStat serverStat)
			throws NotConnectedException {
		int connectedClients = serverStat.getDsoMbean().getClients().length;
		long expectedClients = props.getClientCount();
		LOG.info("expected " + expectedClients + " got: " + connectedClients
				+ " clients");
		missingClientsProbeCount = (connectedClients < expectedClients ? (missingClientsProbeCount + 1)
				: 0);
		check(missingClientsProbeCount == props.getMissingClientMaxCheck(),
				"Expecting [" + expectedClients + "] but got ["
						+ connectedClients + "]");
	}

	private void check(boolean condition, String msg) {
		if (condition) {
			LOG.error(msg);
			sendMail(msg);
			for (MirrorGroup mg : clusterList) {
				for (ServerStat st : mg.servers()) {
					try {
						st.dumpClusterState();
					} catch (NotConnectedException e) {
						LOG.debug(e.getMessage());
						LOG.error(e.getLocalizedMessage());
					}
				}
			}
//			LOG.debug("Resetting all probe counts.");
//			lowTxrProbeCount = 0;
//			clusterDownProbeCount = 0;
//			missingClientsProbeCount = 0;
		}
	}

	private void sendMail(String error) {
		if (mail == null)
			return;
		LOG.error("Sending mail...");
		try {
			mail.send("Cluster-watcher report", "Status: " + error + NL + NL
					+ summary());
		} catch (NotConnectedException e) {
			LOG.debug(e.getMessage());
			LOG.error(e.getLocalizedMessage());
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	private String summary() throws NotConnectedException {
		StringBuilder sb = new StringBuilder();
		sb.append(props).append(NL);
		for (MirrorGroup gp : clusterList) {
			sb.append("All Server Online: " + gp.isAllServerOnline())
					.append(NL);
			ServerStat active = gp.getActiveCoordinator();
			if (active == null) {
				sb.append("Active Coordinator: " + false).append(NL);
			} else {
				sb.append("Active Coordinator: " + active).append(NL);
				ObjectName[] clients = active.getDsoMbean().getClients();
				sb.append("Clients Connected: " + clients.length).append(NL);
				sb.append(
						"Txn Rate: "
								+ active.getDsoMbean().getTransactionRate())
						.append(NL);
			}
		}
		sb.append(NL).append(NL);
		sb.append(tcStats.snapshot());
		return sb.toString();
	}

	public static void main(String[] arg) throws Exception {
		String propertyFile = System.getProperty("test.properties",
				"src/main/resources/test.properties");
		ClusterWatcherProperties props = new ClusterWatcherProperties(
				propertyFile);
		ClusterWatcher cw = new ClusterWatcher(props);
		cw.run();
	}
}
