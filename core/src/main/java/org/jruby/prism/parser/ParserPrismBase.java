package org.jruby.prism.parser;

import org.jcodings.Encoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFile;
import org.jruby.RubyInstanceConfig;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.management.ParserStats;
import org.jruby.parser.Parser;
import org.jruby.parser.ParserType;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.jruby.util.io.SeekableByteChannelImpl;
import org.ruby_lang.prism.Nodes.ProgramNode;
import org.ruby_lang.prism.ParsingOptions;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.IntStream;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.parser.ParserType.EVAL;
import static org.jruby.parser.ParserType.INLINE;
import static org.jruby.parser.ParserType.MAIN;

public abstract class ParserPrismBase extends Parser {
    protected boolean parserTiming = org.jruby.util.cli.Options.PARSER_SUMMARY.load();

    public ParserPrismBase(Ruby runtime) {
        super(runtime);
    }

    protected abstract byte[] parse(byte[] source, int sourceLength, byte[] metadata);

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

        if (parserTiming) time = System.nanoTime();
        LoaderPrism loader = new LoaderPrism(runtime, serialized);
        org.ruby_lang.prism.ParseResult res = loader.load();
        Encoding encoding = loader.getEncoding();

        if (parserTiming) {
            ParserStats stats = runtime.getParserManager().getParserStats();

            stats.addPrismTimeDeserializing(System.nanoTime() - time);
            stats.addPrismSerializedBytes(serialized.length);
            stats.addParsedBytes(source.length);
        }

        if (res.warnings != null) {
            for (org.ruby_lang.prism.ParseResult.Warning warning: res.warnings) {
                if (warning.level != org.ruby_lang.prism.ParseResult.WarningLevel.WARNING_VERBOSE || runtime.isVerbose()) {
                    runtime.getWarnings().warn(fileName, res.source.line(warning.location.startOffset), warning.message);
                }
            }
        }

        if (res.errors != null && res.errors.length > 0) {
            int line = res.source.line(res.errors[0].location.startOffset);

            throw runtime.newSyntaxError(fileName + ":" + line + ": " + res.errors[0].message, fileName);
        }

        if (type == MAIN && res.dataLocation != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(source, 0, source.length);
            bais.skip(res.dataLocation.startOffset + 8); // FIXME: 8 is for including __END__\n
            runtime.defineDATA(RubyFile.DATAFile(runtime, fileName, new SeekableByteChannelImpl(bais)));
        }

        int lineCount = res.source.getLineCount();
        RubyArray lines = getLines(type == EVAL, fileName, lineCount);
        if (lines != null) {  // SCRIPT_DATA__ exists we need source filled in for this parse
            populateScriptData(source, encoding, lines);
        }

        int coverageMode = CoverageData.NONE;

        if (type != EVAL && runtime.getCoverageData().isCoverageEnabled()) {
            int[] coverage = new int[lineCount - 1];
            Arrays.fill(coverage, -1);
            CoverageLineVisitor visitor = new CoverageLineVisitor(res.source, coverage);
            visitor.defaultVisit(res.value);
            runtime.getCoverageData().prepareCoverage(fileName, coverage);
            coverageMode = runtime.getCoverageData().getMode();
        }

        ParseResultPrism result = new ParseResultPrism(runtime, fileName, source, (ProgramNode) res.value, res.source, encoding, coverageMode);
        if (blockScope != null) {
            if (type == MAIN) { // update TOPLEVEL_BINDNG
                byte[][] locals = ((ProgramNode) result.getAST()).locals;
                var context = runtime.getCurrentContext();
                for (int i = 0; i < locals.length; i++) {
                    blockScope.getStaticScope().addVariableThisScope(asSymbol(context, new ByteList(locals[i], encoding)).idString());
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
            throw runtime.newSyntaxError("Failed to read source file: " + fileName, fileName);
        }
    }

    // lineNumber (0-indexed)
    private byte[] generateMetadata(String fileName, int lineNumber, Encoding encoding, DynamicScope scope, ParserType type) {
        return ParsingOptions.serialize(
                fileName.getBytes(),
                lineNumber + 1,
                encoding.getName(),
                (runtime.getInstanceConfig().isFrozenStringLiteral() instanceof Boolean bool && bool),
                commandLineFromConfig(runtime.getInstanceConfig(), type == MAIN || type == INLINE),
                ParsingOptions.SyntaxVersion.V4_0,
                false,
                true,
                false,
                evalScopes(scope));
    }

    private ParsingOptions.Scope[] evalScopes(DynamicScope scope) {
        if (scope == null) return new ParsingOptions.Scope[0];

        var scopes = new ArrayList<ParsingOptions.Scope>();

        evalScopesRecursive(scope.getStaticScope(), scopes);

        return scopes.toArray(ParsingOptions.Scope[]::new);
    }

    private void evalScopesRecursive(StaticScope scope, ArrayList<ParsingOptions.Scope> scopes) {
        if (scope.getEnclosingScope() != null && scope.isBlockScope()) {
            evalScopesRecursive(scope.getEnclosingScope(), scopes);
        }

        var variables = scope.getVariables();

        scopes.add(new ParsingOptions.Scope(
                Arrays
                        .stream(variables)
                        .map(String::getBytes)
                        .toArray(byte[][]::new),
                IntStream
                        .range(0, scope.getVariables().length)
                        .mapToObj((i) -> {
                            var name = variables[i];
                            return switch (name) {
                                case "..." -> ParsingOptions.Forwarding.ALL;
                                case "*" -> ParsingOptions.Forwarding.POSITIONAL;
                                case "**" -> ParsingOptions.Forwarding.KEYWORD;
                                case "&" -> ParsingOptions.Forwarding.BLOCK;
                                default -> ParsingOptions.Forwarding.NONE;
                            };
                        })
                        .toArray(ParsingOptions.Forwarding[]::new)));
    }

    private EnumSet<ParsingOptions.CommandLine> commandLineFromConfig(RubyInstanceConfig config, boolean isMain) {
        var list = new ArrayList<ParsingOptions.CommandLine>();

        if (isMain) {
            if (config.isSplit()) list.add(ParsingOptions.CommandLine.A); // -a
            if (config.isInlineScript()) list.add(ParsingOptions.CommandLine.E);    // -e
            if (config.isProcessLineEnds()) list.add(ParsingOptions.CommandLine.L); // -l
            if (config.isAssumeLoop()) list.add(ParsingOptions.CommandLine.N);      // -n
            if (config.isAssumePrinting()) list.add(ParsingOptions.CommandLine.P); // -p
            if (config.isXFlag()) list.add(ParsingOptions.CommandLine.X);          // -x
        }

        return list.isEmpty() ? EnumSet.noneOf(ParsingOptions.CommandLine.class) : EnumSet.copyOf(list);
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

    private void encodeEvalScopes(ByteList buf, StaticScope scope) {
        int startIndex = buf.realSize();

        // append uint 0 to reserve the space
        appendUnsignedInt(buf, 0);

        // write the scopes to the buffer
        int count = encodeEvalScopesInner(buf, scope, 1);

        // overwrite int 0 with scope count
        writeUnsignedInt(buf, startIndex, count);
    }

    private int encodeEvalScopesInner(ByteList buf, StaticScope scope, int count) {
        if (scope.getEnclosingScope() != null && scope.isBlockScope()) {
            count = encodeEvalScopesInner(buf, scope.getEnclosingScope(), count + 1);
        }

        // once more for method scope
        String names[] = scope.getVariables();

        // number of variables
        appendUnsignedInt(buf, names.length);

        // forwarding flags
        buf.append(0);

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

    // Prism generates this for us.
    @Override
    public ParseResult addGetsLoop(Ruby runtime, ParseResult result, boolean printing, boolean processLineEndings, boolean split) {
        return result;
    }
}
