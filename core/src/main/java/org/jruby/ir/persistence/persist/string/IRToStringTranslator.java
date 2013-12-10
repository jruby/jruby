package org.jruby.ir.persistence.persist.string;

import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;


public class IRToStringTranslator {
    
    private IRToStringTranslator(){}
    
    public static String translate(IRScope irScope) {
        IRScopeStringExtractor stringExtractor = IRScopeStringExtractor.createToplevelInstance();
        return stringExtractor.extract(irScope);
    }
    
    public static String translate(Instr instr) {
        IRInstrStringExtractor stringExtractor = IRInstrStringExtractor.createToplevelInstance();
        return stringExtractor.extract(instr);
    }
    
    public static String translate(Operand operand) {
        IROperandStringExtractor stringExtractor = IROperandStringExtractor.createToplevelInstance();
        return stringExtractor.extract(operand);
    }
    
    // Following methods are used from inside StringBuilder's
    // They were added to preserve single instance of StringBuilder
    public static void continueTranslation(StringBuilder builder, Operand operand) {
        IROperandStringExtractor stringExtractor = IROperandStringExtractor.createInstance(builder);
        stringExtractor.produceString(operand);
    }
    
    public static void continueTranslation(StringBuilder builder, Instr instr) {
        IRInstrStringExtractor stringExtractor = IRInstrStringExtractor.createInstance(builder);
        stringExtractor.produceString(instr);
    }
    
}