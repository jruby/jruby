package org.jruby.parser;

import jnr.ffi.LibraryLoader;
import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.lexer.LexerSource;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.yarp.Nodes;
import org.yarp.YarpParseResult;

import java.io.InputStream;

public class YARPParser extends Parser {
    YARPParserBindings yarpLibrary = LibraryLoader.create(YARPParserBindings.class).load("yarp");

    public YARPParser(Ruby runtime) {
        super(runtime);
    }

    // FIXME: lots of missing stuff.
    public ParseResult parse(String fileName, int lineNumber, InputStream in, DynamicScope blockScope, int flags) {
        byte[] source = getSourceAsBytes(in);
        byte[] metadata = new byte[0];
        byte[] serialized = parse(source, metadata);
        org.yarp.ParseResult res = org.yarp.Loader.load(source, new Nodes.Source(serialized));

        ParseResult result = new YarpParseResult(fileName, source, (Nodes.ProgramNode) res.value);
        if (blockScope != null) {
            result.getStaticScope().setEnclosingScope(blockScope.getStaticScope());
        }

        return result;
    }

    private byte[] getSourceAsBytes(InputStream in) {
        return ((LoadServiceResourceInputStream) in).getBytes();
    }
    // FIXME: metadata is formed via some other method and how this is packaged as API needs to be figured out.
    private byte[] parse(byte[] source, byte[] metadata) {
        byte[] serialized = new byte[0];
        yarpLibrary.parseAndSerialize(source, source.length, serialized, metadata);
        return serialized;
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
