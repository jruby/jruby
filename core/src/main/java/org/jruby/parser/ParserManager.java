package org.jruby.parser;

import org.jcodings.Encoding;

import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.ir.persistence.util.IRFileExpert;
import org.jruby.management.ParserStats;
import org.jruby.runtime.DynamicScope;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Front-end API to parsing Ruby source.
 * <p>
 * Notes:
 *   1. file parses can deserialize from IR but evals never do.
 */
public class ParserManager {
    private static final boolean PARSER_TIMING = Options.PARSER_SUMMARY.load();
    public static final int INLINE = 1;      // is it -e source
    public static final int DATA = 2;        // should we provide DATA for data after __END__
    public static final int EVAL = 4;

    private final Ruby runtime;

    private final Parser parser;

    // Parser stats
    private final ParserStats parserStats;

    public ParserManager(Ruby runtime) {
        this.runtime = runtime;
        parser = Options.PARSER_YARP.load() ? new YARPParser(runtime) : new Parser(runtime);
        parserStats = new ParserStats(runtime);
    }

    public Parser getParser() {
        return parser;
    }

    public ParseResult parseEval(String fileName, int lineNumber, String source, DynamicScope scope) {
        return parseEval(fileName, lineNumber, new ByteList(encodeToBytes(source), runtime.getDefaultEncoding()), scope);
    }

    public ParseResult parseEval(String fileName, int lineNumber, ByteList source, DynamicScope scope) {
        long nanos = 0;
        parserStats.addEvalParse();

        if (PARSER_TIMING) nanos = System.nanoTime();
        ParseResult result = parser.parse(fileName, lineNumber, source, scope, EVAL);
        if (PARSER_TIMING) parserStats.addParseTime(System.nanoTime() - nanos);

        return result;
    }

    public ParseResult parseFile(String fileName, int lineNumber, InputStream in, Encoding encoding, DynamicScope scope, int flags) {
        long nanos = 0;
        parserStats.addLoadParse();

        if (PARSER_TIMING) nanos = System.nanoTime();
        ParseResult result;
        if (RubyInstanceConfig.IR_READING) {
            result = loadFileFromIRPersistence(fileName, lineNumber, in, encoding, scope, flags);
        } else {
            result = parser.parse(fileName, lineNumber, in, encoding, scope, flags);
        }
        if (PARSER_TIMING) parserStats.addParseTime(System.nanoTime() - nanos);

        return result;
    }

    public ParseResult parseFile(String fileName, int lineNumber, ByteList source, DynamicScope scope, int flags) {
        parserStats.addLoadParse();

        if (RubyInstanceConfig.IR_READING) {
            InputStream in = new ByteArrayInputStream(source.getUnsafeBytes(), source.begin(), source.length());
            return loadFileFromIRPersistence(fileName, lineNumber, in, source.getEncoding(), scope, flags);
        } else {
            return parser.parse(fileName, lineNumber, source, scope, flags);
        }
    }

    public ParseResult loadFileFromIRPersistence(String fileName, int lineNumber, InputStream in, Encoding encoding, DynamicScope scope, int flags) {
        try {
            // Get IR from .ir file
            return IRReader.load(runtime.getIRManager(), new IRReaderStream(runtime.getIRManager(),
                    IRFileExpert.getIRPersistedFile(fileName), fileName));
        } catch (IOException e) {
            // FIXME: What if something actually throws IOException
            return parseFile(fileName, lineNumber, in, encoding, scope, flags);
        }
    }

    // flag methods

    // FIXME: This is in ripper but I don't think it needs to be.
    public static boolean isEval(int flags) {
        return (flags & ParserManager.EVAL) != 0;
    }

    static boolean isInline(int flags) {
        return (flags & ParserManager.INLINE) != 0;
    }

    static boolean isSaveData(int flags) {
        return (flags & ParserManager.DATA) != 0;
    }

    // Parser stats methods

    public ParserStats getParserStats() {
        return parserStats;
    }

    private byte[] encodeToBytes(String string) {
        Charset charset = runtime.getDefaultCharset();

        return charset == null ? string.getBytes() : string.getBytes(charset);
    }

    public long getTotalBytes() {
        return parser.getTotalBytes();
    }

    public void printParserStatistics() {
        System.out.println("Parser Statistics:");
        System.out.println("  files parsed: " + parserStats.getNumberOfLoadParses());
        System.out.println("  evals parsed: " + parserStats.getNumberOfEvalParses());
        System.out.println("  time spent(s): " + parserStats.getTotalParseTime());
    }
}
