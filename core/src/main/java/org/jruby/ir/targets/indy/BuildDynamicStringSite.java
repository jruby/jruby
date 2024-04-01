package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jcodings.Encoding;
import org.jruby.RubyString;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class BuildDynamicStringSite extends MutableCallSite {
    public static final Handle BUILD_DSTRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(BuildDynamicStringSite.class),
            "buildDString",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class),
            false);

    public static CallSite buildDString(MethodHandles.Lookup lookup, String name, MethodType type, Object[] args) {
        return new BuildDynamicStringSite(type, args);
    }

    record ByteListAndCodeRange(ByteList bl, int cr) {}

    final int initialSize;
    final Encoding encoding;
    final long descriptor;
    final boolean frozen;
    final int elementCount;
    final ByteListAndCodeRange[] strings;

    public BuildDynamicStringSite(MethodType type, Object[] stringArgs) {
        super(type);

        initialSize = (Integer) stringArgs[stringArgs.length - 5];
        encoding = StringBootstrap.encodingFromName((String) stringArgs[stringArgs.length - 4]);
        frozen = ((Integer) stringArgs[stringArgs.length - 3]) != 0;
        descriptor = (Long) stringArgs[stringArgs.length - 2];
        elementCount = (Integer) stringArgs[stringArgs.length - 1];

        ByteListAndCodeRange[] strings = new ByteListAndCodeRange[elementCount];
        int stringArgsIdx = 0;
        Binder binder = Binder.from(type);

        boolean specialize = elementCount <= 4;
        for (int i = 0; i < elementCount; i++) {
            if ((descriptor & (1 << i)) != 0) {
                ByteListAndCodeRange blcr = new ByteListAndCodeRange(StringBootstrap.bytelist((String) stringArgs[stringArgsIdx * 3], (String) stringArgs[stringArgsIdx * 3 + 1]), (Integer) stringArgs[stringArgsIdx * 3 + 2]);
                strings[i] = blcr;
                if (specialize) {
                    // for small arities bind BL+CR directly and call specialized version below
                    binder = binder.insert(i + 1, blcr);
                }
                stringArgsIdx++;
            }
        }
        this.strings = strings;

        if (specialize) {
            // bind directly to specialized builds
            binder = binder.append(arrayOf(Encoding.class, int.class), encoding, initialSize);
            setTarget(binder.invokeStaticQuiet(BuildDynamicStringSite.class, "buildString"));
        } else {
            binder = binder.prepend(this).collect(2, IRubyObject[].class);
            setTarget(binder.invokeVirtualQuiet("buildString"));
        }

    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, Encoding encoding, int initialSize) { // 0b0
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Encoding encoding, int initialSize) { // 0b1
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, Encoding encoding, int initialSize) { // 0b00
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, Encoding encoding, int initialSize) { // 0b01
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, Encoding encoding, int initialSize) { // 0b10
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, Encoding encoding, int initialSize) { // 0b11
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, IRubyObject c, Encoding encoding, int initialSize) { // 0b000
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);
        buffer.appendAsDynamicString(c);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b001
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, IRubyObject c, Encoding encoding, int initialSize) { // 0b010
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.appendAsDynamicString(c);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b011
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, IRubyObject c, Encoding encoding, int initialSize) { // 0b100
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);
        buffer.appendAsDynamicString(c);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b101
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, IRubyObject c, Encoding encoding, int initialSize) { // 0b110
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.appendAsDynamicString(c);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b111
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, IRubyObject c, IRubyObject d, Encoding encoding, int initialSize) { // 0b0000
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);
        buffer.appendAsDynamicString(c);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, IRubyObject c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0001
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);
        buffer.appendAsDynamicString(c);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, ByteListAndCodeRange c, IRubyObject d, Encoding encoding, int initialSize) { // 0b0010
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, IRubyObject b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0011
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.appendAsDynamicString(b);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, IRubyObject c, IRubyObject d, Encoding encoding, int initialSize) { // 0b0100
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.appendAsDynamicString(c);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, IRubyObject c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0101
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.appendAsDynamicString(c);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, ByteListAndCodeRange c, IRubyObject d, Encoding encoding, int initialSize) { // 0b0110
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, IRubyObject a, ByteListAndCodeRange b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0111
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.appendAsDynamicString(a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, IRubyObject c, IRubyObject d, Encoding encoding, int initialSize) { // 0b1000
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);
        buffer.appendAsDynamicString(c);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, IRubyObject c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1001
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);
        buffer.appendAsDynamicString(c);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, ByteListAndCodeRange c, IRubyObject d, Encoding encoding, int initialSize) { // 0b1010
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, IRubyObject b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1011
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.appendAsDynamicString(b);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, IRubyObject c, IRubyObject d, Encoding encoding, int initialSize) { // 0b1100
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.appendAsDynamicString(c);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, IRubyObject c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1101
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.appendAsDynamicString(c);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, ByteListAndCodeRange c, IRubyObject d, Encoding encoding, int initialSize) { // 0b1110
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.appendAsDynamicString(d);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1111
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        buffer.catWithCodeRange(a.bl, a.cr);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public RubyString buildString(ThreadContext context, IRubyObject... values) {
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);

        int valueIdx = 0;
        for (int i = 0; i < elementCount; i++) {
            if ((descriptor & (1 << i)) != 0) {
                buffer.catWithCodeRange(strings[i].bl, strings[i].cr);
            } else {
                buffer.appendAsDynamicString(values[valueIdx++]);
            }
        }

        if (frozen) {
            buffer.freeze(context);
        }

        return buffer;
    }
}
