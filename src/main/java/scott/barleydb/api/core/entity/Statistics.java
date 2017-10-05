package scott.barleydb.api.core.entity;

public class Statistics {

  /**
   * number of queries made
   */
  private int numberOfQueries;

  private int numberQueryDatabseCalls;

  private int numberOfBatchInserts;

  private int numberOfBatchUpdates;

  private int numberOfBatchDeletes;

  private int numberOfRecordInserts;

  private int numberOfRecordUpdates;

  private int numberOfRecordDeletes;

  public Statistics(Statistics src) {
    this.numberOfQueries = src.numberOfQueries;
    this.numberQueryDatabseCalls = src.numberQueryDatabseCalls;
    this.numberOfBatchInserts = src.numberOfBatchInserts;
    this.numberOfBatchUpdates = src.numberOfBatchUpdates;
    this.numberOfBatchDeletes = src.numberOfBatchDeletes;
    this.numberOfRecordInserts = src.numberOfRecordInserts;
    this.numberOfRecordUpdates = src.numberOfRecordUpdates;
    this.numberOfRecordDeletes = src.numberOfRecordDeletes;
  }

  public Statistics() {
  }

  public int getNumberOfQueries() {
    return numberOfQueries;
  }

  public void addNumberOfQueries(int numberOfQueries) {
    this.numberOfQueries += numberOfQueries;
  }

  public int getNumberOfBatchInserts() {
    return numberOfBatchInserts;
  }

  public int getNumberOfQueryDatabseCalls() {
    return numberQueryDatabseCalls;
  }

  public void addNumberOfQueryDatabseCalls(int numberQueryDatabseCalls) {
    this.numberQueryDatabseCalls += numberQueryDatabseCalls;
  }

  public void addNumberOfBatchInserts(int numberOfBatchInserts) {
    this.numberOfBatchInserts += numberOfBatchInserts;
  }

  public int getNumberOfBatchUpdates() {
    return numberOfBatchUpdates;
  }

  public void addNumberOfBatchUpdates(int numberOfBatchUpdates) {
    this.numberOfBatchUpdates += numberOfBatchUpdates;
  }

  public int getNumberOfBatchDeletes() {
    return numberOfBatchDeletes;
  }

  public void addNumberOfBatchDeletes(int numberOfBatchDeletes) {
    this.numberOfBatchDeletes += numberOfBatchDeletes;
  }

  public int getNumberOfRecordInserts() {
    return numberOfRecordInserts;
  }

  public void addNumberOfRecordInserts(int numberOfRecordInserts) {
    this.numberOfRecordInserts += numberOfRecordInserts;
  }

  public int getNumberOfRecordUpdates() {
    return numberOfRecordUpdates;
  }

  public void addNumberOfRecordUpdates(int numberOfRecordUpdates) {
    this.numberOfRecordUpdates += numberOfRecordUpdates;
  }

  public int getNumberOfRecordDeletes() {
    return numberOfRecordDeletes;
  }

  public void addNumberOfRecordDeletes(int numberOfRecordDeletes) {
    this.numberOfRecordDeletes += numberOfRecordDeletes;
  }

  public void clear() {
    numberOfBatchDeletes =
        numberOfBatchInserts =
        numberOfBatchUpdates =
        numberOfQueries =
        numberQueryDatabseCalls =
        numberOfRecordDeletes =
        numberOfRecordInserts =
        numberOfRecordUpdates = 0;
  }

}
