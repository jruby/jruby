/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.ir.operands.Operand;

/**
 *
 * @author enebo
 */
class OperandEncoderMap {

    static void encode(IRWriterEncoder encoder, Operand operand) {
        encoder.encode(operand.getOperandType());
    }
    
}
