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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
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
import org.jruby.demo.TextAreaReadline;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import javax.swing.JApplet;
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
 * JRubyApplet.applet; the script can also define callbacks for applet
 * start, stop, and destroy by passing blocks to JRubyApplet.on_start,
 * JRubyApplet.on_stop, and JRubyApplet.on_destroy, respectively.
 *
 */
public class JRubyApplet extends JApplet {
    private Ruby runtime;
    private IRubyObject rubyObject;
    private RubyProc startProc;
    private RubyProc stopProc;
    private RubyProc destroyProc;
    private Graphics priorGraphics;
    private IRubyObject wrappedGraphics;
    private Facade console;

    public interface PaintCallback {
        public void paint(Graphics g);
    }

    public interface Facade {
        public InputStream getInputStream();
        public PrintStream getOutputStream();
        public PrintStream getErrorStream();
        public void attachRuntime(Ruby runtime);
        public JComponent getComponent();
        public void setPaintCallback(PaintCallback callback);
    }

    public static class AppletModule {
        public static void setup(Ruby runtime, JRubyApplet applet) {
            RubyModule module = runtime.defineModule("JRubyApplet");
            module.dataWrapStruct(applet);
            CallbackFactory cb = runtime.callbackFactory(AppletModule.class);
            module.getMetaClass().defineMethod("applet", cb.getSingletonMethod("applet"));
            module.getMetaClass().defineMethod("on_start", cb.getSingletonMethod("on_start"));
            module.getMetaClass().defineMethod("on_stop", cb.getSingletonMethod("on_stop"));
            module.getMetaClass().defineMethod("on_destroy", cb.getSingletonMethod("on_destroy"));
            module.getMetaClass().defineMethod("on_paint", cb.getSingletonMethod("on_paint"));
            runtime.evalScriptlet("def JRubyApplet.content_pane ; applet.get_content_pane ; end");
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

        public static IRubyObject applet(IRubyObject recv, Block block) {
            return ((JRubyApplet)recv.dataGetStruct()).getRubyObject();
        }

        public static IRubyObject on_start(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            applet.setStartProc(blockToProc(recv.getRuntime(), block));
            return recv;
        }

        public static IRubyObject on_stop(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            applet.setStopProc(blockToProc(recv.getRuntime(), block));
            return recv;
        }

        public static IRubyObject on_destroy(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            applet.setDestroyProc(blockToProc(recv.getRuntime(), block));
            return recv;
        }

        public static IRubyObject on_paint(IRubyObject recv, Block block) {
            JRubyApplet applet = (JRubyApplet)recv.dataGetStruct();
            applet.setPaintProc(blockToProc(recv.getRuntime(), block));
            return recv;
        }
    }

    private synchronized IRubyObject getRubyObject() {
        return rubyObject;
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

    public void init() {
        super.init();
        final JRubyApplet applet = this;

        if (getBooleanParameter("console", false)) {
            console = new ConsoleFacade();
        } else {
            console = new TrivialFacade();
        }

        synchronized (this) {
            if (runtime != null) {
                return;
            }

            final RubyInstanceConfig config = new RubyInstanceConfig() {{
                setInput(console.getInputStream());
                setOutput(console.getOutputStream());
                setError(console.getErrorStream());
                setObjectSpaceEnabled(getBooleanParameter("objectspace", false));
            }};
            Ruby.setSecurityRestricted(true);
            runtime = Ruby.newInstance(config);
            rubyObject = JavaUtil.convertJavaToUsableRubyObject(runtime, this);
            AppletModule.setup(runtime, this);
            console.attachRuntime(runtime);
        }

        final String scriptName = getParameter("script");
        final InputStream scriptStream = getCodeResourceAsStream(scriptName);
        final String evalString = getParameter("eval");

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    applet.setContentPane(applet.console.getComponent());
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

    private synchronized void setStartProc(RubyProc proc) {
        startProc = proc;
    }
    public synchronized void start() {
        super.start();
        invokeCallback(startProc, new IRubyObject[] {});
    }

    private synchronized void setStopProc(RubyProc proc) {
        stopProc = proc;
    }
    public synchronized void stop() {
        invokeCallback(stopProc, new IRubyObject[] {});
        super.stop();
    }

    private synchronized void setDestroyProc(RubyProc proc) {
        destroyProc = proc;
    }
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
            priorGraphics = null;
            wrappedGraphics = null;
            runtime.tearDown();
            super.destroy();
        }
    }

    private synchronized void setPaintProc(final RubyProc proc) {
        if (proc != null) {
            final JRubyApplet applet = this;
            console.setPaintCallback(new PaintCallback() {
                public void paint(Graphics g) {
                    if (applet.priorGraphics != g) {
                        applet.wrappedGraphics = JavaUtil.convertJavaToUsableRubyObject(applet.runtime, g);
                        applet.priorGraphics = g;
                    }
                    ThreadContext context = applet.runtime.getCurrentContext();
                    proc.call(context, new IRubyObject[] {wrappedGraphics}, Block.NULL_BLOCK);
                }
            });
        } else {
            console.setPaintCallback(null);
        }
    }

    public static class TrivialFacade implements Facade {
        private PainterPanel panel;

        public static class PainterPanel extends JPanel {
            private PaintCallback paintCallback;

            public synchronized void setPaintCallback(PaintCallback callback) {
                paintCallback = callback;
                repaint(getVisibleRect());
            }

            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                synchronized (this) {
                    if (paintCallback != null) {
                        paintCallback.paint(g);
                    }
                }
            }
        }

        public TrivialFacade() {
            panel = new PainterPanel();
        }
        public InputStream getInputStream() { return System.in; }
        public PrintStream getOutputStream() { return System.out; }
        public PrintStream getErrorStream() { return System.err; }
        public void attachRuntime(Ruby runtime) {}
        public JComponent getComponent() { return panel; }
        public void setPaintCallback(PaintCallback callback) {
            panel.setPaintCallback(callback);
        }
    }

    public static class ConsoleFacade implements Facade {
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
            adaptor = new TextAreaReadline(textPane, "  JRuby applet console  \n\n");
            inputStream = new PipedInputStream();
            outputStream = new PrintStream(adaptor);
            errorStream = new PrintStream(adaptor);
        }
        public JComponent getComponent() { return scrollPane; }
        public InputStream getInputStream() { return inputStream; }
        public PrintStream getOutputStream() { return outputStream; }
        public PrintStream getErrorStream() { return errorStream; }
        public void attachRuntime(Ruby runtime) {
            adaptor.hookIntoRuntime(runtime);
        }
        public void setPaintCallback(PaintCallback callback) {
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
