package org.jruby.management;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.Ruby;

public class ParserStats implements ParserStatsMBean {
    private final SoftReference<Ruby> ruby;
    private AtomicInteger totalParseTime = new AtomicInteger(0);
    private AtomicInteger totalParsedBytes = new AtomicInteger(0);
    private AtomicInteger totalEvalParses = new AtomicInteger(0);
    private AtomicInteger totalLoadParses = new AtomicInteger(0);
    private AtomicInteger totalJRubyModuleParses = new AtomicInteger(0);
    
    public ParserStats(Ruby ruby) {
        this.ruby = new SoftReference<Ruby>(ruby);
    }

    public void addParseTime(int time) {
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
        Ruby runtime = ruby.get();
        if (runtime == null) return 0;
        return runtime.getParser().getTotalTime() / 1000000000.0;
    }

    public int getTotalParsedBytes() {
        Ruby runtime = ruby.get();
        if (runtime == null) return 0;
        return runtime.getParser().getTotalBytes();
    }

    public double getParseTimePerKB() {
        int totalBytes = getTotalParsedBytes();
        if (totalBytes == 0) return 0;
        return getTotalParseTime() / (totalBytes / 1000.0);
    }

    public int getNumberOfEvalParses() {
        return totalEvalParses.get();
    }

    public int getNumberOfLoadParses() {
        return totalLoadParses.get();
    }
}
