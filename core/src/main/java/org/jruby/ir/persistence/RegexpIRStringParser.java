package org.jruby.ir.persistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;

public class RegexpIRStringParser {

    private static final String EMPTY_STRING = "";
    private static final String INSTR_WITH_DEAD_RESULT_MARKER = "[DEAD-RESULT]";
    private static final String DEAD_INSTR_MARKER = "[DEAD]";
    private static final String INSTR_WITH_OPERANDS_REGEXP = "(\\w+)" +// instruction name (group1)
            "\\(" + "(" + // parentheses and group delimiter (group2)
            "([^,]+,\\s+)" + // operands (group3)
            "*([^,]+)" + // last operand (group4)
            ")" + "\\)"; // end of (group2)
    private static final Pattern instrWithOperands = Pattern.compile(INSTR_WITH_OPERANDS_REGEXP);

    private static final String WHITESPACES = "\\s+";
    private static final int MAX_COUNT_OF_INSTR_PARTS = 4;

    private static final int INSTR_WITH_ASSIGNMENT_AND_OPERANDS_POSSITION = 3;
    private static final int INSTR_WITH_OPERANDS_POSSITION = 1;

    private static final String ASSIGNMENT = "=";
    private static final int ASSIGN_POSSITION = 2;
    private static final int MIN_COUNT_OF_PARTS_WHEN_ASSIGN = 4;
    private static final int LVALUE_POSSITION = 1;
    private static final int RVALUE_POSSITION = 3;

    private static final String LABEL_DETERMINANT = ":";
    private static final int LABEL_POSSITION = 2;

    private IRInstructionBuilder irInstructionBuilder;
    
    private class InstrSemiFinished {
        private String instrName ;
        private String paramsString;
        private boolean constructed; // do not change, must be false by default
        
        private InstrSemiFinished(String possibleInstrWithOperands) {
            Matcher instrWithOperandsMatcher = instrWithOperands.matcher(possibleInstrWithOperands);
            if (instrWithOperandsMatcher.matches()) {
                constructed = true;
                instrName = instrWithOperandsMatcher.group(1);
                paramsString = instrWithOperandsMatcher.group(2);
            }
        }
    }

    public RegexpIRStringParser(IRManager manager) {
        irInstructionBuilder = new IRInstructionBuilder(manager);
    }

    public void parse(String instrString, IRScope scope) throws IRPersistenceException {
        instrString = preprocessing(instrString);
        String[] instrParts = instrString.split(WHITESPACES, MAX_COUNT_OF_INSTR_PARTS);

        if (isIntructionWithAssignment(instrParts)) {
            buildInstrWithAssignment(instrParts, scope);
            return;
        }

        String possibleInstrWithOperands = instrParts[INSTR_WITH_OPERANDS_POSSITION];
        InstrSemiFinished semiFinishedInstr = new InstrSemiFinished(possibleInstrWithOperands);
        if (semiFinishedInstr.constructed) {
            InstrInfo instrInfo = new InstrInfo(semiFinishedInstr.instrName, semiFinishedInstr.paramsString);
            irInstructionBuilder.buildInstrWithoutAssignment(instrInfo, scope);
            return;
        }

        if (instrParts[1].contains(LABEL_DETERMINANT)) {
            irInstructionBuilder.buildLabel(instrParts[LABEL_POSSITION], scope);
            return;
        }

        // What is that
        throw new IRPersistenceException(instrString + " unknown");
    }

    private String preprocessing(String instrString) {
        instrString = instrString.replace(DEAD_INSTR_MARKER, EMPTY_STRING);
        instrString = instrString.replace(INSTR_WITH_DEAD_RESULT_MARKER, EMPTY_STRING);
        return instrString;
    }

    private void buildInstrWithAssignment(String[] instrParts, IRScope scope) {
        String lvalue = instrParts[LVALUE_POSSITION];
        String possibleInstrWithOperands = instrParts[INSTR_WITH_ASSIGNMENT_AND_OPERANDS_POSSITION];
        InstrSemiFinished semiFinishedInstr = new InstrSemiFinished(possibleInstrWithOperands);
        if (semiFinishedInstr.constructed) {
            InstrInfo instrInfo = new InstrInfo(lvalue, semiFinishedInstr.instrName, semiFinishedInstr.paramsString);
            irInstructionBuilder.buildInstrWithAssignment(instrInfo, scope);
        } else {
            int rvaluePossition = RVALUE_POSSITION;
            String rvalue = instrParts[rvaluePossition];
            irInstructionBuilder.buildAssignment(lvalue, rvalue, scope);
        }
    }

    private boolean isIntructionWithAssignment(String[] instrParts) {
        return (MIN_COUNT_OF_PARTS_WHEN_ASSIGN == instrParts.length)
                && (ASSIGNMENT.equals(instrParts[ASSIGN_POSSITION]));
    }
}
