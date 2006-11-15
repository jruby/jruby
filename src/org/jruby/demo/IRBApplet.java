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

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBApplet extends Applet {

    public void start() {
        super.start();

        try {
            final PipedInputStream pipeIn = new PipedInputStream();
            final PipedOutputStream out = new PipedOutputStream(pipeIn);
            final PipedInputStream in = new PipedInputStream();
            final PipedOutputStream pipeOut = new PipedOutputStream(in);
            final IRuby runtime = Ruby.newInstance(pipeIn, new PrintStream(pipeOut),
                    new PrintStream(pipeOut), false);

            runtime.defineGlobalConstant("ARGV", runtime.newArray(new IRubyObject[] {
                    runtime.newString("-f"), runtime.newString("--noreadline"),
                    runtime.newString("--prompt-mode"), runtime.newString("simple") }));
            runtime.getLoadService().init(new ArrayList(0));

            final JTextArea text = new JTextArea();
            text.setLineWrap(true);
            text.setWrapStyleWord(false);
            text.setEditable(false);
            text.getCaret().setBlinkRate(500);
            text.setCaretColor(Color.GRAY);
            text.setBackground(Color.WHITE);
            text.setForeground(Color.GRAY);
            final JScrollPane pane = new JScrollPane();
            pane.setViewportView(text);

            add(pane);
            pane.setPreferredSize(getSize());
            this.validate();

            Thread t = new Thread() {
                public void run() {
                    byte[] buffer = new byte[256];
                    try {
                        while (true) {
                            int len = in.read(buffer);

                            text.append(new String(buffer, 0, len));
                            text.setCaretPosition(text.getText().length());
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            };
            t.start();

            text.addKeyListener(new KeyListener() {
                StringBuffer line = new StringBuffer();

                public void keyPressed(KeyEvent e) {
                    text.getCaret().setVisible(true);
                }

                public void keyReleased(KeyEvent e) {
                    text.getCaret().setVisible(true);
                }

                public void keyTyped(KeyEvent e) {
                    switch (e.getKeyChar()) {
                    case 8:
                        if (line.length() > 0) {
                            text.setText(text.getText().substring(0, text.getText().length() - 1));
                            line.setLength(line.length() - 1);
                        }
                        break;
                    case 10:
                        try {
                            text.append("\n");
                            out.write(line.append("\n").toString().getBytes());
                            Thread.yield();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        line.setLength(0);
                        break;
                    default:
                        text.append(String.valueOf(e.getKeyChar()));
                        line.append(String.valueOf(e.getKeyChar()));
                    }
                    text.setCaretPosition(text.getText().length());
                    text.getCaret().setVisible(true);
                }
            });

            text.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    text.setCaretPosition(text.getText().length());
                    text.getCaret().setVisible(true);
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                    text.getCaret().setVisible(true);
                }
            });

            text.append("*** Welcome to Ruby for the Web! ***\n");

            text.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    text.setCaretPosition(text.getText().length());
                    text.getCaret().setVisible(true);
                }

                public void insertUpdate(DocumentEvent e) {
                    text.setCaretPosition(text.getText().length());
                    text.getCaret().setVisible(true);
                }

                public void removeUpdate(DocumentEvent e) {
                    text.setCaretPosition(text.getText().length());
                    text.getCaret().setVisible(true);
                }
            });

            Thread t2 = new Thread() {
                public void run() {
                    runtime.evalScript("require 'irb'; IRB.start");
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
