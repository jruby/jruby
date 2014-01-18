package org.jruby.ir.persistence.persist.string.producer;

import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryLocalVariable;

public class IROperandStringBuilder extends AbstractIRStringBuilder<Operand> {
    private static final String PARAMETER_LIST_START_MARKER = "{";
    private static final String PARAMETER_SEPARATOR = ", ";
    private static final String PARAMETER_LIST_END_MARKER = "}";

    public IROperandStringBuilder(StringBuilder parentBuilder) {
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

    public void appendOperandType(Operand operand) {
        if (!(operand instanceof TemporaryLocalVariable)) {
        builder.append(operand.getOperandType());
        }
    }
}

