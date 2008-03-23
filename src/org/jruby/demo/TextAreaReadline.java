package org.jruby.demo;

import java.awt.Color;
import java.awt.Point;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

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

public class TextAreaReadline extends OutputStream implements KeyListener {
    
    JTextComponent area;
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

    private LinkedBlockingQueue<String> pendingLines = new LinkedBlockingQueue<String>();
    
    public TextAreaReadline(JTextComponent area) {
        this(area, null);
    }
    
    public TextAreaReadline(JTextComponent area, final String message) {
        this.area = area;
        
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
    }
    
    private Ruby runtime;

    public void hookIntoRuntime(final Ruby runtime) {
        this.runtime = runtime;
        /* Hack in to replace usual readline with this */
        runtime.getLoadService().require("readline");
        RubyModule readlineM = runtime.fastGetModule("Readline");
        
        RubyIO out = new RubyIO(runtime, this);
        runtime.getGlobalVariables().set("$stdout", out);
        runtime.getGlobalVariables().set("$stderr", out);

        readlineM.defineModuleFunction("readline", new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                return RubyString.newUnicodeString(runtime, readLine(args[0].toString()));
            }
            public Arity getArity() { return Arity.twoArguments(); }
        });
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
        pendingLines.offer(getLine());
    }
    
    public String readLine(final String prompt) {
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               append(prompt.trim(), promptStyle);
               append(" ", inputStyle); // hack to get right style for input
               area.setCaretPosition(area.getDocument().getLength());
               startPos = area.getDocument().getLength();
               Readline.getHistory(Readline.getHolder(runtime)).moveToEnd();
            }
        });
        
        try {
            return pendingLines.take().trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
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
        }
        
        if (completePopup.isVisible() &&
                code !=  KeyEvent.VK_TAB &&
                code != KeyEvent.VK_UP &&
                code != KeyEvent.VK_DOWN )
            completePopup.setVisible(false);
    }

    public void keyReleased(KeyEvent arg0) { }

    public void keyTyped(KeyEvent arg0) { }
    
    /** Output methods **/
    
    protected void append(String toAppend, AttributeSet style) {
       try {
           area.getDocument().insertString(area.getDocument().getLength(), toAppend, style);
       } catch (BadLocationException e) { }
    }

    private void writeLineUnsafe(final String line) {
        if (line.startsWith("=>")) append(line, resultStyle);
        else append(line, outputStyle);
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
    
    public void write(int b) throws IOException {
        writeLine("" + b);
    }
    
    public void write(byte[] b, int off, int len) {
        try {
            writeLine(new String(b, off, len, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            writeLine(new String(b, off, len));
        }
    }
    
    public void write(byte[] b) {
        try {
            writeLine(new String(b, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            writeLine(new String(b));
        }
    }
}
