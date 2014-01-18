package org.jruby.ir.persistence.persist.string;

import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;


public class IRToStringTranslator {
    public static String translate(IRScope irScope) {
        return IRScopeStringExtractor.createToplevelInstance().extract(irScope);
    }

    public static String translate(Instr instr) {
        return IRInstrStringExtractor.createToplevelInstance().extract(instr);
    }

    public static String translate(Operand operand) {
        return IROperandStringExtractor.createToplevelInstance().extract(operand);
    }

    // Following methods are used from inside StringBuilder's
    // They were added to preserve single instance of StringBuilder
    public static void continueTranslation(StringBuilder builder, Operand operand) {
        IROperandStringExtractor.createInstance(builder).produceString(operand);
    }

    public static void continueTranslation(StringBuilder builder, Instr instr) {
        IRInstrStringExtractor.createInstance(builder).produceString(instr);
    }
}
