package org.tc.cluster.watcher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.util.ClusterWatcherProperties;

public class ClusterWatcherExecutor {
	private static Logger log = Logger.getLogger(ClusterWatcherExecutor.class);
	private ExecutorService clusterWatcherService;

	public void start(ClusterWatcherProperties props) {
		clusterWatcherService = Executors.newSingleThreadExecutor();
		clusterWatcherService.execute(new ClusterWatcher(props));
	}

	public void stop() {
		if (clusterWatcherService != null) {
			try {
				log.info("Trying to shutdown Cluster watcher ...");
				clusterWatcherService.shutdownNow();
				if (clusterWatcherService
						.awaitTermination(10, TimeUnit.SECONDS)) {
					log.info("Cluster watcher shutdown successful.");
					return;
				}
			} catch (InterruptedException e) {
				//
			}
			log.error("Not able to stop Cluster watcher service ... Trying again!");
			stop();
		}
	}

}
