package org.tc.cluster.watcher;

import static org.tc.cluster.watcher.util.ClusterWatcherProperties.LOG;

import java.util.ArrayList;
import java.util.List;

public class MirrorGroup {
	private static final String   ACTIVE_COORDINATOR = "ACTIVE-COORDINATOR";

	private int clusterCheckProbeCount = 0;
	private final List<ServerStat> members	= new ArrayList<ServerStat>();

	public void addMember (ServerStat server){
		members.add(server);
	}

	public List<ServerStat> servers(){
		return members;
	}

	public int getActiveServerCount(){
		int count	= 0;
		for (ServerStat server : members) {
			try {
				if (server.getInfoBean().isActive())
					count++;
			} catch (NotConnectedException e) {
				LOG.error(e.getLocalizedMessage());
				LOG.debug(e.getMessage());
			}
		}
		LOG.debug("No. of Active Server(s) in " + this.toString() + " : "+count);
		clusterCheckProbeCount = ( count > 1 )? (clusterCheckProbeCount + 1) : 0;
		return count;
	}

	public int getClusterCheckProbeCount() {
		return clusterCheckProbeCount;
	}

	public boolean isAllServerOnline(){
		for (ServerStat server : members) {
			if (!server.isConnected())
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	public ServerStat getActiveCoordinator(){
		ServerStat activeCoordinator = null;
		for (ServerStat server : members) {
			try {
				if (ACTIVE_COORDINATOR.equals(server.getInfoBean().getState())) {
					if (activeCoordinator == null)
						activeCoordinator = server;
					else
						LOG.error("Multiple Active Server in a Mirror Group.");
				}
			} catch (NotConnectedException e) {
				LOG.error(e.getLocalizedMessage());
				LOG.debug(e.getMessage());
			}
		}
		return activeCoordinator;
	}

	@Override
	public String toString(){
		StringBuilder str = new StringBuilder().append("Mirror-Group = [ ");
		for (ServerStat server : members) {
			if (server != null)
				str.append(server.host).append(":")
				.append(server.port).append(" ");
		}
		str.append(" ]");
		return str.toString();
	}

}
