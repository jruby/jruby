package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Aggregate;
import com.kenai.jffi.Type;
import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.ext.ffi.Platform.CPU_TYPE;
import org.jruby.ext.ffi.Platform.OS_TYPE;
import org.jruby.ext.ffi.StructLayout;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * The libffi filler cells chosen for a union by value, per CPU: a union of one floating type is a
 * floating-point aggregate only where the C ABI says so.
 */
public class UnionFillerTest extends TestCase {
    private Ruby runtime;

    @Override
    protected void setUp() {
        runtime = Ruby.newInstance();
    }

    @Override
    protected void tearDown() {
        runtime.tearDown();
    }

    private StructLayout union(String members) {
        return (StructLayout) runtime.evalScriptlet(
                "require 'ffi'; Class.new(FFI::Union) { layout " + members + " }.layout");
    }

    private static List<Type> cells(Aggregate descriptor) throws Exception {
        Field fields = descriptor.getClass().getDeclaredField("fields");
        fields.setAccessible(true);
        return Arrays.asList((Type[]) fields.get(descriptor));
    }

    private static void assertCells(String message, StructLayout layout, CPU_TYPE cpu, OS_TYPE os, Type... expected)
            throws Exception {
        assertEquals(message, Arrays.asList(expected), cells(FFIUtil.newUnion(layout, cpu, os)));
    }

    public void testHomogeneousDoublesUseTheFloatFillerOnlyWhereUnionsCanBeFloatingAggregates() throws Exception {
        StructLayout doubles = union(":a, :double, :b, [:double, 2]");

        for (CPU_TYPE cpu : new CPU_TYPE[] { CPU_TYPE.AARCH64, CPU_TYPE.ARM, CPU_TYPE.POWERPC64LE, CPU_TYPE.X86_64 }) {
            assertCells(cpu.name(), doubles, cpu, OS_TYPE.LINUX, Type.DOUBLE, Type.DOUBLE);
        }
        assertCells("darwin aarch64", doubles, CPU_TYPE.AARCH64, OS_TYPE.DARWIN, Type.DOUBLE, Type.DOUBLE);
        assertCells("windows x86_64", doubles, CPU_TYPE.X86_64, OS_TYPE.WINDOWS, Type.DOUBLE, Type.DOUBLE);

        for (CPU_TYPE cpu : new CPU_TYPE[] { CPU_TYPE.RISCV64, CPU_TYPE.S390X, CPU_TYPE.LOONGARCH64, CPU_TYPE.UNKNOWN }) {
            assertCells(cpu.name(), doubles, cpu, OS_TYPE.LINUX, Type.SINT64, Type.SINT64);
        }
    }

    public void testHomogeneousFloatsFollowTheSameRule() throws Exception {
        StructLayout floats = union(":f, :float, :v, [:float, 2]");

        assertCells("aarch64", floats, CPU_TYPE.AARCH64, OS_TYPE.LINUX, Type.FLOAT, Type.FLOAT);
        assertCells("s390x", floats, CPU_TYPE.S390X, OS_TYPE.LINUX, Type.SINT32, Type.SINT32);
    }

    public void testMixedUnionsAreClassifiedPerCellOnSysVx86_64() throws Exception {
        StructLayout mixed = union(":i, :int32, :f, [:float, 3]");

        assertCells("sysv x86_64", mixed, CPU_TYPE.X86_64, OS_TYPE.LINUX, Type.SINT32, Type.FLOAT, Type.FLOAT);
        assertCells("windows x86_64", mixed, CPU_TYPE.X86_64, OS_TYPE.WINDOWS, Type.SINT32, Type.SINT32, Type.SINT32);
        assertCells("aarch64", mixed, CPU_TYPE.AARCH64, OS_TYPE.LINUX, Type.SINT32, Type.SINT32, Type.SINT32);
    }
}
