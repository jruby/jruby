package org.jruby.parser;

import org.jcodings.Encoding;

import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyFile;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.ir.persistence.util.IRFileExpert;
import org.jruby.management.ParserStats;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadServiceResourceInputStream;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.jruby.parser.ParserType.*;

/**
 * Front-end API to parsing Ruby source.
 * <p>
 * Notes:
 *   1. file parses can deserialize from IR but evals never do.
 */
public class ParserManager {
    public static final boolean PARSER_WASM = Options.PARSER_WASM.load();

    public final boolean parserTiming = Options.PARSER_SUMMARY.load();

    private final Ruby runtime;

    private final Parser parser;

    // Parser stats
    private final ParserStats parserStats;

    public ParserManager(Ruby runtime) {
        this.runtime = runtime;
        ParserProvider provider;
        try {
            provider = ParserServiceLoader.provider(Options.PARSER_PRISM.load() || Options.PARSER_WASM.load());
            provider.initialize(runtime.getJRubyHome() + "/lib/libprism." + getLibraryExtension());
        } catch (UnsatisfiedLinkError e) {
            System.err.println("warning: Problem loading requested parser. Falling back to default parser.\nOriginal error message: " + e.getMessage());
            provider = new ParserProviderDefault();
        }
        runtime.getIRManager().setBuilderFactory(provider.getBuilderFactory());
        parser = provider.getParser(runtime);
        parserStats = new ParserStats(runtime);
    }

    private static String getLibraryExtension() {
        if (Platform.IS_WINDOWS) return "dll";
        if (Platform.IS_MAC) return "dylib";
        return "so";
    }

    public Parser getParser() {
        return parser;
    }

    /**
     * @param fileName the potentially relative path file
     * @param lineNumber the zero-indexed line
     * @param source the source
     * @param scope scope of the eval which for embedding can potentially be none
     * @return the parsed Ruby
     */
    public ParseResult parseEval(String fileName, int lineNumber, String source, DynamicScope scope) {
        return parseEval(fileName, lineNumber, new ByteList(encodeToBytes(source), runtime.getDefaultEncoding()), scope);
    }

    /**
     * @param fileName the potentially relative path file
     * @param lineNumber the zero-indexed line
     * @param source the source
     * @param scope scope of the eval
     * @return the parsed Ruby
     */
    public ParseResult parseEval(String fileName, int lineNumber, ByteList source, DynamicScope scope) {
        long nanos = 0;
        parserStats.addEvalParse();

        if (parserTiming) nanos = System.nanoTime();
        ParseResult result = parser.parse(fileName, lineNumber, source, scope, EVAL);
        if (parserTiming) parserStats.addParseTime(System.nanoTime() - nanos);

        return result;
    }

    /**
     * Parse main file (-e or main script file).  Other file parses should use parseFile.  Evals should use
     * parseEval.
     *
     * @param fileName   the potentially relative path file
     * @param lineNumber the zero-indexed line
     * @param in         the source
     * @param encoding   the encoding to treat the source unless magic comments intervene
     * @param scope      top-level binding in case of MAIN file (eval uses should call parseEval instead).
     * @param type       whether this is eval, is -e, or should worry about DATA
     * @return the parsed Ruby
     */
    public ParseResult parseMainFile(String fileName, int lineNumber, InputStream in, Encoding encoding, DynamicScope scope, ParserType type) {
        long nanos = 0;
        parserStats.addLoadParse();

        if (parserTiming) nanos = System.nanoTime();
        ParseResult result;
        if (RubyInstanceConfig.IR_READING) {
            result = loadFileFromIRPersistence(fileName, lineNumber, in, encoding, scope, type);
        } else {
            result = parser.parse(fileName, lineNumber, in, encoding, scope, type);
        }
        if (parserTiming) parserStats.addParseTime(System.nanoTime() - nanos);

        return result;
    }

    /**
     * Parse a (non-main) file.  Other file parses should use parseFile.  Evals should use parseEval.
     *
     * @param fileName the potentially relative path file
     * @param lineNumber the zero-indexed line
     * @param in the source
     * @param encoding the encoding to treat the source unless magic comments intervene
     * @return
     */
    public ParseResult parseFile(String fileName, int lineNumber, InputStream in, Encoding encoding) {
        return parseMainFile(fileName, lineNumber, in, encoding, null, NORMAL);
    }

    private ParseResult loadFileFromIRPersistence(String fileName, int lineNumber, InputStream in, Encoding encoding,
                                                  DynamicScope scope, ParserType type) {
        try {
            // Get IR from .ir file
            return IRReader.load(runtime.getIRManager(), new IRReaderStream(runtime.getIRManager(),
                    IRFileExpert.getIRPersistedFile(fileName), fileName));
        } catch (IOException e) {  // FIXME: What if something actually throws IOException
            if (type == MAIN || type == INLINE) {
                return parseMainFile(fileName, lineNumber, in, encoding, scope, type);
            } else {
                return parseFile(fileName, lineNumber, in, encoding);
            }
        }
    }

    // Parser stats methods

    public ParserStats getParserStats() {
        return parserStats;
    }

    private byte[] encodeToBytes(String string) {
        Charset charset = runtime.getDefaultCharset();

        return charset == null ? string.getBytes() : string.getBytes(charset);
    }

    public IRubyObject getLineStub(ThreadContext context, IRubyObject arg) {
        ByteList contents = ((RubyString) RubyFile.read(context, arg, new IRubyObject[] { arg }, Block.NULL_BLOCK)).getByteList();
        int begin = contents.begin();
        int length = contents.realSize();
        byte[] bytes = contents.unsafeBytes();

        int lineCount = 0;
        for (int i = begin; i < length; i++) {
            if (bytes[i] == '\n') lineCount++;
        }

        // FIXME: Semantic problem.  Linenumber affects both differently due to prism being 1-indexed and AST being 0-indexed.
        ParseResult result = parseFile("", Options.PARSER_PRISM.load() ? 0 : -1, new LoadServiceResourceInputStream(contents.bytes()), contents.getEncoding());
        return parser.getLineStub(context, result, lineCount);
    }

    // Modifies incoming source for -n, -p, and -F
    public ParseResult addGetsLoop(Ruby runtime, ParseResult oldRoot, boolean printing, boolean processLineEndings, boolean split) {
        return parser.addGetsLoop(runtime, oldRoot, printing, processLineEndings, split);
    }
}
