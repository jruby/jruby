package org.jruby.parser;

import jnr.ffi.Pointer;
import jnr.ffi.LibraryLoader;
import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.yarp.Nodes;
import org.yarp.YarpParseResult;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class YARPParser extends Parser {
    YARPParserBindings yarpLibrary;

    public YARPParser(Ruby runtime) {
        super(runtime);

        String path = runtime.getInstanceConfig().getJRubyHome() + "/lib/libyarp.so";
        System.out.println("Binding to " + path);
        yarpLibrary = LibraryLoader.create(YARPParserBindings.class).load(path);
    }

    // FIXME: lots of missing stuff.
    // FIXME: main source file should be IO in case of __END__/DATA.
    // FIXME: has memory leak.
    public ParseResult parse(String fileName, int lineNumber, ByteList content, DynamicScope blockScope, int flags) {
        // FIXME: this is duplicated source array when it usually will not need it.
        byte[] source = content.bytes();
        byte[] metadata = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] serialized = parse(source, metadata);
        org.yarp.ParseResult res = org.yarp.Loader.load(serialized, new Nodes.Source(source));

        ParseResult result = new YarpParseResult(fileName, source, (Nodes.ProgramNode) res.value);
        if (blockScope != null) {
            result.getStaticScope().setEnclosingScope(blockScope.getStaticScope());
        }

        return result;
    }

    public ParseResult parse(String fileName, int lineNumber, InputStream in, Encoding encoding,
                             DynamicScope blockScope, int flags) {
        byte[] source = getSourceAsBytes(fileName, in);
        byte[] metadata = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] serialized = parse(source, metadata);
        org.yarp.ParseResult res = org.yarp.Loader.load(serialized, new Nodes.Source(source));

        ParseResult result = new YarpParseResult(fileName, source, (Nodes.ProgramNode) res.value);
        if (blockScope != null) {
            result.getStaticScope().setEnclosingScope(blockScope.getStaticScope());
        }

        return result;
    }

    private byte[] getSourceAsBytes(String fileName, InputStream in) {
        if (in instanceof LoadServiceResourceInputStream) {
            return ((LoadServiceResourceInputStream) in).getBytes();
        }

        return loadFully(fileName, in);
    }

    private byte[] loadFully(String fileName, InputStream in) {
        // Assumes complete source is available (which should be true for fis and bais).
        try(DataInputStream data = new DataInputStream(in)) {
            int length = data.available();
            byte[] source = new byte[length];
            data.readFully(source);
            return source;
        } catch (IOException e) {
            throw runtime.newSyntaxError("Failed to read source file: " + fileName);
        }
    }

    // FIXME: metadata is formed via some other method and how this is packaged as API needs to be figured out.
    private byte[] parse(byte[] source, byte[] metadata) {
        //yarpLibrary.yp_version();
        YARPParserBindings.Buffer buffer = new YARPParserBindings.Buffer(jnr.ffi.Runtime.getRuntime(yarpLibrary));
        yarpLibrary.yp_buffer_init(buffer);
        yarpLibrary.yp_parse_serialize(source, source.length, buffer, metadata);

        int length = buffer.length.intValue();
        byte[] src = new byte[length];
        buffer.value.get().get(0, src, 0, length);

        return src;
    }

    private byte[] encodeEvalScopes(StaticScope scope) {
        ByteList buf = new ByteList();
        buf.append(0); // keep space for number of scopes.
        int count = encodeEvalScopesInner(buf, scope, 0);
        //System.out.println("COUNT: " + count);
        buf.set(0, count); // FIXME: This only allows 256 vars

        return buf.bytes();
    }

    private int encodeEvalScopesInner(ByteList buf, StaticScope scope, int count) {
        if (scope.getEnclosingScope() != null && scope.isBlockScope()) {
            count = encodeEvalScopesInner(buf, scope.getEnclosingScope(), count + 1);
        }

        // once more for method scope
        String names[] = scope.getVariables();
        buf.append(names.length);
        for (String name : names) {
            buf.append(name.getBytes()); // FIXME: needs to be raw bytes
            buf.append(0);
        }

        return count;
    }
}
