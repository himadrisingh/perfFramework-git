package org.tc.perf.util;

import static org.tc.perf.util.Utils.HOSTNAME;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.DiskUsage;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;

/**
 * It collects the system stats using {@link Sigar}
 *
 * Collects following stats into a CSV file. <li>Cpu Sys(%)</li> <li>Cpu Usr(%)</li> <li>Cpu Total(%)</li> <li>
 * Mem Free(MB)</li> <li>Disk Read(B/s)</li> <li>Disk Write(B/s)</li> <li>Page
 * In(MB)</li> <li>PageOut(MB)</li> <li>TCP In</li> <li>TCP Out</li>
 *
 * @author Himadri Singh
 */
public class SystemStatsCollector {

	private static final int interval = 4 * 1000;
	private static final Logger log = Logger
			.getLogger(SystemStatsCollector.class);
	private static final DateFormat df = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.MEDIUM);
	private static final NumberFormat nf = NumberFormat.getInstance();
	private static String userDir = System.getProperty("user.dir");

	private final Sigar sigar;
	private long prevDiskRead, prevDiskWrite;
	private Thread t;

	private static class SystemStatsCollectorHolder {
		private static SystemStatsCollector instance = new SystemStatsCollector();
	}

	/**
	 *
	 * @return singleton instance
	 */
	public static SystemStatsCollector getInstance() {
		return SystemStatsCollectorHolder.instance;
	}

	private SystemStatsCollector() {
		sigar = new Sigar();
		try {
			for (FileSystem fs : sigar.getFileSystemList())
				log.info("FileSystem: " + fs);

			userDir = sigar.getFileSystemList()[0].toString();
			log.info("Monitoring disk usage for " + userDir);

			DiskUsage disk = sigar.getDiskUsage(userDir);
			prevDiskRead = disk.getReadBytes();
			prevDiskWrite = disk.getWriteBytes();
		} catch (SigarException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update system stats log dir. Helpful for diff client/server log
	 * locations.
	 *
	 * @param logDir
	 */
	public void updateLogLocation(File logDir) {
		try {
			File logs = new File(logDir, HOSTNAME + "-system.stats.csv");
			Layout layout = new PatternLayout("%m%n");
			FileAppender fap = new RollingFileAppender(layout,
					logs.getAbsolutePath());
			fap.setAppend(false);
			log.removeAllAppenders();
			log.setAdditivity(false);
			log.setLevel(Level.DEBUG);
			log.addAppender(fap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		t = new Thread(new Runnable() {

			public void run() {
				try {
					runThread();
				} catch (SigarException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	public void stop() {
		if (t != null) {
			while (t.isAlive())
				t.interrupt();
			t = null;
		}
	}

	private void runThread() throws SigarException {
		log.debug(String.format(
				"\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\","
						+ "\"%s\",\"%s\",\"%s\",\"%s\"", "Time", "Cpu Sys(%)",
				"Cpu Usr(%)", "Cpu Total(%)", "Mem Free(MB)", "Disk Read(B/s)",
				"Disk Write(B/s)", "Page In(MB)", "PageOut(MB)", "TCP In",
				"TCP Out"));

		while (true) {
			DiskUsage disk = sigar.getDiskUsage(userDir);
			Mem mem = sigar.getMem();
			NetStat net = sigar.getNetStat();
			CpuPerc cpu = sigar.getCpuPerc();
			Swap swap = sigar.getSwap();

			long diskRead = disk.getReadBytes();
			long diskWrite = disk.getWriteBytes();
			long swapIn = swap.getPageIn();
			long swapOut = swap.getPageOut();
			log.debug(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\","
					+ "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"", df
					.format(new Date()), nf.format(cpu.getSys() * 100), nf
					.format(cpu.getUser() * 100), nf.format((cpu.getSys()
					+ cpu.getWait() + cpu.getUser()) * 100), Sigar
					.formatSize(mem.getFree()), nf.format(diskRead
					- prevDiskRead), nf.format(diskWrite - prevDiskWrite),
					Sigar.formatSize(swapIn), Sigar.formatSize(swapOut), net
							.getTcpInboundTotal(), net.getTcpOutboundTotal()));
			prevDiskRead = diskRead;
			prevDiskWrite = diskWrite;
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				log.debug("System stats collection stopped.");
				return;
			}
		}
	}
}
