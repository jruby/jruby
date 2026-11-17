package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;

import org.jruby.util.ByteList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ByteListCompareBenchmark {

    // Where the two buffers first differ. "none" means they are equal (full comparison).
    @Param({"none", "start", "middle", "end"})
    public String diffPosition;

    @Param({"4", "8", "16", "64", "1024", "65536"})
    public int size;

    private byte[] a;
    private byte[] b;
    private ByteList listA;
    private ByteList listB;

    @Setup
    public void setup() {
        a = new byte[size];
        for (int i = 0; i < size; i++) {
            a[i] = (byte) ('a' + (i % 26));
        }
        b = a.clone();
        switch (diffPosition) {
            case "none":
                break;
            case "start":
                b[0] ^= 1;
                break;
            case "middle":
                b[size / 2] ^= 1;
                break;
            case "end":
                b[size - 1] ^= 1;
                break;
        }
        listA = new ByteList(a, false);
        listB = new ByteList(b, false);
    }

    @Benchmark
    public boolean startsWith() {
        return listA.startsWith(listB, 0);
    }

    @Benchmark
    public boolean startsWithBaseline() {
        return startsWithOriginal(listA, listB, 0);
    }

    @Benchmark
    public int memcmpRanges() {
        return ByteList.memcmp(a, 0, size, b, 0, size);
    }

    @Benchmark
    public int memcmpRangesBaseline() {
        return memcmpRangesOriginal(a, 0, size, b, 0, size);
    }

    @Benchmark
    public int memcmpLen() {
        return ByteList.memcmp(a, 0, b, 0, size);
    }

    @Benchmark
    public int memcmpLenBaseline() {
        return memcmpLenOriginal(a, 0, b, 0, size);
    }

    private static boolean startsWithOriginal(ByteList self, ByteList other, int toffset) {
        if (self.realSize() == 0 || self.realSize() < other.realSize() + toffset) return false;

        byte[] ta = self.unsafeBytes();
        int to = self.begin() + toffset;
        byte[] pa = other.unsafeBytes();
        int po = other.begin();
        int pc = other.realSize();

        while (--pc >= 0) if (ta[to++] != pa[po++]) return false;
        return true;
    }

    private static int memcmpRangesOriginal(byte[] first, int firstStart, int firstLen, byte[] second, int secondStart, int secondLen) {
        if (first == second) return 0;
        final int len = Math.min(firstLen, secondLen);
        int offset = -1;
        for (; ++offset < len && first[firstStart + offset] == second[secondStart + offset]; ) ;
        if (offset < len) {
            return (first[firstStart + offset] & 0xFF) > (second[secondStart + offset] & 0xFF) ? 1 : -1;
        }
        return firstLen == secondLen ? 0 : firstLen == len ? -1 : 1;
    }

    private static int memcmpLenOriginal(byte[] first, int firstStart, byte[] second, int secondStart, int len) {
        int a = firstStart;
        int b = secondStart;
        int tmp;

        for (; len != 0; --len) {
            if ((tmp = first[a++] - second[b++]) != 0) {
                return tmp;
            }
        }
        return 0;
    }

}
