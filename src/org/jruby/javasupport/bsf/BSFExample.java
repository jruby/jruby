/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.javasupport.bsf;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** An example demonstrating the use of JRuby and BSF.
 * 
 */
public class BSFExample {
    private BSFManager manager;

    public static void main(String[] args) {
        /*
         * First we need to register the JRuby engine.
         */
        BSFManager.registerScriptingEngine("ruby", "org.jruby.javasupport.bsf.JRubyEngine", new String[] { "rb" });

        /*
         * Now we create a new BSFManager.
         */
        new BSFExample(new BSFManager());
    }

    public BSFExample(BSFManager manager) {
        this.manager = manager;
        
        /*
         * Initialize a simple Frame. 
         */
        initUI();
    }

    private void initUI() {
        /*
         * Initialize some components.
         */
        final JFrame frame = new JFrame("A sample BSF application");
        final JMenuBar menubar = new JMenuBar();
        final JTextArea input = new JTextArea("$frame.setTitle(\"A new title\")");
        final JButton execute = new JButton("Execute");
        final JButton eval = new JButton("Eval");

        try {
            /*
             * Declare those components as beans in BSF. Then it will be
             * possible to access those components in Ruby as global
             * variables ($frame, $menubar, ...)
             */
            manager.declareBean("frame", frame, JFrame.class);
            manager.declareBean("menubar", menubar, JMenuBar.class);
            manager.declareBean("input", input, JTextArea.class);
            manager.declareBean("execute", execute, JButton.class);
            manager.declareBean("eval", eval, JButton.class);
        } catch (BSFException excptn) {
            excptn.printStackTrace();
            JOptionPane.showMessageDialog(null, excptn.getMessage());
        }

        frame.getContentPane().setLayout(new BorderLayout(12, 12));
        frame.getContentPane().add(input, BorderLayout.CENTER);
        
        JPanel buttonPane = new JPanel(new FlowLayout(12));
        frame.getContentPane().add(buttonPane, BorderLayout.SOUTH);
        buttonPane.add(execute, BorderLayout.EAST);
        buttonPane.add(eval, BorderLayout.EAST);

        try {
            /* 
             * Execute a Ruby script (add the menubar to the frame).
             */
            manager.exec("ruby", "initUI", 1, 1, "$frame.setJMenuBar($menubar)");
        } catch (BSFException excptn) {
            excptn.printStackTrace();
            JOptionPane.showMessageDialog(null, excptn.getMessage());
        }

        execute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    /*
                     * Execute Ruby statements.
                     */
                    manager.exec("ruby", "initUI", 1, 1, input.getText());
                } catch (BSFException excptn) {
                    excptn.printStackTrace();
                    JOptionPane.showMessageDialog(frame, excptn.getMessage());
                }
            }
        });
        
        eval.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    /*
                     * Evaluates a Ruby expression and display the result.
                     */
                    String expression = JOptionPane.showInputDialog(frame, "Please enter a Ruby expression:");
                    input.setText(String.valueOf(manager.eval("ruby", "initUI", 1, 1, expression)));
                } catch (BSFException excptn) {
                    excptn.printStackTrace();
                    JOptionPane.showMessageDialog(frame, excptn.getMessage());
                }
            }
        });

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
