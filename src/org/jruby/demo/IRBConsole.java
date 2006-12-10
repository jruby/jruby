package org.jruby.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.io.PipedInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBConsole extends JFrame {
    public IRBConsole(String title) {
        super(title);
    }

    public static void main(String[] args) {
        final IRBConsole console = new IRBConsole("JRuby IRB Console");
        PipedInputStream pipeIn = new PipedInputStream();
        
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
        
        TextAreaReadline tar = new TextAreaReadline(text, " Welcome to the JRuby IRB Console \n\n");
        
        final IRuby runtime = Ruby.newInstance(pipeIn, new PrintStream(tar),
                new PrintStream(tar), false);
        
        IRubyObject argumentArray = runtime.newArray(JavaUtil.convertJavaArrayToRuby(runtime, args));
        runtime.defineGlobalConstant("ARGV", argumentArray);
        runtime.getGlobalVariables().defineReadonly("$*", new ValueAccessor(argumentArray));
        runtime.getGlobalVariables().defineReadonly("$$", new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))));
        runtime.getLoadService().init(new ArrayList());
        
        tar.hookIntoRuntime(runtime);
        
        Thread t2 = new Thread() {
            public void run() {
                console.setVisible(true);
                runtime.evalScript("require 'irb'; require 'irb/completion'; IRB.start");
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
        if (font == null)
            font = new Font(otherwise, style, size);
        return font;
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 3746242973444417387L;

}
