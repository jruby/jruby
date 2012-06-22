package org.jruby.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.internal.runtime.ValueAccessor;

public class IRBConsole extends JFrame {
    public IRBConsole(String title) {
        super(title);
    }

    public static void main(final String[] args) {
        final IRBConsole console = new IRBConsole("JRuby IRB Console");
        final ArrayList<String> list = new ArrayList(Arrays.asList(args));

        console.getContentPane().setLayout(new BorderLayout());
        console.setSize(700, 600);

        JEditorPane text = new JTextPane();

        text.setMargin(new Insets(8,8,8,8));
        text.setCaretColor(new Color(0xa4, 0x00, 0x00));
        text.setBackground(new Color(0xf2, 0xf2, 0xf2));
        text.setForeground(new Color(0xa4, 0x00, 0x00));
        Font font = console.findFont("Monospaced", Font.PLAIN, 14,
                new String[] {"Monaco", "Andale Mono"});

        text.setFont(font);
        JScrollPane pane = new JScrollPane();
        pane.setViewportView(text);
        pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        console.getContentPane().add(pane);
        console.validate();

        final TextAreaReadline tar = new TextAreaReadline(text, " Welcome to the JRuby IRB Console \n\n");
        console.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                tar.shutdown();
            }
        });

        final RubyInstanceConfig config = new RubyInstanceConfig() {{
            CompatVersion compat = CompatVersion.RUBY1_8;
            if (args.length > 0) {
                if (args[0].equals("1.8")) {
                    list.remove(0);
                } else if (args[0].equals("1.9")) {
                    compat = CompatVersion.RUBY1_9;
                    list.remove(0);
                }
            }
            setInput(tar.getInputStream());
            setOutput(new PrintStream(tar.getOutputStream()));
            setError(new PrintStream(tar.getOutputStream()));
            setArgv(list.toArray(new String[0]));
            setCompatVersion(compat);
        }};
        final Ruby runtime = Ruby.newInstance(config);

        runtime.getGlobalVariables().defineReadonly("$$", new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))));

        tar.hookIntoRuntime(runtime);

        Thread t2 = new Thread() {
            public void run() {
                console.setVisible(true);
                runtime.evalScriptlet(
                        "ARGV << '--readline' << '--prompt' << 'inf-ruby';"
                        + "require 'irb'; require 'irb/completion';"
                        + "IRB.start");
            }
        };
        t2.start();

        try {
            t2.join();
        } catch (InterruptedException ie) {
            // ignore
        }

        System.exit(0);
    }

    private Font findFont(String otherwise, int style, int size, String[] families) {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Arrays.sort(fonts);
        Font font = null;
        for (int i = 0; i < families.length; i++) {
            if (Arrays.binarySearch(fonts, families[i]) >= 0) {
                font = new Font(families[i], style, size);
                break;
            }
        }
        if (font == null) {
            font = new Font(otherwise, style, size);
        }
        return font;
    }

    /**
     *
     */
    private static final long serialVersionUID = 3746242973444417387L;

}
