package org.jruby.benchmark;

import java.nio.CharBuffer;
import java.util.concurrent.TimeUnit;

import org.jruby.Ruby;
import org.jruby.RubyString;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures UTF-8 conversion between Java strings, other CharSequences and RubyString.
 *
 * Length is in UTF-16 units of the source text. LATIN1 repeats seven ASCII characters and one
 * accented character. RubyString inputs go through their CharSequence view, which differs from
 * Java UTF-16 for supplementary characters (EMOJI).
 *
 * With JRuby, its dependencies and JMH on the classpath, run:
 * {@code java -cp "$CP" org.openjdk.jmh.Main UTF8ConversionBenchmark -prof gc}
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class UTF8ConversionBenchmark {
    @Param({"ASCII", "LATIN1", "CJK", "EMOJI"})
    public String kind;

    @Param({"16", "128", "1024", "4096"})
    public int length;

    private Ruby runtime;
    private String string;
    private StringBuilder builder;
    private CharBuffer buffer;
    private CharSequence rubySequence;
    private CharSequence custom;
    private RubyString encoded;

    @Setup
    public void setup() {
        runtime = Ruby.newInstance();
        String unit = "ASCII".equals(kind) ? "abcdefgh" : "LATIN1".equals(kind) ? "abcdefg\u00e9"
                : "CJK".equals(kind) ? "\u4e2d\u6587" : "\ud83d\ude00\ud83d\ude80";
        StringBuilder text = new StringBuilder(length);
        while (text.length() < length) text.append(unit);
        string = text.substring(0, length);
        builder = new StringBuilder(string);
        buffer = CharBuffer.wrap("[" + string + "]");
        buffer.position(1);
        buffer.limit(length + 1);
        encoded = RubyString.newString(runtime, string);
        rubySequence = encoded;
        custom = new CustomSequence(string);
    }

    @TearDown
    public void tearDown() {
        runtime.tearDown();
    }

    @Benchmark
    public RubyString newString() { return RubyString.newString(runtime, string); }

    @Benchmark
    public RubyString newBuilder() { return RubyString.newString(runtime, builder); }

    @Benchmark
    public RubyString newBuffer() {
        // Older implementations consume long buffers. Reset both versions identically.
        buffer.position(1);
        return RubyString.newString(runtime, buffer);
    }

    @Benchmark
    public RubyString newUnicode() { return RubyString.newUnicodeString(runtime, string); }

    @Benchmark
    public RubyString newRubyString() { return RubyString.newString(runtime, rubySequence); }

    @Benchmark
    public RubyString newCustom() { return RubyString.newString(runtime, custom); }

    @Benchmark
    public String decode() { return encoded.toString(); }

    private static final class CustomSequence implements CharSequence {
        private final String value;
        CustomSequence(String value) { this.value = value; }
        public int length() { return value.length(); }
        public char charAt(int index) { return value.charAt(index); }
        public CharSequence subSequence(int start, int end) {
            return new CustomSequence(value.substring(start, end));
        }
        public String toString() { return value; }
    }
}
