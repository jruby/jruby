package org.jruby.demo;

import java.applet.Applet;
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
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBApplet extends Applet {

    public void start() {
        super.start();

        final PipedInputStream pipeIn = new PipedInputStream();
        
        setLayout(new BorderLayout());
        
        JEditorPane text = new JTextPane();
        
        text.setMargin(new Insets(8,8,8,8));
        text.setCaretColor(new Color(0xa4, 0x00, 0x00));
        text.setBackground(new Color(0xf2, 0xf2, 0xf2));
        text.setForeground(new Color(0xa4, 0x00, 0x00));
        Font font = findFont("Monospaced", Font.PLAIN, 14,
                new String[] {"Monaco", "Andale Mono"});
        
        text.setFont(font);
        JScrollPane pane = new JScrollPane();
        pane.setViewportView(text);
        pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        add(pane);
        this.validate();
        
        final TextAreaReadline tar = new TextAreaReadline(text, " Welcome to JRuby for the Web! \n\n");
        
        final RubyInstanceConfig config = new RubyInstanceConfig() {{
            setInput(pipeIn);
            setOutput(new PrintStream(tar));
            setError(new PrintStream(tar));
            setObjectSpaceEnabled(false);
        }};
        
        Ruby.setSecurityRestricted(true); // We are in a very limited context. Tighten that belt...
        
        final Ruby runtime = Ruby.newInstance(config);
        
        runtime.defineGlobalConstant("ARGV", runtime.newArrayNoCopy(new IRubyObject[] {
                runtime.newString("-f") }));
        runtime.getLoadService().init(new ArrayList(0));
        
        tar.hookIntoRuntime(runtime);
        
        
        Thread t2 = new Thread() {
            public void run() {
                runtime.evalScript("require 'irb'; require 'irb/completion'; IRB.start");
            }
        };
        t2.start();
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
