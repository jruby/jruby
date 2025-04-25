package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.RubyEncoding;
import org.jruby.RubyString;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class StringBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final Handle BYTELIST_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "bytelist",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class),
            false);
    public static final Handle STRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "string",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class),
            false);

    public static final Handle CSTRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "cstring",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class, String.class, int.class),
            false);
    public static final Handle EMPTY_STRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "emptyString",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class),
            false);
    public static final Handle BUFFER_STRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "bufferString",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class),
            false);
    public static final Handle FSTRING_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "fstring",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class, String.class, int.class),
            false);
    public static final Handle FSTRING_SIMPLE_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(StringBootstrap.class),
            "fstringSimple",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class),
            false);

    private static final MethodHandle STRING_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, ByteList.class, int.class)
                    .invokeStaticQuiet(LOOKUP, StringBootstrap.class, "string");

    private static final MethodHandle CSTRING_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, ByteList.class, int.class, String.class, int.class)
                    .invokeStaticQuiet(LOOKUP, StringBootstrap.class, "chilledString");

    private static final MethodHandle FSTRING_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, MutableCallSite.class, ByteList.class, int.class, String.class, int.class)
                    .invokeStaticQuiet(LOOKUP, StringBootstrap.class, "frozenString");
    private static final MethodHandle FSTRING_SIMPLE_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, MutableCallSite.class, ByteList.class, int.class)
                    .invokeStaticQuiet(LOOKUP, StringBootstrap.class, "frozenStringSimple");
    private static final MethodHandle BUFFERSTRING_HANDLE =
            Binder
                    .from(RubyString.class, ThreadContext.class, Encoding.class, int.class, int.class)
                    .invokeStaticQuiet(LOOKUP, StringBootstrap.class, "bufferString");

    public static CallSite bytelist(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encodingName) {
        return new ConstantCallSite(constant(ByteList.class, bytelist(value, encodingName)));
    }

    public static CallSite string(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encodingName, int cr) {
        return new ConstantCallSite(insertArguments(STRING_HANDLE, 1, bytelist(value, encodingName), cr));
    }

    public static CallSite cstring(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encodingName, int cr, String file, int line) {
        return new ConstantCallSite(insertArguments(CSTRING_HANDLE, 1, bytelist(value, encodingName), cr, file, line));
    }

    public static CallSite emptyString(MethodHandles.Lookup lookup, String name, MethodType type, String encodingName) {
        RubyString.EmptyByteListHolder holder = RubyString.getEmptyByteList(encodingFromName(encodingName));
        return new ConstantCallSite(insertArguments(STRING_HANDLE, 1, holder.bytes, holder.cr));
    }

    public static CallSite bufferString(MethodHandles.Lookup lookup, String name, MethodType type, String encodingName, int size) {
        return new ConstantCallSite(insertArguments(BUFFERSTRING_HANDLE, 1, encodingFromName(encodingName), size, StringSupport.CR_7BIT));
    }

    public static CallSite fstring(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encodingName, int cr, String file, int line) {
        MutableCallSite site = new MutableCallSite(type);

        site.setTarget(insertArguments(FSTRING_HANDLE, 1, site, bytelist(value, encodingName), cr, file, line));

        return site;
    }

    public static CallSite fstringSimple(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encodingName, int cr) {
        MutableCallSite site = new MutableCallSite(type);

        site.setTarget(insertArguments(FSTRING_SIMPLE_HANDLE, 1, site, bytelist(value, encodingName), cr));

        return site;
    }

    public static RubyString string(ThreadContext context, ByteList value, int cr) {
        return RubyString.newStringShared(context.runtime, value, cr);
    }

    public static RubyString chilledString(ThreadContext context, ByteList value, int cr, String file, int line) {
        return RubyString.newChilledString(context.runtime, value, cr, file, line);
    }

    public static RubyString bufferString(ThreadContext context, Encoding encoding, int size, int cr) {
        return RubyString.newString(context.runtime, new ByteList(size, encoding), cr);
    }

    public static RubyString frozenString(ThreadContext context, MutableCallSite site, ByteList value, int cr, String file, int line) {
        RubyString frozen = IRRuntimeHelpers.newFrozenString(context, value, cr, file, line);

        // Permanently bind to the new frozen string
        site.setTarget(dropArguments(constant(RubyString.class, frozen), 0, ThreadContext.class));

        return frozen;
    }

    public static RubyString frozenStringSimple(ThreadContext context, MutableCallSite site, ByteList value, int cr) {
        RubyString frozen = IRRuntimeHelpers.newFrozenString(context, value, cr);

        // Permanently bind to the new frozen string
        site.setTarget(dropArguments(constant(RubyString.class, frozen), 0, ThreadContext.class));

        return frozen;
    }

    public static ByteList bytelist(String value, String encodingName) {
        Encoding encoding = encodingFromName(encodingName);

        if (value.length() == 0) {
            // special case empty string and don't create a new BL
            return RubyString.getEmptyByteList(encoding).bytes;
        }

        return new ByteList(RubyEncoding.encodeISO(value), encoding, false);
    }

    public static ByteList bytelist(int size, String encodingName) {
        Encoding encoding = encodingFromName(encodingName);

        return new ByteList(size, encoding);
    }

    public static Encoding encodingFromName(String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        return encoding;
    }
}
