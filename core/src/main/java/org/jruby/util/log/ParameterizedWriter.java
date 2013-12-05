package org.jruby.util.log;

import java.io.PrintStream;
import java.io.PrintWriter;

class ParameterizedWriter {


    private PrintStream stream;

    ParameterizedWriter(PrintStream stream) {
        this.stream = stream;
    }

    public void write(String message, Object... args) {
        final StringBuilder builder = new StringBuilder();
        if (message != null) {
            final String[] strings = message.split("\\{\\}");
            if (args.length == 0 || strings.length == args.length) {
                for (int i = 0; i < strings.length; i++) {
                    builder.append(strings[i]);
                    if (args.length > 0) {
                        builder.append(args[i]);
                    }
                }
            } else if (strings.length == 0 && args.length == 1) {
                builder.append(args[0]);
            } else {
                stream.println("wrong number of placeholders / arguments");
            }
        }
        stream.println(builder.toString());
    }

    public PrintStream getStream() {
        return stream;
    }
}
