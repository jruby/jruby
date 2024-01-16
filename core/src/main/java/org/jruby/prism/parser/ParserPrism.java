package org.jruby.prism.parser;

import jnr.ffi.LibraryLoader;
import org.jcodings.Encoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyIO;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.management.ParserStats;
import org.jruby.parser.Parser;
import org.jruby.parser.ParserManager;
import org.jruby.parser.ParserType;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.io.ChannelHelper;
import org.prism.Nodes;
import org.prism.Nodes.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jruby.lexer.LexingCommon.DOLLAR_UNDERSCORE;
import static org.jruby.parser.ParserType.EVAL;
import static org.jruby.parser.ParserType.MAIN;

public class ParserPrism extends Parser {
    ParserBindingPrism prismLibrary;

    public ParserPrism(Ruby runtime) {
        super(runtime);

        String path = runtime.getInstanceConfig().getJRubyHome() + "/lib/prism.so";
        //System.out.println("Binding to " + path);
        prismLibrary = LibraryLoader.create(ParserBindingPrism.class).load(path);
    }
    // FIXME: error/warn when cannot bind to yarp (probably silent fail-over option too)

    @Override
    public ParseResult parse(String fileName, int lineNumber, ByteList content, DynamicScope existingScope, ParserType type) {
        int sourceLength = content.realSize();
        byte[] source = content.begin() == 0 ? content.unsafeBytes() : content.bytes();
        byte[] metadata = generateMetadata(fileName, lineNumber, content.getEncoding(), existingScope, type);
        byte[] serialized = parse(source, sourceLength, metadata);
        return parseInternal(fileName, existingScope, source, serialized, type);
    }

    private ParseResult parseInternal(String fileName, DynamicScope blockScope, byte[] source, byte[] serialized, ParserType type) {
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

        if (type == MAIN && res.dataLocation != null) {
            // FIXME: Intentionally leaving as original source for offset.  This can just be an IO where pos is set to right value.
            // FIXME: Somehow spec will say this should File and not IO but I cannot figure out why legacy parser isn't IO also.
            ByteArrayInputStream bais = new ByteArrayInputStream(source, 0, source.length);
            bais.skip(res.dataLocation.startOffset + 8); // FIXME: 8 is for including __END__\n
            runtime.defineDATA(RubyIO.newIO(runtime, ChannelHelper.readableChannel(bais)));
        }

        int lineCount = nodeSource.getLineCount();
        RubyArray lines = getLines(type == EVAL, fileName, lineCount);
        if (lines != null) {  // SCRIPT_DATA__ exists we need source filled in for this parse
            populateScriptData(source, encoding, lines);
        }

        int coverageMode = CoverageData.NONE;

        if (type != EVAL && runtime.getCoverageData().isCoverageEnabled()) {
            int[] coverage = new int[lineCount - 1];
            Arrays.fill(coverage, -1);
            CoverageLineVisitor visitor = new CoverageLineVisitor(nodeSource, coverage);
            visitor.defaultVisit(res.value);
            runtime.getCoverageData().prepareCoverage(fileName, coverage);
            coverageMode = runtime.getCoverageData().getMode();
        }

        ParseResultPrism result = new ParseResultPrism(fileName, source, (Nodes.ProgramNode) res.value, nodeSource, encoding, coverageMode);
        if (blockScope != null) {
            if (type == MAIN) { // update TOPLEVEL_BINDNG
                RubySymbol[] locals = ((Nodes.ProgramNode) result.getAST()).locals;
                for (int i = 0; i < locals.length; i++) {
                    blockScope.getStaticScope().addVariableThisScope(locals[i].idString());
                }
                blockScope.growIfNeeded();
                result.setDynamicScope(blockScope);
            } else {
                result.getStaticScope().setEnclosingScope(blockScope.getStaticScope());
            }
        }

        return result;
    }

    private void populateScriptData(byte[] source, Encoding encoding, RubyArray lines) {
        int begin = 0;
        int lineNumber = 0;
        for (int i = 0; i < source.length; i++) {
            if (source[i] == '\n') {
                ByteList line = new ByteList(source, begin, i - begin + 1);
                line.setEncoding(encoding);
                lines.aset(runtime.newFixnum(lineNumber), runtime.newString(line));
                begin = i + 1;
                lineNumber++;
            }
        }
    }

    @Override
    protected ParseResult parse(String fileName, int lineNumber, InputStream in, Encoding encoding,
                      DynamicScope existingScope, ParserType type) {
        byte[] source = getSourceAsBytes(fileName, in);
        byte[] metadata = generateMetadata(fileName, lineNumber, encoding, existingScope, type);
        byte[] serialized = parse(source, source.length, metadata);
        return parseInternal(fileName, existingScope, source, serialized, type);
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
    private byte[] generateMetadata(String fileName, int lineNumber, Encoding encoding, DynamicScope scope, ParserType type) {
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
        metadata.append(runtime.getInstanceConfig().getVerbosity() == RubyInstanceConfig.Verbosity.NIL ? 1 : 0);

        // FIXME: versioning seems to be potentially in-flux 5 == 3.3 (make enum once we know this)
        metadata.append(5);

        // Eval scopes (or none for normal parses)
        if (type == EVAL) {
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
        int count = encodeEvalScopesInner(buf, scope, 1);
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
            // Get the bytes "raw" (which we use ISO8859_1 for) as this is how we record these in StaticScope.
            byte[] bytes = name.getBytes(ISO8859_1Encoding.INSTANCE.getCharset());
            appendUnsignedInt(buf, bytes.length);
            buf.append(bytes);
        }

        return count;
    }

    public IRubyObject getLineStub(ThreadContext context, ParseResult arg, int lineCount) {
        ParseResultPrism result = (ParseResultPrism) arg;
        int[] lines = new int[lineCount];
        Arrays.fill(lines, -1);
        CoverageLineVisitor lineVisitor = new CoverageLineVisitor(result.nodeSource, lines);
        lineVisitor.defaultVisit(result.root);
        RubyArray lineStubs = context.runtime.newArray(lineCount);

        for (int i = 0; i < lines.length; i++) {
            if (lines[i] == 0) {
                lineStubs.set(i, context.runtime.newFixnum(0));
            } else {
                lineStubs.set(i, context.runtime.getNil());
            }
        }

        return lineStubs;
    }

    // It looks weird to see 0 everywhere but these are all virtual instrs and if they raise during execution it will
    // show it happening on line 1 (which is what it should do).
    @Override
    public ParseResult addGetsLoop(Ruby runtime, ParseResult result, boolean printing, boolean processLineEndings, boolean split) {
        List<Nodes.Node> newBody = new ArrayList<>();

        if (processLineEndings) {
            newBody.add(new Nodes.GlobalVariableWriteNode(runtime.newSymbol(CommonByteLists.DOLLAR_BACKSLASH),
                    new GlobalVariableReadNode(runtime.newSymbol(CommonByteLists.DOLLAR_SLASH), 0, 0), 0, 0));
        }

        Nodes.GlobalVariableReadNode dollarUnderscore = new GlobalVariableReadNode(runtime.newSymbol(DOLLAR_UNDERSCORE), 0, 0);

        List<Nodes.Node> whileBody = new ArrayList<>();

        if (processLineEndings) {
            whileBody.add(new CallNode((short) 0, dollarUnderscore, runtime.newSymbol("chomp!"), null, null, 0, 0));
        }
        if (split) {
            whileBody.add(new GlobalVariableWriteNode(runtime.newSymbol("$F"),
                    new Nodes.CallNode((short) 0, dollarUnderscore, runtime.newSymbol("split"), null, null, 0, 0), 0, 0));
        }

        StatementsNode stmts = ((ProgramNode) result.getAST()).statements;
        if (stmts != null && stmts.body != null) whileBody.addAll(Arrays.asList(stmts.body));

        ArgumentsNode args = new ArgumentsNode((short) 0, new Node[] { dollarUnderscore }, 0, 0);
        if (printing) whileBody.add(new CallNode((short) 0, null, runtime.newSymbol("print"), args, null, 0, 0));

        Node[] nodes = new Node[whileBody.size()];
        whileBody.toArray(nodes);
        StatementsNode statements = new StatementsNode(nodes, 0, 0);

        newBody.add(new WhileNode((short) 0,
                new CallNode(CallNodeFlags.VARIABLE_CALL, null, runtime.newSymbol("gets"), null, null, 0, 0),
                statements, 0, 0));

        nodes = new Node[newBody.size()];
        newBody.toArray(nodes);
        Nodes.ProgramNode newRoot = new Nodes.ProgramNode(new RubySymbol[] {}, new StatementsNode(nodes, 0, 0), 0, 0);

        ((ParseResultPrism) result).setRoot(newRoot);

        return result;
    }
}
