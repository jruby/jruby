package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;
import org.jruby.util.ArraySupport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ArrayCopyBenchmark {

    private static final int INVOCATIONS = 100_000_000;

    private static final String[] arr0 = new String[0];
    private static final Object[] arr1 = {"obj"};
    private static final Object[] arr2 = {1, 2};
    private static final Object[] arr3 = {1, 2, null};
    private static final Object[] arr4 = {1, 2, 3, 4};
    private static final Object[] arr5 = {1, 2, null, 4, null};

    private static final String[] dst0 = {};
    private static final Object[] dst1 = new Object[1];
    private static final Object[] dst2 = new Object[3];
    private static final Object[] dst3 = {1, 2, null};
    private static final Object[] dst4 = {1, 2, null, 4, null};

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchSystemCopy0() {
        for (int i = 0; i < INVOCATIONS; i++) {
            System.arraycopy(arr0, 0, dst0, 0, arr0.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchSystemCopy1() {
        for (int i = 0; i < INVOCATIONS; i++) {
            System.arraycopy(arr1, 0, dst1, 0, arr1.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchSystemCopy2() {
        for (int i = 0; i < INVOCATIONS; i++) {
            System.arraycopy(arr2, 0, dst2, 0, arr2.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchSystemCopy3() {
        for (int i = 0; i < INVOCATIONS; i++) {
            System.arraycopy(arr3, 0, dst3, 0, arr3.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchSystemCopy4() {
        for (int i = 0; i < INVOCATIONS; i++) {
            System.arraycopy(arr4, 0, dst4, 0, arr4.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchSystemCopy5() {
        for (int i = 0; i < INVOCATIONS; i++) {
            System.arraycopy(arr5, 0, new Object[6], 1, arr5.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchUtilsCopy0() {
        for (int i = 0; i < INVOCATIONS; i++) {
            ArraySupport.copy(arr0, dst0, 0, arr0.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchUtilsCopy1() {
        for (int i = 0; i < INVOCATIONS; i++) {
            ArraySupport.copy(arr1, dst1, 0, arr1.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchUtilsCopy2() {
        for (int i = 0; i < INVOCATIONS; i++) {
            ArraySupport.copy(arr2, dst2, 0, arr2.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchUtilsCopy3() {
        for (int i = 0; i < INVOCATIONS; i++) {
            ArraySupport.copy(arr3, dst3, 0, arr3.length);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchUtilsCopy4() {
        for (int i = 0; i < INVOCATIONS; i++) {
            ArraySupport.copy(arr4, dst4, 0, arr4.length);
        }
    }

}
