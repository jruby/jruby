package org.jruby.ir.util;

import java.io.PrintStream;

import static org.jruby.ir.util.IGVDumper.sanitize;

/**
 * Created by enebo on 1/28/17.
 */
public class IGVHelper {
    public static void property(PrintStream writer, String name, Object content) {
        startTag(writer, "p", "name", name);
        writer.print(sanitize(content.toString()));
        endTag(writer, "p");
    }

    public static void emptyTag(PrintStream writer, String name, Object... attributes) {
        writer.print("<" + name + " ");
        for (int i = 0; i < attributes.length; i += 2) {
            writer.print(sanitize(attributes[i].toString()));
            writer.print("=\"");
            writer.print(sanitize(attributes[i+1].toString()));
            writer.print("\" ");
        }
        writer.println("/>");
    }


    public static void endTag(PrintStream writer, String name) {
        writer.println("</" + name + ">");
    }

    public static void startTag(PrintStream writer, String name) {
        writer.println("<" + name + ">");
    }

    public static void startTag(PrintStream writer, String name, Object... attributes) {
        writer.print("<" + name + " ");
        for (int i = 0; i < attributes.length; i += 2) {
            writer.print(sanitize(attributes[i].toString()));
            writer.print("=\"");
            writer.print(sanitize(attributes[i+1].toString()));
            writer.print("\" ");
        }
        writer.println(">");
    }
}
