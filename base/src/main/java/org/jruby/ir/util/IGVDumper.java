package org.jruby.ir.util;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.IGVCFGVisitor;

import static org.jruby.ir.util.IGVHelper.endTag;
import static org.jruby.ir.util.IGVHelper.property;
import static org.jruby.ir.util.IGVHelper.startTag;

/**
 * Created by enebo on 1/28/17.
 */
public class IGVDumper {
    static final String HOST = "localhost";
    static final int PORT = 4444;
    Socket socket;
    PrintStream writer;
    final String baseLabel;

    public IGVDumper(String baseLabel) {
        this.baseLabel = baseLabel;

        try {
            socket = new Socket(HOST, PORT);
            writer = new PrintStream(socket.getOutputStream());
            startTag(writer, "graphDocument");
            startTag(writer, "group");
            startTag(writer, "properties");
            property(writer, "name", baseLabel);
            endTag(writer, "properties");

        } catch (IOException e) {
        }
    }

    public void dump(CFG cfg, String name) {
        new IGVCFGVisitor(cfg, writer, name);
    }

    public void close() {
        endTag(writer, "group");
        endTag(writer, "graphDocument");
        try {
            writer.close();
            socket.close();
        } catch (IOException e) {
        }
    }
}
