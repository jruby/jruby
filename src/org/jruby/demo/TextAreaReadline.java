package org.jruby.demo;

import java.awt.Color;
import java.awt.Point;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ext.Readline;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.Join;

public class TextAreaReadline implements KeyListener {
    private static final String EMPTY_LINE = "";
    
    private JTextComponent area;
    private int startPos;
    private String currentLine;
    
    public volatile MutableAttributeSet promptStyle;
    public volatile MutableAttributeSet inputStyle;
    public volatile MutableAttributeSet outputStyle;
    public volatile MutableAttributeSet resultStyle;
    
    private JComboBox completeCombo;
    private BasicComboPopup completePopup;
    private int start;
    private int end;

    private final InputStream inputStream = new Input();
    private final OutputStream outputStream = new Output();

    private static class InputBuffer {
        public final byte[] bytes;
        public int offset = 0;
        public InputBuffer(byte[] bytes) {
            this.bytes = bytes;
        }
    }

    public enum Channel {
        AVAILABLE,
        READ,
        BUFFER,
        EMPTY,
        LINE,
        GET_LINE,
        SHUTDOWN,
        FINISHED
    }

    private static class ReadRequest {
        public final byte[] b;
        public final int off;
        public final int len;

        public ReadRequest(byte[] b, int off, int len) {
            this.b = b;
            this.off = off;
            this.len = len;
        }

        public int perform(Join join, InputBuffer buffer) {
            final int available = buffer.bytes.length - buffer.offset;
            int len = this.len;
            if ( len > available ) {
                len = available;
            }
            if ( len == available ) {
                join.send(Channel.EMPTY, null);
            } else {
                buffer.offset += len;
                join.send(Channel.BUFFER, buffer);
            }
            System.arraycopy(buffer.bytes, buffer.offset, this.b, this.off, len);
            return len;
        }
    }

    private static final Join.Spec INPUT_SPEC = new Join.Spec() {{
        addReaction(new Join.FastReaction(Channel.SHUTDOWN, Channel.BUFFER) {
            public void react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
            }
        });
        addReaction(new Join.FastReaction(Channel.SHUTDOWN, Channel.EMPTY) {
            public void react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
            }
        });
        addReaction(new Join.FastReaction(Channel.SHUTDOWN, Channel.FINISHED) {
            public void react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
            }
        });

        addReaction(new Join.FastReaction(Channel.FINISHED, Channel.LINE) {
            public void react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
            }
        });

        addReaction(new Join.SyncReaction(Channel.AVAILABLE, Channel.BUFFER) {
            public Object react(Join join, Object[] args) {
                InputBuffer buffer = (InputBuffer)args[1];
                join.send(Channel.BUFFER, buffer);
                return buffer.bytes.length - buffer.offset;
            }
        });
        addReaction(new Join.SyncReaction(Channel.AVAILABLE, Channel.EMPTY) {
            public Object react(Join join, Object[] args) {
                join.send(Channel.EMPTY, null);
                return 0;
            }
        });
        addReaction(new Join.SyncReaction(Channel.AVAILABLE, Channel.FINISHED) {
            public Object react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
                return 0;
            }
        });

        addReaction(new Join.SyncReaction(Channel.READ, Channel.BUFFER) {
            public Object react(Join join, Object[] args) {
                return ((ReadRequest)args[0]).perform(join, (InputBuffer)args[1]);
            }
        });
        addReaction(new Join.SyncReaction(Channel.READ, Channel.EMPTY, Channel.LINE) {
            public Object react(Join join, Object[] args) {
                final ReadRequest request = (ReadRequest)args[0];
                final String line = (String)args[2];
                if (line.length() != 0) {
                    byte[] bytes;
                    try {
                        bytes = line.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        bytes = line.getBytes();
                    }
                    return request.perform(join, new InputBuffer(bytes));
                } else {
                    return -1;
                }
            }
        });
        addReaction(new Join.SyncReaction(Channel.READ, Channel.FINISHED) {
            public Object react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
                return -1;
            }
        });

        addReaction(new Join.SyncReaction(Channel.GET_LINE, Channel.LINE) {
            public Object react(Join join, Object[] args) {
                return args[1];
            }
        });
        addReaction(new Join.SyncReaction(Channel.GET_LINE, Channel.FINISHED) {
            public Object react(Join join, Object[] args) {
                join.send(Channel.FINISHED, null);
                return EMPTY_LINE;
            }
        });
    }};
    private final Join inputJoin = INPUT_SPEC.createJoin();

    public TextAreaReadline(JTextComponent area) {
        this(area, null);
    }
    
    public TextAreaReadline(JTextComponent area, final String message) {
        this.area = area;

        inputJoin.send(Channel.EMPTY, null);
        
        area.addKeyListener(this);
        
        // No editing before startPos
        if (area.getDocument() instanceof AbstractDocument)
            ((AbstractDocument) area.getDocument()).setDocumentFilter(
                new DocumentFilter() {
                    public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                        if (offset >= startPos) super.insertString(fb, offset, string, attr);
                    }
                    
                    public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
                        if (offset >= startPos) super.remove(fb, offset, length);
                    }
                    
                    public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                        if (offset >= startPos) super.replace(fb, offset, length, text, attrs);
                    }
                }
            );
        
        promptStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(promptStyle, new Color(0xa4, 0x00, 0x00));
        
        inputStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(inputStyle, new Color(0x20, 0x4a, 0x87));
        
        outputStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(outputStyle, Color.darkGray);
        
        resultStyle = new SimpleAttributeSet();
        StyleConstants.setItalic(resultStyle, true);
        StyleConstants.setForeground(resultStyle, new Color(0x20, 0x4a, 0x87));
        
        completeCombo = new JComboBox();
        completeCombo.setRenderer(new DefaultListCellRenderer()); // no silly ticks!
        completePopup = new BasicComboPopup(completeCombo);
        
        if (message != null) {
            final MutableAttributeSet messageStyle = new SimpleAttributeSet();
            StyleConstants.setBackground(messageStyle, area.getForeground());
            StyleConstants.setForeground(messageStyle, area.getBackground());
            append(message, messageStyle);
        }

        startPos = area.getDocument().getLength();
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
    
    private Ruby runtime;

    /**
     * Hooks this <code>TextAreaReadline</code> instance into the
     * runtime, redefining the <code>Readline</code> module so that
     * it uses this object. This method does not redefine the standard
     * input-output streams. If you need that, use
     * {@link #hookIntoRuntimeWithStreams(Ruby)}.
     *
     * @param runtime
     *                The runtime.
     * @see #hookIntoRuntimeWithStreams(Ruby)
     */
    public void hookIntoRuntime(final Ruby runtime) {
        this.runtime = runtime;
        /* Hack in to replace usual readline with this */
        runtime.getLoadService().require("readline");
        RubyModule readlineM = runtime.fastGetModule("Readline");

        readlineM.defineModuleFunction("readline", new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                String line = readLine(args[0].toString());
                if (line != null) {
                    return RubyString.newUnicodeString(runtime, line);
                } else {
                    return runtime.getNil();
                }
            }
            public Arity getArity() { return Arity.twoArguments(); }
        });
    }

    /**
     * Hooks this <code>TextAreaReadline</code> instance into the
     * runtime, redefining the <code>Readline</code> module so that
     * it uses this object. This method also redefines the standard
     * input-output streams accordingly.
     *
     * @param runtime
     *                The runtime.
     * @see #hookIntoRuntime(Ruby)
     */
    public void hookIntoRuntimeWithStreams(final Ruby runtime) {
        hookIntoRuntime(runtime);

        RubyIO in = new RubyIO(runtime, getInputStream());
        runtime.getGlobalVariables().set("$stdin", in);

        RubyIO out = new RubyIO(runtime, getOutputStream());
        runtime.getGlobalVariables().set("$stdout", out);
        runtime.getGlobalVariables().set("$stderr", out);
    }
    
    protected void completeAction(KeyEvent event) {
        if (Readline.getCompletor(Readline.getHolder(runtime)) == null) return;
        
        event.consume();
        
        if (completePopup.isVisible()) return;
        
        List candidates = new LinkedList();
        String bufstr = null;
        try {
            bufstr = area.getText(startPos, area.getCaretPosition() - startPos);
        } catch (BadLocationException e) {
            return;
        }
        
        int cursor = area.getCaretPosition() - startPos;
        
        int position = Readline.getCompletor(Readline.getHolder(runtime)).complete(bufstr, cursor, candidates);
        
        // no candidates? Fail.
        if (candidates.isEmpty())
            return;
        
        if (candidates.size() == 1) {
            replaceText(startPos + position, area.getCaretPosition(), (String) candidates.get(0));
            return;
        }
        
        start = startPos + position;
        end = area.getCaretPosition();
        
        Point pos = area.getCaret().getMagicCaretPosition();

        // bit risky if someone changes completor, but useful for method calls
        int cutoff = bufstr.substring(position).lastIndexOf('.') + 1;
        start += cutoff;

        if (candidates.size() < 10)
            completePopup.getList().setVisibleRowCount(candidates.size());
        else
            completePopup.getList().setVisibleRowCount(10);

        completeCombo.removeAllItems();
        for (Iterator i = candidates.iterator(); i.hasNext();) {
            String item = (String) i.next();
            if (cutoff != 0) item = item.substring(cutoff);
            completeCombo.addItem(item);
        }

        completePopup.show(area, pos.x, pos.y + area.getFontMetrics(area.getFont()).getHeight());
    }

    protected void backAction(KeyEvent event) {
        if (area.getCaretPosition() <= startPos)
            event.consume();
    }
    
    protected void upAction(KeyEvent event) {
        event.consume();
        
        if (completePopup.isVisible()) {
            int selected = completeCombo.getSelectedIndex() - 1;
            if (selected < 0) return;
            completeCombo.setSelectedIndex(selected);
            return;
        }
        
        if (!Readline.getHistory(Readline.getHolder(runtime)).next()) // at end
            currentLine = getLine();
        else
            Readline.getHistory(Readline.getHolder(runtime)).previous(); // undo check
        
        if (!Readline.getHistory(Readline.getHolder(runtime)).previous()) return;
        
        String oldLine = Readline.getHistory(Readline.getHolder(runtime)).current().trim();
        replaceText(startPos, area.getDocument().getLength(), oldLine);
    }
    
    protected void downAction(KeyEvent event) {
        event.consume();
        
        if (completePopup.isVisible()) {
            int selected = completeCombo.getSelectedIndex() + 1;
            if (selected == completeCombo.getItemCount()) return;
            completeCombo.setSelectedIndex(selected);
            return;
        }
        
        if (!Readline.getHistory(Readline.getHolder(runtime)).next()) return;
        
        String oldLine;
        if (!Readline.getHistory(Readline.getHolder(runtime)).next()) // at end
            oldLine = currentLine;
        else {
            Readline.getHistory(Readline.getHolder(runtime)).previous(); // undo check
            oldLine = Readline.getHistory(Readline.getHolder(runtime)).current().trim();
        }
        
        replaceText(startPos, area.getDocument().getLength(), oldLine);
    }
    
    protected void replaceText(int start, int end, String replacement) {
        try {
            area.getDocument().remove(start, end - start);
            area.getDocument().insertString(start, replacement, inputStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    protected String getLine() {
        try {
            return area.getText(startPos, area.getDocument().getLength() - startPos);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    protected void enterAction(KeyEvent event) {
        event.consume();
        
        if (completePopup.isVisible()) {
            if (completeCombo.getSelectedItem() != null)
                replaceText(start, end, (String) completeCombo.getSelectedItem());
            completePopup.setVisible(false);
            return;
        }
        
        append("\n", null);
        
        String line = getLine();
        startPos = area.getDocument().getLength();
        inputJoin.send(Channel.LINE, line);
    }
    
    public String readLine(final String prompt) {
        if (EventQueue.isDispatchThread()) {
            throw runtime.newThreadError("Cannot call readline from event dispatch thread");
        }

        EventQueue.invokeLater(new Runnable() {
           public void run() {
               append(prompt.trim(), promptStyle);
               append(" ", inputStyle); // hack to get right style for input
               area.setCaretPosition(area.getDocument().getLength());
               startPos = area.getDocument().getLength();
               Readline.getHistory(Readline.getHolder(runtime)).moveToEnd();
            }
        });
        
        final String line = (String)inputJoin.call(Channel.GET_LINE, null);
        if (line.length() > 0) {
            return line.trim();
        } else {
            return null;
        }
    }
    
    public void keyPressed(KeyEvent event) {
        int code = event.getKeyCode();
        switch (code) {
        case KeyEvent.VK_TAB: completeAction(event); break;
        case KeyEvent.VK_LEFT: 
        case KeyEvent.VK_BACK_SPACE:
            backAction(event); break;
        case KeyEvent.VK_UP: upAction(event); break;
        case KeyEvent.VK_DOWN: downAction(event); break;
        case KeyEvent.VK_ENTER: enterAction(event); break;
        case KeyEvent.VK_HOME: event.consume(); area.setCaretPosition(startPos); break;
        case KeyEvent.VK_D:
            if ( ( event.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK ) != 0 ) {
                event.consume();
                inputJoin.send(Channel.LINE, EMPTY_LINE);
            }
            break;
        }
        
        if (completePopup.isVisible() &&
                code !=  KeyEvent.VK_TAB &&
                code != KeyEvent.VK_UP &&
                code != KeyEvent.VK_DOWN )
            completePopup.setVisible(false);
    }

    public void keyReleased(KeyEvent arg0) { }

    public void keyTyped(KeyEvent arg0) { }

    public void shutdown() {
        inputJoin.send(Channel.SHUTDOWN, null);
    }
    
    /** Output methods **/
    
    protected void append(String toAppend, AttributeSet style) {
       try {
           area.getDocument().insertString(area.getDocument().getLength(), toAppend, style);
       } catch (BadLocationException e) { }
    }

    private void writeLineUnsafe(final String line) {
        if (line.startsWith("=>")) append(line, resultStyle);
        else append(line, outputStyle);
        startPos = area.getDocument().getLength();
    }
    
    private void writeLine(final String line) {
        if (EventQueue.isDispatchThread()) {
            writeLineUnsafe(line);
        } else {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    writeLineUnsafe(line);
                }
            });
        }
    }

    private class Input extends InputStream {
        private volatile boolean closed = false;

        @Override
        public int available() throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }

            return (Integer)inputJoin.call(Channel.AVAILABLE, null);
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            if ( read(b, 0, 1) == 1 ) {
                return b[0];
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }

            if (EventQueue.isDispatchThread()) {
                throw new IOException("Cannot call read from event dispatch thread");
            }

            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off+len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }

            final ReadRequest request = new ReadRequest(b, off, len);
            return (Integer)inputJoin.call(Channel.READ, request);
        }

        @Override
        public void close() {
            closed = true;
            inputJoin.send(Channel.SHUTDOWN, null);
        }
    }

    private class Output extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            writeLine("" + b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            try {
                writeLine(new String(b, off, len, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                writeLine(new String(b, off, len));
            }
        }

        @Override
        public void write(byte[] b) {
            try {
                writeLine(new String(b, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                writeLine(new String(b));
            }
        }
    }
}
