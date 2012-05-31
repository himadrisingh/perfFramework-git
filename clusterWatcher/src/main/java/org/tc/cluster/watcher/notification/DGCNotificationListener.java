package org.tc.cluster.watcher.notification;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.logger.CsvStatsLogger;

import com.tc.objectserver.api.GCStats;

public class DGCNotificationListener implements NotificationListener {

	private static final Logger logger = Logger.getLogger("dgcEvents");

	private static final String GC_STATUS_UPDATE 	= "dso.gc.status.update";
	private static final String CLIENT_ATTACHED 	= "dso.client.attached";
	private static final String CLIENT_DETACHED		= "dso.client.detached";
	private static final String GC_COMPLETE      = "COMPLETE";

	private final CsvStatsLogger csv;

	private static DGCNotificationListener _instance;

	public static DGCNotificationListener create(String log){
		if (_instance == null)
		_instance = new DGCNotificationListener(log);
		return _instance;
	}

	private DGCNotificationListener(String log) {
		logger.info("Logging dgc notifications to " + log);
		String[] headers = new String[] { "Iteration", "StartTime",
				"BeginObjectCount", "ActualGarbageCount", "EndObjectCount",
				"MarkStageTime", "PausedStageTime", "ElapsedTime" };
		csv  = new CsvStatsLogger(log);
		csv.header(headers);
	}

	private void checkDGC(Notification notification){
		GCStats stat = (GCStats) notification.getSource();
		if (GC_COMPLETE.equals(stat.getStatus())){
			csv.log(getStats(stat));
		}
	}

	private String[] getStats(GCStats stat){
		List<String> list = new ArrayList<String>();
		list.add(String.valueOf(stat.getIteration()));
		list.add(String.valueOf(stat.getStartTime()));
		list.add(String.valueOf(stat.getBeginObjectCount()));
		list.add(String.valueOf(stat.getActualGarbageCount()));
		list.add(String.valueOf(stat.getEndObjectCount()));
		list.add(String.valueOf(stat.getMarkStageTime()));
		list.add(String.valueOf(stat.getPausedStageTime()));
		list.add(String.valueOf(stat.getElapsedTime()));
		return list.toArray(new String[list.size()]);
	}

	public void handleNotification(Notification notification, Object handback) {
		//		System.out.println(notification.getType());
		if (GC_STATUS_UPDATE.equals(notification.getType())){
			checkDGC(notification);
		}
	}

}
