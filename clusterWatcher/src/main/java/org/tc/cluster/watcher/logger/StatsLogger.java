package org.tc.cluster.watcher.logger;

import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

public interface StatsLogger {

	public void logStats(ServerStat serverStat) throws NotConnectedException;

	public String snapshot();

	public void close();
}
