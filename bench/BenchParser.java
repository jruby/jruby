
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jruby.Ruby;

import org.jruby.runtime.load.LoadServiceResourceInputStream;

public class BenchParser {
    public static final int N = 1000;
    public static void main(String[] args) {
        try {
            String file = args[0];
            InputStream in = getContents(file);
            int iterations = getIterations(args);

            System.out.println("Parsing " + file + " " + iterations + " times");
            Ruby runtime = Ruby.getGlobalRuntime();
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                long start2 = System.nanoTime();
                runtime.parseFile(file, in, null);
                System.out.println("Parse took: " + seconds(start2, System.nanoTime()) + "ms");
            }
            System.out.println("Total parse took: " + seconds(start, System.nanoTime()) + "ms");
        } catch (Exception e) {
            System.err.println("Parse bench failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static double seconds(long start, long end) {
        return (end - start) * 1.0 / 1000000.0;
    }

    private static int getIterations(String[] args) {
        return args.length > 1 ? Integer.valueOf(args[1]) : N;
    }

    private static InputStream getContents(String file) throws IOException {
        return new LoadServiceResourceInputStream(new FileInputStream(file));
    }
}
