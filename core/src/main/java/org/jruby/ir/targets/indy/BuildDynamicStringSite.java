package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jcodings.Encoding;
import org.jruby.Appendable;
import org.jruby.RubyString;
import org.jruby.api.Convert;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Arrays;

import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class BuildDynamicStringSite extends MutableCallSite {
    private static final Logger LOG = LoggerFactory.getLogger(BuildDynamicStringSite.class);

    public static final Handle BUILD_DSTRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(BuildDynamicStringSite.class),
            "buildDString",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class),
            false);
    private static final int MAX_ELEMENTS_FOR_SPECIALIZE1 = 4;
    private static final int MAX_DYNAMIC_ARGS_FOR_SPECIALIZE2 = 5;
    public static final int MAX_ELEMENTS = 50;
    private static final int METADATA_ARGS_COUNT = 6;

    public static CallSite buildDString(MethodHandles.Lookup lookup, String name, MethodType type, Object[] args) {
        return new BuildDynamicStringSite(type, args);
    }

    record ByteListAndCodeRange(ByteList bl, int cr) {}

    final int initialSize;
    final Encoding encoding;
    final long descriptor;
    final boolean frozen;
    final boolean chilled;
    final int elementCount;
    final ByteListAndCodeRange[] strings;

    public BuildDynamicStringSite(MethodType type, Object[] stringArgs) {
        super(type);

        int metadataIndex = stringArgs.length - METADATA_ARGS_COUNT;

        initialSize = (Integer) stringArgs[metadataIndex];
        encoding = StringBootstrap.encodingFromName((String) stringArgs[metadataIndex + 1]);
        frozen = ((Integer) stringArgs[metadataIndex + 2]) != 0;
        chilled = ((Integer) stringArgs[metadataIndex + 3]) != 0;
        descriptor = (Long) stringArgs[metadataIndex + 4];
        elementCount = (Integer) stringArgs[metadataIndex + 5];

        ByteListAndCodeRange[] strings = new ByteListAndCodeRange[elementCount];
        int stringArgsIdx = 0;
        Binder binder = Binder.from(type);

        int dynamicArgs = type.parameterCount() - 1;
        int to_sArgCount = 2;
        int[] permute = new int[to_sArgCount * dynamicArgs + 1]; // context followed by context, arg, arg triplets
        permute[0] = 0;
        for (int i = 0; i < dynamicArgs; i++) {
            int base = i * to_sArgCount + 1;
            permute[base] = 0;
            permute[base + 1] = i + 1;
        }
        binder = binder.permute(permute);

        // now collect them by binding to AsStringSite
        for (int i = 0; i < dynamicArgs; i++) {
            // separate filter for each dynamic argument, so they can type profile independently
            binder = binder.collect(i + 1, to_sArgCount, Appendable.class, constructGuardedToStringFilter());
        }

        boolean specialize = elementCount <= MAX_ELEMENTS_FOR_SPECIALIZE1;
        for (int i = 0; i < elementCount; i++) {
            if (isStringElement(descriptor, i)) {
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
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info("dstring(" + Long.toBinaryString(descriptor) +")" + "\tbound directly");
            }
            binder = binder.append(arrayOf(Encoding.class, int.class), encoding, initialSize);
            setTarget(binder.invokeStaticQuiet(BuildDynamicStringSite.class, "buildString"));
        } else if (dynamicArgs <= MAX_DYNAMIC_ARGS_FOR_SPECIALIZE2) {
            // second level specialization, using call site to hold strings, no argument[] box, and appending in a loop
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info("dstring(" + Long.toBinaryString(descriptor) +")" + "\tbound to unrolled loop");
            }
            binder = binder.prepend(this).append(arrayOf(Encoding.class, int.class), encoding, initialSize);
            setTarget(binder.invokeVirtualQuiet("buildString2"));
        } else {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info("dstring(" + Long.toBinaryString(descriptor) +")" + "\tbound to loop");
            }
            binder = binder.prepend(this).collect(2, Appendable[].class);
            setTarget(binder.invokeVirtualQuiet("buildStringFromMany"));
        }
    }

    private static MethodHandle constructGuardedToStringFilter() {
        // create an invoke site for the to_s call
        MethodType toSType = MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class);
        CallSite toS = SelfInvokeSite.bootstrap(MethodHandles.lookup(), "invokeFunctional:to_s", toSType, 0, 0, "", -1);
        MethodHandle toS_handle = toS.dynamicInvoker();

        // Cast the result to RubyString, or else call anyToString on the original
        MethodType checkedToSType = MethodType.methodType(Appendable.class, ThreadContext.class, IRubyObject.class);
        MethodHandle checkedToS = Binder.from(checkedToSType)
                .fold(toS_handle) // fold in to_s result
                .invokeStaticQuiet(BuildDynamicStringSite.class, "castToSResultOrAny");

        // guarded with "Appendable" interface for trivially-appendable types
        MethodHandle checkcast = Binder.from(toSType.changeReturnType(boolean.class))
                .permute(1)
                .cast(boolean.class, Object.class)
                .prepend(Appendable.class)
                .invokeVirtualQuiet("isInstance");
        MethodHandle guardedToS = MethodHandles.guardWithTest(checkcast, Binder.from(toSType.changeReturnType(Appendable.class)).permute(1).cast(Appendable.class, Appendable.class).identity(), checkedToS);

        return guardedToS;
    }

    /**
     * Convert the to_s result to an Appendable, returning it if it is a RubyString, or returning the result of
     * calling anyToString on the original object.
     *
     * This is equivalent to RubyBasicObject#asString with the to_s call already performed.
     *
     * @param toSResult the result of the already-performed to_s call
     * @param context the current context
     * @param original the original object
     * @return the to_s result as Appendable, if it is a RubyString, or the anyToString of the original
     */
    public static Appendable castToSResultOrAny(IRubyObject toSResult, ThreadContext context, IRubyObject original) {
        if (toSResult instanceof RubyString str) {
            return str;
        }
        return Convert.anyToString(context, original);
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Encoding encoding, int initialSize) { // 0b0
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Encoding encoding, int initialSize) { // 0b1
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, Encoding encoding, int initialSize) { // 0b00
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, Encoding encoding, int initialSize) { // 0b01
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, Encoding encoding, int initialSize) { // 0b10
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, Encoding encoding, int initialSize) { // 0b11
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, Appendable c, Encoding encoding, int initialSize) { // 0b000
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);
        c.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b001
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, Appendable c, Encoding encoding, int initialSize) { // 0b010
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);
        c.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b011
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, Appendable c, Encoding encoding, int initialSize) { // 0b100
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);
        c.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b101
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, Appendable c, Encoding encoding, int initialSize) { // 0b110
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);
        c.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, ByteListAndCodeRange c, Encoding encoding, int initialSize) { // 0b111
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, Appendable c, Appendable d, Encoding encoding, int initialSize) { // 0b0000
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);
        c.appendIntoString(buffer);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, Appendable c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0001
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);
        c.appendIntoString(buffer);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, ByteListAndCodeRange c, Appendable d, Encoding encoding, int initialSize) { // 0b0010
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);
        buffer.catWithCodeRange(c.bl, c.cr);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, Appendable b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0011
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        b.appendIntoString(buffer);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, Appendable c, Appendable d, Encoding encoding, int initialSize) { // 0b0100
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);
        c.appendIntoString(buffer);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, Appendable c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0101
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);
        c.appendIntoString(buffer);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, ByteListAndCodeRange c, Appendable d, Encoding encoding, int initialSize) { // 0b0110
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, Appendable a, ByteListAndCodeRange b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b0111
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
        a.appendIntoString(buffer);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, Appendable c, Appendable d, Encoding encoding, int initialSize) { // 0b1000
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);
        c.appendIntoString(buffer);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, Appendable c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1001
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);
        c.appendIntoString(buffer);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, ByteListAndCodeRange c, Appendable d, Encoding encoding, int initialSize) { // 0b1010
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);
        buffer.catWithCodeRange(c.bl, c.cr);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, Appendable b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1011
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        b.appendIntoString(buffer);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, Appendable c, Appendable d, Encoding encoding, int initialSize) { // 0b1100
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);
        c.appendIntoString(buffer);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, Appendable c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1101
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);
        c.appendIntoString(buffer);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, ByteListAndCodeRange c, Appendable d, Encoding encoding, int initialSize) { // 0b1110
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        d.appendIntoString(buffer);

        return buffer;
    }

    public static RubyString buildString(ThreadContext context, ByteListAndCodeRange a, ByteListAndCodeRange b, ByteListAndCodeRange c, ByteListAndCodeRange d, Encoding encoding, int initialSize) { // 0b1111
        RubyString buffer = createBufferFromStaticString(context, initialSize, a);
        buffer.catWithCodeRange(b.bl, b.cr);
        buffer.catWithCodeRange(c.bl, c.cr);
        buffer.catWithCodeRange(d.bl, d.cr);

        return buffer;
    }

    public RubyString buildString2(ThreadContext context, Encoding encoding, int initialSize) {
        return buildString2(context, null, null, null, null, encoding, initialSize);
    }

    public RubyString buildString2(ThreadContext context, Appendable a, Encoding encoding, int initialSize) {
        return buildString2(context, a, null, null, null, null, encoding, initialSize);
    }

    public RubyString buildString2(ThreadContext context, Appendable a, Appendable b, Encoding encoding, int initialSize) {
        return buildString2(context, a, b, null, null, null, encoding, initialSize);
    }

    public RubyString buildString2(ThreadContext context, Appendable a, Appendable b, Appendable c, Encoding encoding, int initialSize) {
        return buildString2(context, a, b, c, null, null, encoding, initialSize);
    }

    public RubyString buildString2(ThreadContext context, Appendable a, Appendable b, Appendable c, Appendable d, Encoding encoding, int initialSize) {
        return buildString2(context, a, b, c, d, null, encoding, initialSize);
    }

    public RubyString buildString2(ThreadContext context, Appendable a, Appendable b, Appendable c, Appendable d, Appendable e, Encoding encoding, int initialSize) {
        long descriptor = this.descriptor;
        ByteListAndCodeRange[] strings = this.strings;
        int i;
        RubyString buffer;

        if (isDynamicElement(descriptor, 0)) {
            // first element is dynamic, create empty buffer and start at index 0 below
            buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);
            i = 0;
        } else {
            // first element is string, use it to create buffer and start at index 1 below
            buffer = createBufferFromStaticString(context, initialSize, strings[0]);
            i = 1;
        }

        int dynamicArg = 0;
        int elementCount = this.elementCount;

        for (; i < elementCount; i++) {
            if (isDynamicElement(descriptor, i)) {
                Appendable dynamicElement = switch (dynamicArg++) {
                    case 0 -> a;
                    case 1 -> b;
                    case 2 -> c;
                    case 3 -> d;
                    case 4 -> e;
                    default ->
                            throw new RuntimeException("BUG: trying to use buildString2 with more than 5 dynamic args");
                };

                dynamicElement.appendIntoString(buffer);
            } else {
                ByteListAndCodeRange string = strings[i];
                buffer.catWithCodeRange(string.bl, string.cr);
            }
        }

        if (frozen) {
            buffer.freeze(context);
        } else if (chilled) {
            buffer.chill();
        }

        return buffer;
    }

    private static RubyString createBufferFromStaticString(ThreadContext context, int initialSize, ByteListAndCodeRange firstString) {
        RubyString buffer;
        ByteList firstStringByteList = firstString.bl;
        int firstStringCR = firstString.cr;

        // copy element bytes into a buffer initialSize wide, zeroing out the begin offset
        byte[] bufferArray = Arrays.copyOfRange(firstStringByteList.unsafeBytes(), firstStringByteList.begin(), initialSize);

        // use element realSize for starting buffer realSize
        ByteList bufferByteList = new ByteList(bufferArray, 0, firstStringByteList.realSize(), firstStringByteList.getEncoding(), false);

        buffer = RubyString.newString(context.runtime, bufferByteList, firstStringCR);
        return buffer;
    }

    private static boolean isDynamicElement(long descriptor, int i) {
        if (i > 63) throw new ArrayIndexOutOfBoundsException("bit " + i + " out of long range");
        return (descriptor & (1L << i)) == 0;
    }

    private static boolean isStringElement(long descriptor, int i) {
        if (i > 63) throw new ArrayIndexOutOfBoundsException("bit " + i + " out of long range");
        return (descriptor & (1L << i)) != 0;
    }

    public RubyString buildStringFromMany(ThreadContext context, Appendable... values) {
        RubyString buffer = StringBootstrap.bufferString(context, encoding, initialSize, StringSupport.CR_7BIT);

        int valueIdx = 0;
        for (int i = 0; i < elementCount; i++) {
            if (isStringElement(descriptor, i)) {
                buffer.catWithCodeRange(strings[i].bl, strings[i].cr);
            } else {
                values[valueIdx++].appendIntoString(buffer);
            }
        }

        if (frozen) {
            buffer.freeze(context);
        } else if (chilled) {
            buffer.chill();
        }

        return buffer;
    }
}
