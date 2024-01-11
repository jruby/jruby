package org.jruby.parser;

import org.jcodings.Encoding;

import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.ir.persistence.util.IRFileExpert;
import org.jruby.management.ParserStats;
import org.jruby.prism.parser.ParserPrism;
import org.jruby.runtime.DynamicScope;
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
    public static final boolean PARSER_TIMING = Options.PARSER_SUMMARY.load();

    private final Ruby runtime;

    private final Parser parser;

    // Parser stats
    private final ParserStats parserStats;

    public ParserManager(Ruby runtime) {
        this.runtime = runtime;
        parser = Options.PARSER_PRISM.load() ? new ParserPrism(runtime) : new Parser(runtime);
        parserStats = new ParserStats(runtime);
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

        if (PARSER_TIMING) nanos = System.nanoTime();
        ParseResult result = parser.parse(fileName, lineNumber, source, scope, EVAL);
        if (PARSER_TIMING) parserStats.addParseTime(System.nanoTime() - nanos);

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

        if (PARSER_TIMING) nanos = System.nanoTime();
        ParseResult result;
        if (RubyInstanceConfig.IR_READING) {
            result = loadFileFromIRPersistence(fileName, lineNumber, in, encoding, scope, type);
        } else {
            result = parser.parse(fileName, lineNumber, in, encoding, scope, type);
        }
        if (PARSER_TIMING) parserStats.addParseTime(System.nanoTime() - nanos);

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
}
