package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;

import org.jruby.util.StringSupport;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SearchNonAsciiBenchmark {

    private static final int INVOCATIONS = 100_000;

    // Where the first non-ASCII byte appears, as a fraction of the buffer length.
    // "none" means the buffer is entirely ASCII (worst case: full scan, no match).
    @Param({"none", "start", "middle", "end"})
    public String hitPosition;

    @Param({"7", "8", "16", "64", "1024", "65536"})
    public int size;

    private byte[] bytes;

    @Setup
    public void setup() {
        bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) ('a' + (i % 26));
        }
        switch (hitPosition) {
            case "none":
                break;
            case "start":
                if (size > 0) bytes[0] = (byte) 0xFF;
                break;
            case "middle":
                if (size > 0) bytes[size / 2] = (byte) 0xFF;
                break;
            case "end":
                if (size > 0) bytes[size - 1] = (byte) 0xFF;
                break;
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void searchNonAsciiSwar(final Blackhole blackhole) {
        final byte[] b = bytes;
        final int len = size;
        for (int i = 0; i < INVOCATIONS; i++) {
            blackhole.consume(StringSupport.searchNonAscii(b, 0, len));
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void searchNonAsciiScalar(final Blackhole blackhole) {
        final byte[] b = bytes;
        final int len = size;
        for (int i = 0; i < INVOCATIONS; i++) {
            blackhole.consume(searchNonAsciiScalarBaseline(b, 0, len));
        }
    }

    // Pre-SWAR baseline (the original StringSupport.searchNonAscii), kept here for comparison only.
    private static int searchNonAsciiScalarBaseline(byte[] bytes, int p, int end) {
        while (p < end) {
            if ((bytes[p] & 0x80) != 0) return p;
            p++;
        }
        return -1;
    }

}
