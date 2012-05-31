package org.tc.cluster.watcher.notification;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;
import org.tc.cluster.watcher.logger.CsvStatsLogger;

import com.tc.operatorevent.TerracottaOperatorEvent;

public class OperatorEventsNotificationListener implements NotificationListener {

	private static final Logger log = Logger.getLogger("OperatorEvents");
	private final CsvStatsLogger eventLog;

	private static OperatorEventsNotificationListener _instance;

	public static OperatorEventsNotificationListener create(ServerStat server,
			String log) {
		if (_instance == null)
			_instance = new OperatorEventsNotificationListener(server, log);
		return _instance;
	}

	private OperatorEventsNotificationListener(ServerStat server, String file) {
		log.info("Logging operator events to " + file);
		eventLog = new CsvStatsLogger(file);
		try {
			List<TerracottaOperatorEvent> events = server.getDsoMbean()
					.getOperatorEvents();
			List<String> str = new ArrayList<String>();
			for (TerracottaOperatorEvent e : events) {
				str.add(e.toString());
			}
			log.info(str);
			eventLog.header(str.toArray(new String[str.size()]));
		} catch (NotConnectedException e) {
			//
		}
	}

	public void handleNotification(Notification notification, Object handback) {
		eventLog.log(notification.getSource().toString());
		log.info(notification.getSource().toString());
	}

}