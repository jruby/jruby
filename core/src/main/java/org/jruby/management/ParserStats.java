package org.jruby.management;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.jruby.Ruby;
import org.jruby.parser.ParserManager;
import org.jruby.util.cli.Options;

public class ParserStats implements ParserStatsMBean {
    private final AtomicLong totalIRBuildTime = new AtomicLong(0); // nanos
    private final AtomicLong totalParseTime = new AtomicLong(0); // nanos
    private final AtomicLong totalYARPDeserializingTime = new AtomicLong(0); // nanos
    private final AtomicLong totalYARPCParseSerializingTime = new AtomicLong(0); // nanos
    private final AtomicLong totalYARPSerializedBytes = new AtomicLong(0);
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

    public long getTotalYARPSerializedBytes() {
        return totalYARPSerializedBytes.get();
    }

    public double getYARPCParseSerializeTime() {
        return totalYARPCParseSerializingTime.get() / 1_000_000_000.0;
    }

    public double getYARPDeserializingTime() {
        return totalYARPDeserializingTime.get() / 1_000_000_000.0;
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

    public void addYARPTimeDeserializing(long time) {
        totalYARPDeserializingTime.addAndGet(time);
    }

    public void addYARPSerializedBytes(int length) {
        totalYARPSerializedBytes.addAndGet(length);
    }

    public void addYARPTimeCParseSerialize(long time) {
        totalYARPCParseSerializingTime.addAndGet(time);
    }

    public void printParserStatistics() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Parser Statistics:");
        System.out.println("  Generic:");
        System.out.println("    parser type: " + (ParserManager.PARSER_WASM ? "Prism(wasm)" :
                (Options.PARSER_PRISM.load() ? "Prism(C)" : "Legacy")));
        System.out.println("    bytes processed: " + getTotalParsedBytes());
        System.out.println("    files parsed: " + getNumberOfLoadParses());
        System.out.println("    evals parsed: " + getNumberOfEvalParses());
        System.out.println("    time spent parsing(s): " + getTotalParseTime());
        System.out.println("    time spend parsing + building: " + (getTotalParseTime() + getIRBuildTime()));
        if (Options.PARSER_PRISM.load()) {
            System.out.println("  YARP:");
            System.out.println("    time C parse+serialize: " + getYARPCParseSerializeTime());
            System.out.println("    time deserializing: " + getYARPDeserializingTime());
            System.out.println("    serialized bytes: " + getTotalYARPSerializedBytes());
            System.out.println("    serialized to source ratio: x" +
                    ((float)getTotalYARPSerializedBytes() / getTotalParsedBytes()));
        }
        System.out.println("  IRBuild:");
        System.out.println("    build time: " + getIRBuildTime());
    }

    private double getIRBuildTime() {
        return totalIRBuildTime.get() / 1_000_000_000.0;
    }

    public void addIRBuildTime(long time) {
        totalIRBuildTime.addAndGet(time);
    }
}
