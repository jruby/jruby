package org.jruby.management;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.jruby.Ruby;
import org.jruby.parser.ParserManager;
import org.jruby.parser.ParserProvider;
import org.jruby.util.cli.Options;

public class ParserStats implements ParserStatsMBean {
    private final AtomicLong totalIRBuildTime = new AtomicLong(0); // nanos
    private final AtomicLong totalParseTime = new AtomicLong(0); // nanos
    private final AtomicLong totalPrismDeserializingTime = new AtomicLong(0); // nanos
    private final AtomicLong totalPrismCParseSerializingTime = new AtomicLong(0); // nanos
    private final AtomicLong totalPrismSerializedBytes = new AtomicLong(0);
    private final AtomicLong totalParsedBytes = new AtomicLong(0);
    private final AtomicInteger totalEvalParses = new AtomicInteger(0);
    private final AtomicInteger totalLoadParses = new AtomicInteger(0);
    private final AtomicInteger totalJRubyModuleParses = new AtomicInteger(0);

    // FIXME: should we really report this per runtime to mbeans?
    public ParserStats(Ruby ruby) {
    }

    public void addParseTime(long time) {
        totalParseTime.addAndGet(time);
    }

    public void addParsedBytes(int bytes) {
        totalParsedBytes.addAndGet(bytes);
    }

    public void addEvalParse() {
        totalEvalParses.incrementAndGet();
    }

    public void addLoadParse() {
        totalLoadParses.incrementAndGet();
    }

    public void addJRubyModuleParse() {
        totalJRubyModuleParses.incrementAndGet();
    }

    public double getTotalParseTime() {
        return totalParseTime.get() / 1_000_000_000.0;
    }

    public long getTotalParsedBytes() {
        return totalParsedBytes.get();
    }

    public long getTotalPrismSerializedBytes() {
        return totalPrismSerializedBytes.get();
    }

    public double getPrismCParseSerializeTime() {
        return totalPrismCParseSerializingTime.get() / 1_000_000_000.0;
    }

    public double getPrismDeserializingTime() {
        return totalPrismDeserializingTime.get() / 1_000_000_000.0;
    }

    public double getParseTimePerKB() {
        long totalBytes = getTotalParsedBytes();
        if (totalBytes == 0) return 0.0;
        return getTotalParseTime() / (totalBytes / 1_000.0);
    }

    public int getNumberOfEvalParses() {
        return totalEvalParses.get();
    }

    public int getNumberOfLoadParses() {
        return totalLoadParses.get();
    }

    public void addPrismTimeDeserializing(long time) {
        totalPrismDeserializingTime.addAndGet(time);
    }

    public void addPrismSerializedBytes(int length) {
        totalPrismSerializedBytes.addAndGet(length);
    }

    public void addPrismTimeCParseSerialize(long time) {
        totalPrismCParseSerializingTime.addAndGet(time);
    }

    public void printParserStatistics(Ruby runtime) {
        System.err.println("--------------------------------------------------------------------------------");
        System.err.println("Parser Statistics:");
        System.err.println("  Generic:");
        System.err.println("    parser type: " + runtime.getParserManager().getParser().getClass());
        System.err.println("    bytes processed: " + getTotalParsedBytes());
        System.err.println("    files parsed: " + getNumberOfLoadParses());
        System.err.println("    evals parsed: " + getNumberOfEvalParses());
        System.err.println("    time spent parsing(s): " + getTotalParseTime());
        System.err.println("    time spend parsing + building: " + (getTotalParseTime() + getIRBuildTime()));
        if (Options.PARSER_PRISM.load()) {
            System.err.println("  Prism:");
            System.err.println("    time C parse+serialize: " + getPrismCParseSerializeTime());
            System.err.println("    time deserializing: " + getPrismDeserializingTime());
            System.err.println("    serialized bytes: " + getTotalPrismSerializedBytes());
            System.err.println("    serialized to source ratio: x" +
                    ((float) getTotalPrismSerializedBytes() / getTotalParsedBytes()));
        }
        System.err.println("  IRBuild:");
        System.err.println("    build time: " + getIRBuildTime());
    }

    private double getIRBuildTime() {
        return totalIRBuildTime.get() / 1_000_000_000.0;
    }

    public void addIRBuildTime(long time) {
        totalIRBuildTime.addAndGet(time);
    }
}
