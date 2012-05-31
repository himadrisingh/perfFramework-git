package com.terracotta;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.apache.log4j.Logger;
import org.springframework.cache.ehcache.TcEhCacheManagerFactoryBean;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;
import org.terracotta.coordination.Barrier;
import org.terracotta.util.ClusteredAtomicLong;

import com.terracotta.cache.CacheStatsProcessor;
import com.terracotta.ehcache.perf.Configuration;
import com.terracotta.ehcache.perf.HotSetConfiguration;
import com.terracotta.ehcache.perf.test.AbstractTest;
import com.terracotta.ehcache.search.SearchExecutor;
import com.terracotta.util.SpringFactory;
import com.terracotta.util.Stats;
import com.terracotta.util.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicLong;

public class EhCachePerfTest {

  // Needed for Destructive tests L1 restarts. Restarted L1s should skip barriers all the time.
  private static final boolean      skipBarrier = Boolean.parseBoolean(System.getProperty("skip.barrier", "false"));

  private static final int          precision = 100;
  private static final Logger       log           = Logger.getLogger(EhCachePerfTest.class);
  private static final String       BARRIER_ROOT  = "BARRIER_ROOT";

  private final Configuration       configuration;
  private final int                 nodeId;
  private final Barrier             barrier;
  private final AbstractTest        test;
  private List<Thread>              threads;
  private long                      testStartTime;
  private long                      lastReportTime;
  private long                      estimatedTestEndTime;
  private long                      actualTestEndTime;

  private final AtomicLong          nonstopCacheExceptionCount = new AtomicLong();
  private volatile boolean          testHasErrors;
  private volatile boolean          testComplete;
  private Thread                    reporterThread;
  private volatile long             readCountAtLastReport, writeCountAtLastReport;

  private long                      bulkLoadCompleteTime;

  private volatile ClusteredAtomicLong clusterReadLatency, clusterWriteLatency, clusterTotalLatency, clusterWarmupLatency;
  private volatile ClusteredAtomicLong clusterReads, clusterWrites, clusterCacheWarmup;

  private final CacheStatsProcessor processor = CacheStatsProcessor.getInstance();

  private final TcEhCacheManagerFactoryBean ehcacheBean;

  public EhCachePerfTest(final Configuration configuration) {
    SpringFactory.getApplicationContext(configuration);
    ehcacheBean = SpringFactory.getControllerBean(configuration, TcEhCacheManagerFactoryBean.class);
    if (ehcacheBean == null) {
      log.warn("Cant find TcEhCacheManagerFactoryBean. Bulk loading WON'T work.");
    }

    this.test = configuration.getTestCase().getTest();
    if (test == null) throw new RuntimeException("Test case is null.");
    log.info("Running test: " + this.test.getClass().getSimpleName());
    test.setDriver(this);
    this.configuration = configuration;
    ClusteringToolkit toolkit = new TerracottaClient(configuration.getExpressTerracottaUrl()).getToolkit();
    this.barrier = toolkit.getBarrier(BARRIER_ROOT, configuration.getNodesNum());
    clusterReads = toolkit.getAtomicLong("ehcacheperf-reads");
    clusterWrites = toolkit.getAtomicLong("ehcacheperf-writes");
    clusterCacheWarmup = toolkit.getAtomicLong("ehcacheperf-warmup");
    clusterWarmupLatency = toolkit.getAtomicLong("ehcacheperf-warmupLatency");
    clusterReadLatency = toolkit.getAtomicLong("ehcacheperf-readLatency");
    clusterWriteLatency = toolkit.getAtomicLong("ehcacheperf-writeLatency");
    clusterTotalLatency = toolkit.getAtomicLong("ehcacheperf-totalLatency");

    // Counter for increasing keys over time
    ClusteredAtomicLong atomicLong = toolkit.getAtomicLong("ehcacheperf-currKeyCount");
    atomicLong.set(configuration.getElementNum());
    test.setCurrKeyCount(atomicLong);
   this.nodeId = await();
    test.setNodeId(nodeId);
    test.setNumberOfNodes(configuration.getNodesNum());
  }

  public Barrier getBarrier() {
    return barrier;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  private void setBulkLoad(boolean bulkLoad){
    if(configuration.getCacheType().equals("ehcache") && ehcacheBean != null)
      ehcacheBean.setBulkLoad(bulkLoad);
  }

  private void waitUntilBulkLoadComplete(){
    if(configuration.getCacheType().equals("ehcache") && ehcacheBean != null){
      ehcacheBean.setBulkLoad(false);
      log.info("Waiting for all nodes to be coherent.");
      ehcacheBean.waitUntilBulkLoadComplete();
    }
  }

  private void runTest() {
    log.info("Welcome on node #" + nodeId + ", total nodes: " + configuration.getNodesNum());
    log.info("Starting warmup phase");
    try {
      log.info("Starting L2 Warmup phase.");
      log.info("Loading cache: bulk load enabled : " + configuration.isBulkLoadEnabled());
      setBulkLoad(configuration.isBulkLoadEnabled());

      long start = now();
      test.doL2WarmUp();
      long end = now();

      test.flushAllPutsToServer();
      long endTimeServerFlush = now();

      long time = (end - start) / 1000;
      long realL2WarmupTime = (endTimeServerFlush - start) / 1000;

      time = (time == 0) ? 1 : time;
      realL2WarmupTime = (time == 0) ? 1 : realL2WarmupTime;
      test.processCacheStats();
      long warmup = processor.getWrite();
      Stats warmupStats = processor.getWriteStat();
      log.info(String.format("Cache Warmup: %d puts, %d seconds, %.1f puts/sec", warmup, time, warmup * 1.0 / time));
      log.info(String.format("Warmup time ensuring all entries have been sent to server: %d seconds", realL2WarmupTime));
      log.info("Cache Warmup Latency: " + warmupStats.toString());
      clusterCacheWarmup.addAndGet(warmup / time);
      clusterWarmupLatency.addAndGet((long)(warmupStats.getAverage() * precision));
      test.resetCacheStats();

      log.info("Waiting for all nodes to complete L2 warmup.");
      await();
      start = now();
      waitUntilBulkLoadComplete();
      bulkLoadCompleteTime = now() - start;
      await();
      if (configuration.isL1Enabled()) {
        log.info("Starting L1 Warmup phase.");
        setBulkLoad(configuration.isBulkLoadEnabled());
        test.doL1WarmUp();
        waitUntilBulkLoadComplete();

        test.processCacheStats();
        warmup = processor.getWrite();
        warmupStats = processor.getWriteStat();
        log.info(String.format("Cache L1 Warmup: %d puts, %d seconds, %.1f puts/sec", warmup, time, warmup * 1.0 / time));
        log.info("Cache L1 Warmup Latency: " + warmupStats.toString());
        test.resetCacheStats();
       }
    } catch (Exception e) {
      log.fatal("Error during warm up phase!", e);
      System.exit(-1);
    }
    log.info("Warmup phase done... Waiting for the other nodes to be ready");
    await();
    log.info("Starting the actual test phase now!");
    if (configuration.isLogMisses()) {
      test.logMisses(true);
    }
    test.beforeTest();

    if (configuration.isSearchEnabled()){
      SearchExecutor exec = new SearchExecutor(configuration, ehcacheBean);
      exec.run();
    }

    testStartTime = now();
    lastReportTime = now();
    estimatedTestEndTime = testStartTime + (configuration.getTestDuration() * 1000);

    this.threads = new ArrayList<Thread>(configuration.getThreadNum());
    for (int i = 0; i < configuration.getThreadNum(); i++) {
      threads.add(new Thread("PerfAppThread-" + i) {
        @Override
        public void run() {
          test.beforeTestForEachAppThread();
          while (isTestRunning()) {
            try {
              test.doTestBody();
            }
            catch (NonStopCacheException ne){
              nonstopCacheExceptionCount.incrementAndGet();
            }
            catch (Exception e) {
              log.error("error in test", e);
              testHasErrors = true;
              System.exit(-1);
            }
          }
          test.afterTestForEachAppThread();
        }
      });
    }

    if(configuration.isAddOwnersEnabled()){
      log.info("Starting add-owners thread. " + getConfiguration().getAddOwnersCount()
               + " new owners will be added every "
               + getConfiguration().getAddOwnersPeriodInSecs()
               + " secs.");
      new Thread() {
        @Override
        public void run() {
          while (isTestRunning()) {
            try {
              sleep(getConfiguration().getAddOwnersPeriodInSecs() * 1000);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            test.addNewOwners(getConfiguration().getAddOwnersCount());
          }
        }
      }.start();
    }else
      log.info("Not adding new owners.....");

    for (Thread thread : threads) {
      thread.start();
    }
    startReporterThread();
    waitForTestThreads();
    waitForReporterThread();
    doFinalReport();
  }

  private boolean isTestRunning(){
    return (!testComplete && now() < estimatedTestEndTime) || configuration.getTestDuration() < 0;
  }

  private void waitForTestThreads() {
    // wait for all threads to complete
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    test.testComplete();
    this.actualTestEndTime = now();
  }

  public void completeTest() {
    this.testComplete = true;
  }

  private void waitForReporterThread() {
    // wait for the reporter thread to exit and then do the final report
    try {
      reporterThread.interrupt();
      reporterThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void startReporterThread() {
    this.reporterThread = new Thread() {
      @Override
      public void run() {
        while (isTestRunning()) {
          try {
            sleep(configuration.getReportInterval() * 1000);
          } catch (InterruptedException e) {
            // ignored
          }
          doPeriodReport();
        }
      }
    };
    reporterThread.start();
  }

  private void doPeriodReport() {
    doPeriodReport(true);
  }

  private void doPeriodReport(final boolean period) {
    if (period && !isTestRunning()) return;

    long now = now();
    if (period) {
      log.info("");
      log.info("------------------ Cache Stats -----------------------");
      if (configuration.getTestDuration() < 0) {
        log.info("Test running for " + Util.formatTimeInSecondsToWords((now - testStartTime) / 1000));
      } else {
        log.info("Remaining Time: " + Util.formatTimeInSecondsToWords((estimatedTestEndTime - now) / 1000));
      }
    }
    test.processCacheStats();

    // Cache detailed stats
    Stats readStat = processor.getReadStat();
    Stats writeStat = processor.getWriteStat();

    long readCount = processor.getRead();
    long writeCount = processor.getWrite();
    long total = readCount + writeCount;

    if (period){
      /* *******************************************************
       * Node Periodic Cache Stats
       * *******************************************************/

      long periodReadCount = readCount - readCountAtLastReport;
      long periodWriteCount = writeCount - writeCountAtLastReport;
      long periodTotalCount = (total) - (readCountAtLastReport + writeCountAtLastReport);

      log.info(String.format("Cache: Period Read iterations/sec =  %.1f, completed = %d ", 1000.0 * periodReadCount
                             / (now - lastReportTime),
                             periodReadCount));
      log.info(String.format("Cache: Period Write iterations/sec =  %.1f, completed = %d ", 1000.0 * periodWriteCount
                             / (now - lastReportTime),
                             periodWriteCount));
      log.info(String.format("Cache: Period iterations/sec =  %.1f, completed = %d ", 1000.0 * periodTotalCount
                             / (now - lastReportTime),
                             periodTotalCount));

      readCountAtLastReport = readCount;
      writeCountAtLastReport = writeCount;
    }

    /* *******************************************************
     * Cumulative Cache stats
     * *******************************************************/
    log.info(String.format("Cache: Cumulative Read iterations/sec =  %.1f, completed = %d", 1000.0 * readCount / (now - testStartTime),
                           readCount));
    log.info(String.format("Cache: Cumulative Write iterations/sec =  %.1f, completed = %d", 1000.0 * writeCount / (now - testStartTime),
                           writeCount));
    log.info(String.format("Cache: Cumulative Total iterations/sec =  %.1f, completed = %d", 1000.0 * total / (now - testStartTime),
                           total));
    log.info("Cache: Cumulative Read latency: " + readStat);
    log.info("Cache: Cumulative Write latency: " + writeStat);

    processor.reset();
    if (testHasErrors) {
      log.error("Node: Test has errors. NonstopCacheException: " + nonstopCacheExceptionCount.get());
    }
    lastReportTime = now;
  }

  public static void main(String[] args) throws Exception {
    log.info("EhCache Performance Test Application");
    if (args.length != 1) {
      log.fatal("You need to provide a valid properties file as argument");
      System.exit(-1);
    }

    log.info("##########################################\n");
    log.info("Test info: ");
    log.info("1. Test has 6 caches: owners, pets, visits, petTypes, ownerPets, petVisits. 1 owner = 10 objects.");
    log.info("2. owners Cache stores Owners object.  This has valuePadding.");
    log.info("3. pets Cache saves Pets objects. Each owner has 2 Pets i.e. 1 Owner = 2 Pets. valuePadding DOESNT apply to this. ");
    log.info("4. visits cache stores visits for pets. Each Pet has 2 Visits i.e. 1 Owner = 4 Visits. This has valuePadding.");
    log.info("5. petVisits stores list of ids of visits. Each pet will result in 1 petVisits element i.e. 1 owner = 2 petVisits. valuePadding DOESNT apply to this.");
    log.info("6. ownerPets store ids for Pets. Each owner will result in 1 ownerPets element i.e. 1 Owner = 1 ownerPets. valuePadding DOESNT apply to this.");
    log.info("7. There are 6 petTypes. The total elements should be 6 only. valuePadding DOESNT apply to this.\n");
    log.info("##########################################\n");

    String propertiesLine = args[0];
    log.info("Property file location: " + propertiesLine);
    Properties props = loadProperties(propertiesLine);
    log.info("Properties: " + props);
    Configuration conf = new Configuration(props);

    // changes for hot data set
    conf.setHotSetConfiguration(HotSetConfiguration.getHotSetConfig(conf, props));

    log.info("Running test case " + conf.getTestCase());
    log.info("\n" + conf);
    new EhCachePerfTest(conf).runTest();
    log.info("Done with test case " + conf.getTestCase() + ". Hoping to see you again soon...");
    System.exit(0);
  }

  public void doFinalReport() {
    await();
    doPeriodReport(false);
    test.processCacheStats();
    Stats read = processor.getReadStat();
    Stats write = processor.getWriteStat();
    Stats total = new Stats();
    total.add(read);
    total.add(write);

    clusterReads.addAndGet(processor.getRead());
    clusterWrites.addAndGet(processor.getWrite());

    clusterReadLatency.addAndGet((long) (read.getAverage() * precision));
    clusterWriteLatency.addAndGet((long) (write.getAverage() * precision));
    clusterTotalLatency.addAndGet((long) (total.getAverage() * precision));

    await();

    long totalRead = clusterReads.get();
    long totalWrite = clusterWrites.get();

    log.info(total);
    long testDuration = (actualTestEndTime - testStartTime) / 1000;
    log.info("------- FINAL REPORT -------- ");
    log.info("Read Latency (ms): " + read);
    log.info("Write Latency (ms): " + write);
    log.info("Total Latency (ms): " + total);
    log.info("------- Cluster Cache Report -------- ");
    log.info(String.format("Read TPS: %.1f", (double) totalRead / testDuration));
    log.info(String.format("Write TPS: %.1f", (double) totalWrite / testDuration));
    log.info(String.format("Total TPS: %.1f", (double) (totalRead + totalWrite) / testDuration));
    log.info(String.format("Warmup TPS: %d", clusterCacheWarmup.get()));
    log.info("");
    log.info(String.format("Read Avg Latency (ms): %.2f " , (double) clusterReadLatency.get() / (configuration.getNodesNum() * precision )));
    log.info(String.format("Write Avg Latency (ms): %.2f " , (double) clusterWriteLatency.get() / (configuration.getNodesNum() * precision )));
    log.info(String.format("Total Avg Latency (ms): %.2f " , (double) clusterTotalLatency.get() / (configuration.getNodesNum() * precision )));
    log.info(String.format("Warmup Avg Latency (ms): %.2f " , (double) clusterWarmupLatency.get() / (configuration.getNodesNum() * precision )));

    log.info("");
    log.info(String.format("Time taken for clusterCoherent (ms): %d", bulkLoadCompleteTime));

    long exceptions = nonstopCacheExceptionCount.get();
    if (exceptions > 0){
      log.info(String.format("Node NonstopCache Exception Count: %d ", exceptions));
    }
  }

  private static Properties loadProperties(final String location) {
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return props;
  }

  protected static long now() {
    return System.currentTimeMillis();
  }

  private int await(){
    if (skipBarrier){
      log.warn("Skipping barriers.....");
      return configuration.getNodesNum();
    }

    int parties = -1;
    try {
      parties = barrier.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    }
    return parties;
  }

  public long getTestElapsedTimeSeconds() {
    return (now() - testStartTime) / 1000;
  }
}

