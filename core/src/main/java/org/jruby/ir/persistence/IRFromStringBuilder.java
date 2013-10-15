package org.jruby.ir.persistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.ir.instructions.Instr;

public class IRFromStringBuilder {

    private static final String INSTR_WITH_OPERANDS_REGEXP = "(%?\\w+)" + "\\("
            + "([^,],)*" + "([^,])" + "\\)";
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

    public Instr build(String instrString) throws IRPersistenceException {
        String[] instrParts = instrString.split(WHITESPACES, MAX_COUNT_OF_INSTR_PARTS);
        if (isIntructionWithAssignment(instrParts)) {
            return buildInstrWithAssignment(instrParts);
        }

        Matcher instrWithOperandsMatcher = matcherForInstrWithOperandsAtPossition(instrParts, INSTR_WITH_OPERANDS_POSSITION);
        if (instrWithOperandsMatcher.matches()) {
            return buildInstrWithOperands(new OperationInfo(instrWithOperandsMatcher));
        }

        if (instrString.contains(LABEL_DETERMINANT)) {
            return buildLabel(instrParts[LABEL_POSSITION]);
        }
        
        // What is that
        throw new IRPersistenceException(instrString + " unknown");
    }

    private Instr buildInstrWithAssignment(String[] instrParts) {
        String lvalue = instrParts[LVALUE_POSSITION];
        Matcher instrWithOperandsMatcher = matcherForInstrWithOperandsAtPossition(instrParts, INSTR_WITH_ASSIGNMENT_AND_OPERANDS_POSSITION);
        if (instrWithOperandsMatcher.matches()) {
            OperationInfo operationInfo = new OperationInfo(instrWithOperandsMatcher);
            return buildInstrWithOperandsAndAssignment(lvalue, operationInfo);
        } else {
            int rvaluePossition = RVALUE_POSSITION;
            String rvalue = instrParts[rvaluePossition];
            return buildAssignment(lvalue, rvalue);
        }
    }

    private class OperationInfo {
        String name;
        String[] params;


        OperationInfo(Matcher instrWithOperandsMatcher) {
            name = instrWithOperandsMatcher.group(1);
            
            int groupCount = instrWithOperandsMatcher.groupCount();
            int paramCount = groupCount - 1;
            params = new String[paramCount];
            for (int i = 0; i < paramCount; i++) {
                params[i] = instrWithOperandsMatcher.group(i + 2).replace(",", "").trim();
            }
        }
    }

    private Matcher matcherForInstrWithOperandsAtPossition(String[] instrParts, int pos) {
        String possibleInstrWithOperands = instrParts[pos];
        Matcher instrWithOperandsMatcher = instrWithOperands.matcher(possibleInstrWithOperands);
        return instrWithOperandsMatcher;
    }

    private boolean isIntructionWithAssignment(String[] instrParts) {
        return (MIN_COUNT_OF_PARTS_WHEN_ASSIGN == instrParts.length)
                && (ASSIGNMENT.equals(instrParts[ASSIGN_POSSITION]));
    }

    private Instr buildInstrWithOperandsAndAssignment(String lvalue, OperationInfo operationInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    private Instr buildInstrWithOperands(OperationInfo operationInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    private Instr buildAssignment(String instrString, String rvalue) {
        // TODO Auto-generated method stub
        return null;
    }

    private Instr buildLabel(String instrString) {
        return null;
    }
}

