package org.jruby.management;

public interface ParserStatsMBean {
    public double getTotalParseTime();
    public double getParseTimePerKB();
    public long getTotalParsedBytes();
    public int getNumberOfEvalParses();
    public int getNumberOfLoadParses();
}
