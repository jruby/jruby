package org.jruby.management;

public interface ParserStatsMBean {
    public double getTotalParseTime();
    public double getParseTimePerKB();
    public int getTotalParsedBytes();
    public int getNumberOfEvalParses();
    public int getNumberOfLoadParses();
}
