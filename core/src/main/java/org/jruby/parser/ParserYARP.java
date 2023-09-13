package org.jruby.parser;

import jnr.ffi.LibraryLoader;
import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.management.ParserStats;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.yarp.Nodes;
import org.yarp.YarpParseResult;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.jruby.parser.ParserManager.isEval;

public class ParserYARP extends Parser {
    ParserBindingYARP yarpLibrary;

    public ParserYARP(Ruby runtime) {
        super(runtime);

        String path = runtime.getInstanceConfig().getJRubyHome() + "/lib/libyarp.so";
        //System.out.println("Binding to " + path);
        yarpLibrary = LibraryLoader.create(ParserBindingYARP.class).load(path);
    }
    // FIXME: error/warn when cannot bind to yarp (probably silent fail-over option too)

    // FIXME: Need to connect DATA
    // FIXME: main source file should be IO in case of __END__/DATA.
    @Override
    public ParseResult parse(String fileName, int lineNumber, ByteList content, DynamicScope blockScope, int flags) {
        int sourceLength = content.realSize();
        byte[] source = content.begin() == 0 ? content.unsafeBytes() : content.bytes();
        byte[] metadata = generateMetadata(fileName, blockScope, flags);
        byte[] serialized = parse(source, sourceLength, metadata);
        return parseInternal(fileName, blockScope, source, serialized);
    }

    private ParseResult parseInternal(String fileName, DynamicScope blockScope, byte[] source, byte[] serialized) {
        long time = 0;

        if (ParserManager.PARSER_TIMING) time = System.nanoTime();
        Nodes.Source nodeSource = new Nodes.Source(source);
        org.yarp.ParseResult res = org.yarp.Loader.load(serialized, nodeSource);
        if (ParserManager.PARSER_TIMING) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addYARPTimeDeserializing(System.nanoTime() - time);
            stats.addYARPSerializedBytes(serialized.length);
            stats.addParsedBytes(source.length);
        }

        if (res.errors != null && res.errors.length > 0) {
            throw runtime.newSyntaxError(fileName + ":" + nodeSource.line(res.errors[0].location.startOffset) + ": " + res.errors[0].message);
        }

        ParseResult result = new YarpParseResult(fileName, source, (Nodes.ProgramNode) res.value);
        if (blockScope != null) {
            result.getStaticScope().setEnclosingScope(blockScope.getStaticScope());
        }

        return result;
    }

    @Override
    ParseResult parse(String fileName, int lineNumber, InputStream in, Encoding encoding,
                             DynamicScope blockScope, int flags) {
        byte[] source = getSourceAsBytes(fileName, in);
        byte[] metadata = generateMetadata(fileName, blockScope, flags);
        byte[] serialized = parse(source, source.length, metadata);
        return parseInternal(fileName, blockScope, source, serialized);
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
    private byte[] parse(byte[] source, int sourceLength, byte[] metadata) {
        long time = 0;
        if (ParserManager.PARSER_TIMING) time = System.nanoTime();
        ParserBindingYARP.Buffer buffer = new ParserBindingYARP.Buffer(jnr.ffi.Runtime.getRuntime(yarpLibrary));
        yarpLibrary.yp_buffer_init(buffer);
        yarpLibrary.yp_parse_serialize(source, sourceLength, buffer, metadata);
        if (ParserManager.PARSER_TIMING) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addYARPTimeCParseSerialize(System.nanoTime() - time);
        }

        int length = buffer.length.intValue();
        byte[] src = new byte[length];
        buffer.value.get().get(0, src, 0, length);

        return src;
    }

    private byte[] generateMetadata(String fileName, DynamicScope scope, int flags) {
        ByteList metadata = new ByteList();

        byte[] name = fileName.getBytes();
        appendUnsignedInt(metadata, name.length);
        metadata.append(name);

        // all evals should provide some scope
        if (isEval(flags)) {
            encodeEvalScopes(metadata, scope.getStaticScope());
        } else {
            appendUnsignedInt(metadata, 0);
        }

        return metadata.bytes(); // FIXME: extra arraycopy
    }

    private void writeUnsignedInt(ByteList buf, int index, int value) {
        buf.set(index, value);
        buf.set(index+1, value >>> 8);
        buf.set(index+2, value >>> 16);
        buf.set(index+3, value >>> 24);
    }

    private void appendUnsignedInt(ByteList buf, int value) {
        buf.append(value);
        buf.append(value >>> 8);
        buf.append(value >>> 16);
        buf.append(value >>> 24);
    }

    private byte[] encodeEvalScopes(ByteList buf, StaticScope scope) {
        int startIndex = buf.realSize();
        appendUnsignedInt(buf, 0);
        int count = encodeEvalScopesInner(buf, scope, 0);
        writeUnsignedInt(buf, startIndex, count);
        return buf.bytes();
    }

    private int encodeEvalScopesInner(ByteList buf, StaticScope scope, int count) {
        if (scope.getEnclosingScope() != null && scope.isBlockScope()) {
            count = encodeEvalScopesInner(buf, scope.getEnclosingScope(), count + 1);
        }

        // once more for method scope
        String names[] = scope.getVariables();
        appendUnsignedInt(buf, names.length);
        for (String name : names) {
            byte[] bytes = name.getBytes(); // FIXME: needs to be raw bytes
            appendUnsignedInt(buf, bytes.length);
            buf.append(bytes);
        }

        return count;
    }
}
