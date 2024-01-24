package org.prism;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// @formatter:off
public abstract class ParsingOptions {
    /** Serialize parsing options into byte array.
     *
     * @param filepath the name of the file that is currently being parsed
     * @param line the line within the file that the parser starts on. This value is 0-indexed
     * @param encoding the name of the encoding that the source file is in
     * @param frozenStringLiteral whether the frozen string literal option has been set
     * @param verbose whether the parser emits warnings
     * @param version code of Ruby version which syntax will be used to parse
     * @param scopes scopes surrounding the code that is being parsed with local variable names defined in every scope
     *            ordered from the outermost scope to the innermost one */
    public static byte[] serialize(byte[] filepath, int line, byte[] encoding, boolean frozenStringLiteral,
            boolean verbose, byte version, byte[][][] scopes) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        // filepath
        write(output, serializeInt(filepath.length));
        write(output, filepath);

        // line
        write(output, serializeInt(line));

        // encoding
        write(output, serializeInt(encoding.length));
        write(output, encoding);

        // frozenStringLiteral
        if (frozenStringLiteral) {
            output.write(1);
        } else {
            output.write(0);
        }

        // verbose
        boolean suppressWarnings = !verbose;
        if (suppressWarnings) {
            output.write(1);
        } else {
            output.write(0);
        }

        // version
        output.write(version);

        // scopes

        // number of scopes
        write(output, serializeInt(scopes.length));
        // local variables in each scope
        for (byte[][] scope : scopes) {
            // number of locals
            write(output, serializeInt(scope.length));

            // locals
            for (byte[] local : scope) {
                write(output, serializeInt(local.length));
                write(output, local);
            }
        }

        return output.toByteArray();
    }

    private static void write(ByteArrayOutputStream output, byte[] bytes) {
        // Note: we cannot use output.writeBytes(local) because that's Java 11
        output.write(bytes, 0, bytes.length);
    }

    private static byte[] serializeInt(int n) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(n);
        return buffer.array();
    }
}
// @formatter:on
