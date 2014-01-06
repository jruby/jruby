package org.jruby.ir.persistence.persist.string.producer;

import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;

public class IRInstructionStringBuilder extends AbstractIRStringBuilder<Instr> {
    private static final String PARAMETER_LIST_START_MARKER = "(";
    private static final String PARAMETER_SEPARATOR = ", ";
    private static final String PARAMETER_LIST_END_MARKER = ")";

    private static final String EQUAL = " = ";

    private static final String DEAD_MARKER = "[DEAD]";
    private static final String HAS_UNUSED_RESULT_MARKER = "[DEAD-RESULT]";

    public IRInstructionStringBuilder(StringBuilder parentBuilder) {
        super(parentBuilder);
    }

    @Override
    String getParameterListStartMarker() {
        return PARAMETER_LIST_START_MARKER;
    }

    @Override
    String getParameterSeparator() {
        return PARAMETER_SEPARATOR;
    }

    @Override
    String getParameterListEndMarker() {
        return PARAMETER_LIST_END_MARKER;
    }

    public void appendPrefix(Instr instr) {
        if (instr instanceof ResultInstr) {  // '%v_2 ='
            builder.append(((ResultInstr) instr).getResult()).append(EQUAL);
        }

        builder.append(instr.getOperation()); // 'copy' Note: Not really a prefix in my mind
    }

    public void appendMarkers(Instr instr) {
        if (instr.hasUnusedResult()) builder.append(HAS_UNUSED_RESULT_MARKER);
        if (instr.isDead()) builder.append(DEAD_MARKER);
    }
}

