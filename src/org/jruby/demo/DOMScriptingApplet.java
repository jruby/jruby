package org.jruby.demo;

import java.applet.Applet;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import netscape.javascript.JSObject;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import com.sun.java.browser.dom.DOMService;
import com.sun.java.browser.dom.DOMUnsupportedException;

public class DOMScriptingApplet extends Applet {

    public void start() {
        super.start();

        try {
            final PipedInputStream pipeIn = new PipedInputStream();
            final PipedOutputStream out = new PipedOutputStream(pipeIn);
            final PipedInputStream in = new PipedInputStream();
            final PipedOutputStream pipeOut = new PipedOutputStream(in);
            final IRuby runtime = Ruby.newInstance(pipeIn, new PrintStream(pipeOut),
                    new PrintStream(pipeOut), false);

            runtime.defineGlobalConstant("ARGV", runtime.newArray());
            //runtime.defineGlobalConstant("JSObject", JavaEmbedUtils.javaToRuby(runtime, JSObject.getWindow(this)));
            runtime.getLoadService().init(new ArrayList(0));
            
            final String script = getParameter("script");

            Thread t2 = new Thread() {
                public void run() {
                    runtime.evalScript(script);
                }
            };
            t2.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 3746242973444417387L;

}
