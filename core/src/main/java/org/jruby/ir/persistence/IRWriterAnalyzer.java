/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.parser.StaticScope;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.Signature;
import org.jruby.util.ByteList;

/**
 *
 * @author enebo
 */
public class IRWriterAnalyzer implements IRWriterEncoder {
    private int currentOffsetId = 0;
    private final Map<IRScope, Integer> offsetIds = new HashMap();

    // Figure out most commonly used operands for eventual creation of an operand pool
    private final Map<Operand, Integer> operandCounts = new HashMap<Operand, Integer>();

    @Override
    public void encode(Instr instr) {
        for (Operand operand: instr.getOperands()) {
            increment(operand);
        }
    }

    @Override
    public boolean isAnalyzer() {
        return true;
    }

    public IRScope getCurrentScope() {
        return null;
    }

    @Override
    public void encode(ByteList value) {
    }

    @Override
    public void encode(byte[] value) {
    }

    @Override
    public void encode(Encoding encoding) {
    }

    @Override
    public void encode(RubySymbol value) {
    }

    @Override
    public void encodeRaw(RubySymbol value) {

    }

    @Override
    public void encode(String value) {
    }

    @Override
    public void encode(String[] values) {
    }

    @Override
    public void encode(IRScope value) {
    }

    @Override
    public void encode(IRScopeType value) {
    }

    @Override
    public void encode(StaticScope.Type value) {
    }

    @Override
    public void encode(Operation value) {
    }

    @Override
    public void encode(OperandType value) {
    }

    @Override
    public void encode(Operand operand) {
    }

    @Override
    public void encode(Operand[] operand) {
    }

    @Override
    public void encode(boolean value) {
    }

    @Override
    public void encode(byte value) {
    }

    @Override
    public void encode(char value) {
    }

    @Override
    public void encode(int value) {
    }

    @Override
    public void encode(int[] value) {

    }

    @Override
    public void encode(long value) {
    }

    @Override
    public void encode(float value) {
    }

    @Override
    public void encode(EnumSet<IRFlags> value) {
    }

    @Override
    public void encode(double value) {
    }

    @Override
    public void encode(Signature value) {
    }

    @Override
    public void encode(RubyEvent event) {
    }

    @Override
    public void startEncodingScopeHeader(IRScope scope) {
    }

    @Override
    public void endEncodingScopeHeader(IRScope scope) {
    }

    @Override
    public void startEncodingScopeInstrs(IRScope scope) {
        offsetIds.put(scope, currentOffsetId++);
    }

    @Override
    public void endEncodingScopeInstrs(IRScope scope) {
    }

    @Override
    public void startEncodingScopeHeaders(IRScope script) {
    }

    @Override
    public void endEncodingScopeHeaders(IRScope script) {
    }

    @Override
    public void startEncoding(IRScope script) {
    }

    @Override
    public void endEncoding(IRScope script) {
    }

    private void increment(Operand operand) {
        Integer count = operandCounts.get(operand);
        if (count == null) count = Integer.valueOf(0);

        operandCounts.put(operand, count + 1);
    }

    public int getScopeID(IRScope value) {
        return offsetIds.get(value);
    }

    public int getScopeCount() {
        return offsetIds.size();
    }
}
