/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.interpreter;

import org.jruby.compiler.ir.operands.Label;
import org.jruby.exceptions.JumpException;

/**
 *
 * @author enebo
 */
public class Jump extends JumpException {
    public final Label target;

    public Jump(Label target) {
        this.target = target;
    }

    public Label getTarget() {
        return target;
    }
}
