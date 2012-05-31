package com.terracotta.util;

import org.apache.commons.lang.math.LongRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class Stats {
  private static final Logger log    = LoggerFactory.getLogger(Stats.class);

  private Stats               period = null;
  private AtomicLong          transactionsCount;

  private AtomicLong          totalTxLength;
  private double              minLatency, maxLatency;
  private final Histogram     histo  = new Histogram();

  public Stats() {
    defaultInit();
  }

  public Stats(Stats stat) {
    if (stat != null) {
      init(stat.getTxnCount(), stat.getTotalTxLength(), stat.minLatency, stat.maxLatency);
    } else {
      defaultInit();
    }
  }

  private void defaultInit() {
    init(0, 0, Double.MAX_VALUE, Double.MIN_VALUE);
  }

  private void init(long txnCount, long total, double min, double max) {
    this.transactionsCount = new AtomicLong(txnCount);
    this.totalTxLength = new AtomicLong(total);
    this.minLatency = min;
    this.maxLatency = max;
  }

  public Stats add(Stats stat) {
    this.transactionsCount.addAndGet(stat.getTxnCount());
    this.totalTxLength.addAndGet(stat.getTotalTxLength());
    if (stat.minLatency < this.minLatency) this.minLatency = stat.minLatency;
    if (stat.maxLatency > this.maxLatency) this.maxLatency = stat.maxLatency;
    this.histo.add(stat.histo);
    return this;
  }

  /**
   * Add txLength
   */
  public void add(long txLength) {
    if (txLength > Short.MAX_VALUE) {
      log.warn("stat transaction length exceeds 32 secs, txLength = " + txLength);
    }
    transactionsCount.incrementAndGet();
    totalTxLength.addAndGet(txLength);
    if (txLength < minLatency) minLatency = txLength;
    if (txLength > maxLatency) maxLatency = txLength;
    if (period == null) period = new Stats();
    histo.add(txLength);
    period.transactionsCount.incrementAndGet();
    period.totalTxLength.addAndGet(txLength);
    if (txLength < period.minLatency) period.minLatency = txLength;
    if (txLength > period.maxLatency) period.maxLatency = txLength;
  }

  public double getAverage() {
    if (transactionsCount.get() > 0) return (double) totalTxLength.get() / transactionsCount.get();
    return 0;
  }

  public double getMaxLatency() {
    if (maxLatency == Double.MIN_VALUE) return Double.NaN;
    return maxLatency;
  }

  public double getMinLatency() {
    if (minLatency == Double.MAX_VALUE) return Double.NaN;
    return minLatency;
  }

  public long getTotalTxLength() {
    return totalTxLength.get();
  }

  public void reset() {
    transactionsCount.set(0);
    totalTxLength.set(0);
    minLatency = Double.MAX_VALUE;
    maxLatency = Double.MIN_VALUE;
  }

  public long getTxnCount() {
    return transactionsCount.get();
  }

  public Stats getPeriodStats() {
    Stats p = new Stats(period);
    if (period != null) {
      period.reset();
    }
    return p;
  }

  public Histogram getHisto() {
    return histo;
  }

  @Override
  public String toString() {
    return String.format("Min: %.1f, Max: %.1f, Avg: %.5f \n Histo: %s", getMinLatency(), getMaxLatency(),
                         getAverage(), getHisto());
  }

  private static class Histogram {
    private final AtomicLong       BUCKET_0_10_COUNT      = new AtomicLong();
    private final AtomicLong       BUCKET_10_50_COUNT     = new AtomicLong();
    private final AtomicLong       BUCKET_50_100_COUNT    = new AtomicLong();
    private final AtomicLong       BUCKET_100_200_COUNT   = new AtomicLong();
    private final AtomicLong       BUCKET_200_500_COUNT   = new AtomicLong();
    private final AtomicLong       BUCKET_500_1000_COUNT  = new AtomicLong();
    private final AtomicLong       BUCKET_1000_5000_COUNT = new AtomicLong();
    private final AtomicLong       BUCKET_5000_PLUS_COUNT = new AtomicLong();
    private final AtomicLong       TOTAL                  = new AtomicLong();

    private static final LongRange Range_0_10             = new LongRange(Long.MIN_VALUE, 10);
    private static final LongRange Range_10_50            = new LongRange(10, 50);
    private static final LongRange Range_50_100           = new LongRange(50, 100);
    private static final LongRange Range_100_200          = new LongRange(100, 200);
    private static final LongRange Range_200_500          = new LongRange(200, 500);
    private static final LongRange Range_500_1000         = new LongRange(500, 1000);
    private static final LongRange Range_1000_5000        = new LongRange(1000, 5000);

    public void add(long value) {
      if (Range_0_10.containsLong(value)) {
        BUCKET_0_10_COUNT.incrementAndGet();
      } else if (Range_10_50.containsLong(value)) {
        BUCKET_10_50_COUNT.incrementAndGet();
      } else if (Range_50_100.containsLong(value)) {
        BUCKET_50_100_COUNT.incrementAndGet();
      } else if (Range_100_200.containsLong(value)) {
        BUCKET_100_200_COUNT.incrementAndGet();
      } else if (Range_200_500.containsLong(value)) {
        BUCKET_200_500_COUNT.incrementAndGet();
      } else if (Range_500_1000.containsLong(value)) {
        BUCKET_500_1000_COUNT.incrementAndGet();
      } else if (Range_1000_5000.containsLong(value)) {
        BUCKET_1000_5000_COUNT.incrementAndGet();
      } else {
        BUCKET_5000_PLUS_COUNT.incrementAndGet();
      }
      TOTAL.incrementAndGet();
    }

    public Histogram add(Histogram med) {
      this.BUCKET_0_10_COUNT.addAndGet(med.BUCKET_0_10_COUNT.get());
      this.BUCKET_10_50_COUNT.addAndGet(med.BUCKET_10_50_COUNT.get());
      this.BUCKET_50_100_COUNT.addAndGet(med.BUCKET_50_100_COUNT.get());
      this.BUCKET_100_200_COUNT.addAndGet(med.BUCKET_100_200_COUNT.get());
      this.BUCKET_200_500_COUNT.addAndGet(med.BUCKET_200_500_COUNT.get());
      this.BUCKET_500_1000_COUNT.addAndGet(med.BUCKET_500_1000_COUNT.get());
      this.BUCKET_1000_5000_COUNT.addAndGet(med.BUCKET_1000_5000_COUNT.get());
      this.BUCKET_5000_PLUS_COUNT.addAndGet(med.BUCKET_5000_PLUS_COUNT.get());
      this.TOTAL.addAndGet(med.TOTAL.get());
      return this;
    }

    public double calculatePercentage(AtomicLong value) {
      return (TOTAL.get() > 0) ? ((((double) value.get()) / ((double) TOTAL.get())) * 100.0f) : 0f;

    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("0-10 frequency count = ").append(BUCKET_0_10_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_0_10_COUNT))).append("\n");
      sb.append("10-50 frequency count = ").append(BUCKET_10_50_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_10_50_COUNT))).append("\n");
      sb.append("50-100 frequency count = ").append(BUCKET_50_100_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_50_100_COUNT))).append("\n");
      sb.append("100-200 frequency count = ").append(BUCKET_100_200_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_100_200_COUNT))).append("\n");
      sb.append("200-500 frequency count = ").append(BUCKET_200_500_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_200_500_COUNT))).append("\n");
      sb.append("500-1000 frequency count = ").append(BUCKET_500_1000_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_500_1000_COUNT))).append("\n");
      sb.append("1000-5000 frequency count = ").append(BUCKET_1000_5000_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_1000_5000_COUNT))).append("\n");
      sb.append("5000-PLUS frequency count = ").append(BUCKET_5000_PLUS_COUNT)
          .append(String.format(" percentage = %.1f", calculatePercentage(BUCKET_5000_PLUS_COUNT))).append("\n");
      return sb.toString();
    }
  }
}
