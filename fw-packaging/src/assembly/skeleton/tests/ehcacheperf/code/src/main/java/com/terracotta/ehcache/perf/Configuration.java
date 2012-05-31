package com.terracotta.ehcache.perf;

import org.apache.log4j.Logger;

import com.terracotta.ehcache.perf.test.TestCase;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex Snaps
 */
public class Configuration {

  private static final Logger log = Logger.getLogger(Configuration.class);

  private final boolean    standalone;
  private final int        nodesNum;
  private final int        threadNum;
  private final Properties props;
  private final String     expressTerracottaUrl;
  private final TestCase   testCase;
  private final long       testDuration;
  private final int        reportInterval;
  private final String     cacheType;
  private final String     environmentType;
  private final int        writePercentage;
  private final boolean    logMisses;
  private final boolean    l1Enabled;
  private final boolean    jtaEnabled;
  private final boolean    searchEnabled;
  private final boolean    bulkLoadEnabled;
  private final String     transactionManager;
  private final boolean    noDB;

  private final int       addOwnersPeriodInSecs;
  private final int       addOwnersCount;
  private final boolean   addOwnersEnabled;

  private HotSetConfiguration       hotSetConfiguration;

  private final int elementNum;

  public Configuration(Properties props) {
    this.props = props;
    this.standalone = getBoolean("standalone", true);
    this.noDB = getBoolean("noDB", true);
    this.nodesNum = getInteger("numOfNodes", 1);
    this.threadNum = getInteger("numOfThreads", 5);
    this.expressTerracottaUrl = getString("expressTerracottaUrl", "localhost:9510").trim();
    this.testCase = TestCase.valueOf(getString("testCase", "readOnlyTest"));
    this.testDuration = getLong("duration", 60);
    this.reportInterval = getInteger("reportInterval", 5);
    this.cacheType = getString("cache", "ehcache");
    this.environmentType = getString("env", "local");
    this.writePercentage = getInteger("readwrite.write.percentage", 2);
    this.logMisses = getBoolean("logMisses", false);
    this.l1Enabled = getBoolean("l1WarmupEnabled", true);
    this.jtaEnabled = getBoolean("jtaEnabled", false);
    this.bulkLoadEnabled = getBoolean("bulkLoad.enabled", true);
    this.transactionManager = getString("transactionManager", "btm");
    this.elementNum = getInteger("elementNum", 1000);

    this.addOwnersCount = getInteger("addOwnersCount", 10);
    this.addOwnersEnabled = getBoolean("addOwners.enabled", false);
    this.addOwnersPeriodInSecs = getInteger("addOwnersPeriodInSecs", 20);

    this.searchEnabled = getBoolean("search.enabled", false);
    com.terracotta.ehcache.perf.FakeWriteBehindFactory.millisToSleep = TimeUnit.SECONDS.toMillis(getInteger("writer.maxWriteDelay", 0)) / 2;

    getBoolean("ehcache.clustered" , true);
  }

  public int getElementNum() {
    return elementNum;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return Boolean.valueOf(getString(key, String.valueOf(defaultValue)));
  }

  public long getLong(String key, long defaultValue) {
    return Long.valueOf(getString(key, String.valueOf(defaultValue)));
  }

  public int getInteger(String key, int defaultValue) {
    return Integer.valueOf(getString(key, String.valueOf(defaultValue)));
  }

  public String getString(String key, String defaultValue) {
    String value = props.getProperty(key);
    if (value == null){
      log.warn("Key not found in Properties: " + key + " , Using defaults: " + defaultValue);
      props.setProperty(key, defaultValue);
      return defaultValue;
    }
    return value.trim();
  }

  public boolean isStandalone() {
    return standalone;
  }

  public boolean isNoDB() {
    return noDB;
  }

  public int getNodesNum() {
    return nodesNum;
  }

  public boolean isBulkLoadEnabled() {
    return bulkLoadEnabled;
  }

  public Properties getProperties() {
    return props;
  }

  public String getExpressTerracottaUrl() {
    return expressTerracottaUrl;
  }

  public TestCase getTestCase() {
    return testCase;
  }

  public int getThreadNum() {
    return threadNum;
  }

  public long getTestDuration() {
    return testDuration;
  }

  public int getReportInterval() {
    return reportInterval;
  }

  public String getCacheType() {
    return cacheType;
  }

  public String getEnvironmentType() {
    return environmentType;
  }

  public int getWritePercentage() {
    return writePercentage;
  }

  public boolean isLogMisses() {
    return logMisses;
  }

  public boolean isL1Enabled() {
    return l1Enabled;
  }

  public boolean isJtaEnabled() {
    return jtaEnabled;
  }

  public String getTransactionManager() {
    return transactionManager;
  }

  public HotSetConfiguration getHotSetConfiguration() {
    return hotSetConfiguration;
  }

  public void setHotSetConfiguration(HotSetConfiguration hotSetConfiguration) {
    this.hotSetConfiguration = hotSetConfiguration;
  }

  public int getAddOwnersPeriodInSecs() {
    return addOwnersPeriodInSecs;
  }

  public int getAddOwnersCount() {
    return addOwnersCount;
  }

  public boolean isAddOwnersEnabled() {
    return addOwnersEnabled;
  }

  @Override
  public String toString() {

    return new StringBuilder("Configuration = {")
    .append("\n  standalone \t\t= ").append(standalone)
    .append("\n  nodesNum \t\t= ").append(nodesNum)
    .append("\n  threadNum \t\t= ").append(threadNum)
    .append("\n  elementNum \t\t= ").append(elementNum)
    .append("\n  expressTerracottaUrl \t\t= ").append(expressTerracottaUrl)
    .append("\n  testCase \t\t= ").append(testCase)
    .append("\n  testDuration \t\t= ").append(testDuration)
    .append("\n  reportInterval \t= ").append(reportInterval)
    .append("\n  cacheType \t\t= ").append(cacheType)
    .append("\n  environmentType \t= ").append(environmentType)
    .append("\n  writePercentage \t= ").append(writePercentage)
    .append("\n  l1Enabled \t= ").append(l1Enabled)
    .append("\n  jtaEnabled \t= ").append(jtaEnabled)
    .append("\n  bulkLoad \t= ").append(bulkLoadEnabled)
    .append("\n  transactionManager \t= ").append(transactionManager)
    .append("\n  noDB \t= ").append(noDB)
    .append("\n}").append(hotSetConfiguration).toString();

  }

  public boolean isSearchEnabled() {
    return this.searchEnabled;
  }
}

