/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Charles Nutter <charles.o.nutter@sun.com>
 * Copyright (C) 2008 MenTaLguY <mental@rydia.net>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.VolatileImage;
import java.io.InputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;

import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.anno.JRubyMethod;
import org.jruby.demo.TextAreaReadline;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

/**
 * @author <a href="mailto:mental@rydia.net">MenTaLguY</a>
 *
 * The JRubyApplet class provides a simple way to write Java applets using
 * JRuby without needing to create a custom Java applet class.  At applet
 * initialization time, JRubyApplet starts up a JRuby runtime, then evaluates
 * the scriptlet given as the "eval" applet parameter.
 *
 * The Java applet instance is available to the Ruby script as
 * JRUBY_APPLET; the script can define callbacks for applet start, stop,
 * and destroy by passing blocks to JRUBY_APPLET.on_start,
 * JRUBY_APPLET.on_stop, and JRUBY_APPLET.on_destroy, respectively.
 *
 * Ruby code can install a custom paint callback using JRUBY_APPLET.on_paint
 * (the Graphics2D object is passed as an argument to the callback).  By
 * default, JRubyApplet painting is double-buffered, but you can select
 * single-buffered painting via JRUBY_APPLET.double_buffered = false.
 *
 * The applet's background color can be set via JRUBY_APPLET.background_color=.
 * You may want to set it to nil if you're not using double-buffering, so that
 * no background color will be drawn (your own paint code is then responsible
 * for filling the area).
 *
 * Beyond these things, you should be able to use JRuby's Java integration
 * to do whatever you would do in Java with the applet instance.
 *
 */
public class JRubyApplet extends Applet {
    private Ruby runtime;
    private boolean doubleBuffered = true;
    private Color backgroundColor = Color.WHITE;
    private IRubyObject rubyObject;
    private RubyProc startProc;
    private RubyProc stopProc;
    private RubyProc destroyProc;
    private RubyProc paintProc;
    private Graphics priorGraphics;
    private IRubyObject wrappedGraphics;
    private VolatileImage backBuffer;
    private Graphics backBufferGraphics;
    private Facade facade;

    private interface Facade {
        public InputStream getInputStream();
        public PrintStream getOutputStream();
        public PrintStream getErrorStream();
        public void attach(Ruby runtime, Applet applet);
    }

    private static RubyProc blockToProc(Ruby runtime, Block block) {
        if (block.isGiven()) {
            RubyProc proc = block.getProcObject();
            if (proc == null) {
                proc = RubyProc.newProc(runtime, block, block.type);
            }
            return proc;
        } else {
            return null;
        }
    }

    private boolean getBooleanParameter(String name, boolean default_value) {
        String value = getParameter(name);
        if ( value != null ) {
            return value.equals("true");
        } else {
            return default_value;
        }
    }

    private InputStream getCodeResourceAsStream(String name) {
        if (name == null) {
            return null;
        }
        try {
            final URL directURL = new URL(getCodeBase(), name);
            return directURL.openStream();
        } catch (IOException e) {
        }
        return JRubyApplet.class.getClassLoader().getResourceAsStream(name);
    }

    public static class RubyMethods {
        @JRubyMethod
        public static IRubyObject on_start(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            synchronized (applet) {
                applet.startProc = blockToProc(applet.runtime, block);
            }
            return recv;
        }

        @JRubyMethod
        public static IRubyObject on_stop(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            synchronized (applet) {
                applet.stopProc = blockToProc(applet.runtime, block);
            }
            return recv;
        }

        @JRubyMethod
        public static IRubyObject on_destroy(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            synchronized (applet) {
                applet.destroyProc = blockToProc(applet.runtime, block);
            }
            return recv;
        }

        @JRubyMethod
        public static IRubyObject on_paint(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            synchronized (applet) {
                applet.paintProc = blockToProc(applet.runtime, block);
                applet.repaint();
            }
            return recv;
        }
    }

    @Override
    public void init() {
        super.init();
        final JRubyApplet applet = this;

        if (getBooleanParameter("console", false)) {
            facade = new ConsoleFacade();
        } else {
            facade = new TrivialFacade();
        }

        synchronized (this) {
            if (runtime != null) {
                return;
            }

            final RubyInstanceConfig config = new RubyInstanceConfig() {{
                setInput(facade.getInputStream());
                setOutput(facade.getOutputStream());
                setError(facade.getErrorStream());
                setObjectSpaceEnabled(getBooleanParameter("objectspace", false));
            }};
            Ruby.setSecurityRestricted(true);
            runtime = Ruby.newInstance(config);
            rubyObject = JavaUtil.convertJavaToUsableRubyObject(runtime, this);
            rubyObject.dataWrapStruct(this);
            runtime.defineGlobalConstant("JRUBY_APPLET", rubyObject);
            rubyObject.getMetaClass().defineAnnotatedMethods(RubyMethods.class);
            facade.attach(runtime, this);
        }

        final String scriptName = getParameter("script");
        final InputStream scriptStream = getCodeResourceAsStream(scriptName);
        final String evalString = getParameter("eval");

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if (scriptStream != null) {
                        applet.runtime.runFromMain(scriptStream, scriptName);
                    }
                    if (evalString != null) {
                        applet.runtime.evalScriptlet(evalString);
                    }
                }
            });
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error running script", e.getCause());
        }
    }

    private void invokeCallback(final RubyProc proc, final IRubyObject[] args) {
        if (proc == null) {
            return;
        }
        final Ruby runtime = this.runtime;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    ThreadContext context = runtime.getCurrentContext();
                    proc.call(context, args, Block.NULL_BLOCK);
                }
            });
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Ruby callback failed", e.getCause());
        }
    }

    public synchronized void setBackgroundColor(Color color) {
        backgroundColor = color;
        repaint();
    }

    public synchronized Color getBackgroundColor() {
        return backgroundColor;
    }

    public synchronized boolean isDoubleBuffered() {
        return doubleBuffered;
    }

    public synchronized void setDoubleBuffered(boolean shouldBuffer) {
        doubleBuffered = shouldBuffer;
        repaint();
    }

    @Override
    public synchronized void start() {
        super.start();
        invokeCallback(startProc, new IRubyObject[] {});
    }

    @Override
    public synchronized void stop() {
        invokeCallback(stopProc, new IRubyObject[] {});
        super.stop();
    }

    @Override
    public synchronized void destroy() {
        try {
            invokeCallback(destroyProc, new IRubyObject[] {});
        } finally {
            final Ruby runtime = this.runtime;
            this.runtime = null;
            rubyObject = null;
            startProc = null;
            stopProc = null;
            destroyProc = null;
            paintProc = null;
            priorGraphics = null;
            wrappedGraphics = null;
            runtime.tearDown();
            super.destroy();
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public synchronized void paint(Graphics g) {
        if (doubleBuffered) {
            paintBuffered(g);
        } else {
            paintUnbuffered(g);
        }
    }

    private synchronized void paintBuffered(Graphics g) {
        do {
            GraphicsConfiguration config = getGraphicsConfiguration();
            int width = getWidth();
            int height = getHeight();
            if (backBuffer == null || width != backBuffer.getWidth() || height != backBuffer.getHeight() || backBuffer.validate(config) == VolatileImage.IMAGE_INCOMPATIBLE) {
                if (backBuffer != null) {
                    backBufferGraphics.dispose();
                    backBuffer.flush();
                }
                backBuffer = config.createCompatibleVolatileImage(width, height);
                backBufferGraphics = backBuffer.createGraphics();
            }
            backBufferGraphics.setClip(g.getClip());
            paintUnbuffered(backBufferGraphics);
        } while (backBuffer.contentsLost());
        g.drawImage(backBuffer, 0, 0, this);
    }

    private synchronized void paintUnbuffered(Graphics g) {
        if (backgroundColor != null) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (paintProc != null) {
            if (priorGraphics != g) {
                wrappedGraphics = JavaUtil.convertJavaToUsableRubyObject(runtime, g);
                priorGraphics = g;
            }
            ThreadContext context = runtime.getCurrentContext();
            paintProc.call(context, new IRubyObject[] {wrappedGraphics}, Block.NULL_BLOCK);
        }
        super.paint(g);
    }

    private static class TrivialFacade implements Facade {
        public TrivialFacade() {}
        public InputStream getInputStream() { return System.in; }
        public PrintStream getOutputStream() { return System.out; }
        public PrintStream getErrorStream() { return System.err; }
        public void attach(Ruby runtime, Applet applet) {}
    }

    private static class ConsoleFacade implements Facade {
        private JTextPane textPane;
        private JScrollPane scrollPane;
        private TextAreaReadline adaptor;
        private InputStream inputStream;
        private PrintStream outputStream;
        private PrintStream errorStream;
        
        public ConsoleFacade() {
            textPane = new JTextPane();
	    textPane.setMargin(new Insets(4, 4, 4, 4));
            textPane.setCaretColor(new Color(0xa4, 0x00, 0x00));
            textPane.setBackground(new Color(0xf2, 0xf2, 0xf2));
            textPane.setForeground(new Color(0xa4, 0x00, 0x00));

            Font font = findFont("Monospaced", Font.PLAIN, 14,
                                 new String[] {"Monaco", "Andale Mono"});

            textPane.setFont(font);

            scrollPane = new JScrollPane(textPane);
            scrollPane.setDoubleBuffered(true);
            adaptor = new TextAreaReadline(textPane, "  JRuby applet console  \n\n");
            inputStream = new PipedInputStream();
            outputStream = new PrintStream(adaptor);
            errorStream = new PrintStream(adaptor);
        }
        public InputStream getInputStream() { return inputStream; }
        public PrintStream getOutputStream() { return outputStream; }
        public PrintStream getErrorStream() { return errorStream; }
        public void attach(Ruby runtime, Applet applet) {
            adaptor.hookIntoRuntime(runtime);
            applet.add(scrollPane);
            applet.validate();
        }

        private Font findFont(String otherwise, int style, int size, String[] families) {
            String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Arrays.sort(fonts);
            for (int i = 0; i < families.length; i++) {
                if (Arrays.binarySearch(fonts, families[i]) >= 0) {
                    return new Font(families[i], style, size);
                }
            }
            return new Font(otherwise, style, size);
        }
    }
}
