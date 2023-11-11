package org.jruby.parser;

import jnr.ffi.LibraryLoader;
import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.management.ParserStats;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.prism.Nodes;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.jruby.parser.ParserManager.isEval;

public class ParserPrism extends Parser {
    ParserBindingPrism prismLibrary;

    public ParserPrism(Ruby runtime) {
        super(runtime);

        String path = runtime.getInstanceConfig().getJRubyHome() + "/lib/prism.so";
        //System.out.println("Binding to " + path);
        prismLibrary = LibraryLoader.create(ParserBindingPrism.class).load(path);
    }
    // FIXME: error/warn when cannot bind to yarp (probably silent fail-over option too)

    // FIXME: Need to connect DATA
    // FIXME: main source file should be IO in case of __END__/DATA.
    @Override
    public ParseResult parse(String fileName, int lineNumber, ByteList content, DynamicScope blockScope, int flags) {
        int sourceLength = content.realSize();
        byte[] source = content.begin() == 0 ? content.unsafeBytes() : content.bytes();
        byte[] metadata = generateMetadata(fileName, lineNumber, content.getEncoding(), blockScope, flags);
        byte[] serialized = parse(source, sourceLength, metadata);
        return parseInternal(fileName, blockScope, source, serialized);
    }

    private ParseResult parseInternal(String fileName, DynamicScope blockScope, byte[] source, byte[] serialized) {
        long time = 0;

        if (ParserManager.PARSER_TIMING) time = System.nanoTime();
        Nodes.Source nodeSource = new Nodes.Source(source);
        LoaderPrism loader = new LoaderPrism(runtime, serialized, nodeSource);
        org.prism.ParseResult res = loader.load();
        Encoding encoding = loader.getEncoding();

        if (ParserManager.PARSER_TIMING) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addYARPTimeDeserializing(System.nanoTime() - time);
            stats.addYARPSerializedBytes(serialized.length);
            stats.addParsedBytes(source.length);
        }

        if (res.warnings != null) {
            for (org.prism.ParseResult.Warning warning: res.warnings) {
                runtime.getWarnings().warn(fileName, nodeSource.line(warning.location.startOffset), warning.message);
            }
        }

        if (res.errors != null && res.errors.length > 0) {
            int line = nodeSource.line(res.errors[0].location.startOffset);

            throw runtime.newSyntaxError(fileName + ":" + line + ": " + res.errors[0].message);
        }

        ParseResult result = new ParseResultPrism(fileName, source, (Nodes.ProgramNode) res.value, nodeSource, encoding);
        if (blockScope != null) {
            result.getStaticScope().setEnclosingScope(blockScope.getStaticScope());
        }

        return result;
    }

    @Override
    ParseResult parse(String fileName, int lineNumber, InputStream in, Encoding encoding,
                             DynamicScope blockScope, int flags) {
        byte[] source = getSourceAsBytes(fileName, in);
        byte[] metadata = generateMetadata(fileName, lineNumber, encoding, blockScope, flags);
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
        ParserBindingPrism.Buffer buffer = new ParserBindingPrism.Buffer(jnr.ffi.Runtime.getRuntime(prismLibrary));
        prismLibrary.pm_buffer_init(buffer);
        prismLibrary.pm_serialize_parse(buffer, source, sourceLength, metadata);
        if (ParserManager.PARSER_TIMING) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addYARPTimeCParseSerialize(System.nanoTime() - time);
        }

        int length = buffer.length.intValue();
        byte[] src = new byte[length];
        buffer.value.get().get(0, src, 0, length);

        return src;
    }

    // lineNumber (0-indexed)
    private byte[] generateMetadata(String fileName, int lineNumber, Encoding encoding, DynamicScope scope, int flags) {
        ByteList metadata = new ByteList();

        // Filepath
        byte[] name = fileName.getBytes();
        appendUnsignedInt(metadata, name.length);
        metadata.append(name);

        // FIXME: I believe line number can be negative?
        // Line Number (1-indexed)
        appendUnsignedInt(metadata, lineNumber + 1);

        // Encoding
        name = encoding.getName();
        appendUnsignedInt(metadata, name.length);
        metadata.append(name);

        // frozen string literal
        metadata.append(runtime.getInstanceConfig().isFrozenStringLiteral() ? 1 : 0);

        // supress warnings
        metadata.append(runtime.getInstanceConfig().getVerbosity() == RubyInstanceConfig.Verbosity.NIL ? 0 : 1);

        // Eval scopes (or none for normal parses)
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
