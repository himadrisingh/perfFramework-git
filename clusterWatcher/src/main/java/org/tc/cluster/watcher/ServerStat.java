package org.tc.cluster.watcher;

import static org.tc.cluster.watcher.util.ClusterWatcherProperties.LOG;

import java.io.IOException;
import java.net.ConnectException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.tc.cluster.watcher.notification.DGCNotificationListener;
import org.tc.cluster.watcher.notification.OperatorEventsNotificationListener;
import org.tc.cluster.watcher.util.JMXConnectorProxy;

import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.stats.api.DSOMBean;

public class ServerStat {

	private static final String SERVER_MBEAN_NAME = "org.terracotta.internal:type=Terracotta Server,name=Terracotta Server";
	private static final String DSO_MBEAN_NAME = "org.terracotta:type=Terracotta Server,name=DSO";
	private static final String L2_DUMPER = "org.terracotta.internal:type=Terracotta Server,name=L2Dumper";
	private static final String OPS_EVENT = "org.terracotta:type=TC Operator Events,name=Terracotta Operator Events Bean";

	public String host;
	public int port;
	private JMXConnectorProxy jmxProxy;
	private MBeanServerConnection mbsc;

	private DSOMBean dsoMBean;
	private TCServerInfoMBean infoMBean;
	private L2DumperMBean l2DumperMBean;
	private NotificationListener ops, dgc;
	private ObjectName dsoObjectName, eventListenerObjectName;

	public ServerStat(String host, int port) {
		if (host.equals("%i"))
			host = "localhost";
		this.host = host;
		this.port = port;
		try {
			init();
		} catch (NotConnectedException e) {
			LOG.error(e.getMessage());
		}
	}

	private synchronized void init() throws NotConnectedException {
		if (jmxProxy != null) {
			try {
				jmxProxy.close();
			} catch (IOException e) {
				// ignore
			}
		}
		jmxProxy = new JMXConnectorProxy(host, port);
		try {
			mbsc = jmxProxy.getMBeanServerConnection();
			ObjectName serverObjectName = new ObjectName(SERVER_MBEAN_NAME);
			dsoObjectName = new ObjectName(DSO_MBEAN_NAME);
			eventListenerObjectName = new ObjectName(OPS_EVENT);

			ObjectName l2dumperObjectName = new ObjectName(L2_DUMPER);

			dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc,
					dsoObjectName, DSOMBean.class, false);

			infoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc,
					serverObjectName, TCServerInfoMBean.class, false);

			l2DumperMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc,
					l2dumperObjectName, L2DumperMBean.class, false);
		} catch (ConnectException ce) {
			LOG.warn(this + " : Connection Refused. Probably server crashed.");
			throw new NotConnectedException(host + ":" + port, ce);
		} catch (Exception e) {
			throw new NotConnectedException(host + ":" + port, e);
		}
	}

	public boolean isConnected() {
		try {
			mbsc.getDefaultDomain();
		} catch (Exception e) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	public TCServerInfoMBean getInfoBean() throws NotConnectedException {
		if (!isConnected()) {
			init();
		}
		return infoMBean;
	}

	public DSOMBean getDsoMbean() throws NotConnectedException {
		if (!isConnected()) {
			init();
		}
		return dsoMBean;
	}

	public void dumpClusterState() throws NotConnectedException {
		if (!isConnected()) {
			init();
		}
		LOG.info("Taking state dump for " + this);
		l2DumperMBean.dumpClusterState();
	}

	public void registerDgcNotificationListener(DGCNotificationListener dgc) {
		try {
			mbsc.addNotificationListener(dsoObjectName, dgc, null, null);
			LOG.info("Added DSO Notification listener...");
		} catch (Exception e) {
			LOG.error("Error in registering DSO Notification Listener : "
					+ e.getMessage());
		}
	}

	public void registerEventNotificationListener(OperatorEventsNotificationListener ops) {
		try {
			mbsc.addNotificationListener(eventListenerObjectName, ops, null,
					null);
			LOG.info("Added Event Nofitication Listener...");
		} catch (InstanceNotFoundException ne) {
			LOG.error("Error in registering Event Notification Listener. Are u running OS kit? : "
					+ ne.getMessage());
			ops = null;
		} catch (Exception e) {
			LOG.error("Not able to register Event Notification Listener.", e);
			ops = null;
		}
	}

	public void shutdown() {
		try {
			dumpClusterState();
		} catch (NotConnectedException e1) {
			LOG.error("Shutdown: Not able to take state dump for " + this);
		}
		try {
			if (dgc != null) {
				mbsc.removeNotificationListener(dsoObjectName, dgc);
				LOG.info("Successfully unregistered DGC Notification Listener.");
			}
		} catch (Exception e) {
			LOG.error("Not able to unregister DGC Notification Listener.");
		}

		try {
			if (ops != null) {
				mbsc.removeNotificationListener(eventListenerObjectName, ops);
				LOG.info("Successfully unregistered Event Notification Listener.");
			}
		} catch (Exception e) {
			LOG.error("Not able to register Event Notification Listener.");
		}

		if (mbsc != null) {
			try {
				ObjectName serverObjectName = new ObjectName(SERVER_MBEAN_NAME);
				ObjectName l2dumperObjectName = new ObjectName(L2_DUMPER);

				mbsc.unregisterMBean(l2dumperObjectName);
				mbsc.unregisterMBean(dsoObjectName);
				mbsc.unregisterMBean(serverObjectName);
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

	}

	@Override
	public String toString() {
		return "[" + host + ":" + port + "]";
	}

	public static void main(String[] a) throws Exception {
		ServerStat stat = new ServerStat("localhost", 9520);
		stat.l2DumperMBean.doThreadDump();
	}
}
