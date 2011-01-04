
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jruby.Ruby;

import org.jruby.RubyInstanceConfig;
import org.jruby.common.NullWarnings;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.ParserConfiguration;

public class BenchParser {
    public static final int N = 1000;
    public static void main(String[] args) {
        try {
            byte[] bytes = getContents(args);
            int iterations = getIterations(args);
            int[] parsers = getParsers(args);

            System.out.println("Parsing " + args[0] + " " + iterations + " times");
            DefaultRubyParser parser = new DefaultRubyParser();
            parser.setWarnings(new NullWarnings(null));
            Ruby runtime = Ruby.getGlobalRuntime();
            RubyInstanceConfig rconfig = new RubyInstanceConfig();
            ParserConfiguration config = new ParserConfiguration(runtime, 0, false, false, true, rconfig);

            for (int x = 0; x < parsers.length; x++) {
                System.out.println("Benching parse with " + (parsers[x] == 0 ? "InputStream" : "ByteArray") + "LexerSource");
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    parser.parse(config, getLexerSource(args[0], bytes, config, parsers[x] == 0));
                }
                System.out.println("Parse took: " + ((System.nanoTime() - start) * 1.0 / 1000000.0) + "ms");
            }
        } catch (Exception e) {
            System.err.println("Parse bench failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static final LexerSource getLexerSource(String filename, byte[] bytes, ParserConfiguration config, boolean useStream) {
        if (useStream) {
            return LexerSource.getSource(filename, new ByteArrayInputStream(bytes), null, config);
        } else {
            return LexerSource.getSource(filename, bytes, null, config);
        }
    }

    private static int getIterations(String[] args) {
        if (args.length > 1) {
            return Integer.valueOf(args[1]);
        } else {
            return N;
        }
    }

    private static int[] getParsers(String[] args) {
        if (args.length > 2) {
            String[] strs = args[2].split(",");
            int[] parsers = new int[strs.length > 2 ? 2 : strs.length];
            for (int i = 0; i < parsers.length; i++) {
                parsers[i] = Integer.valueOf(strs[i]) - 1;
            }
            return parsers;
        } else {
            return new int[] {0, 1};
        }
    }

    private static byte[] getContents(String[] args) throws IOException {
        InputStream contents = new FileInputStream(args[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bytesRead = 0;
        byte[] buf = new byte[16384];
        while ((bytesRead = contents.read(buf)) > 0) {
            out.write(buf, 0, bytesRead);
        }
        contents.close();
        return out.toByteArray();
    }
}
