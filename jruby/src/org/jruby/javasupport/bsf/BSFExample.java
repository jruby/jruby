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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2003-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.javasupport.bsf;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

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
