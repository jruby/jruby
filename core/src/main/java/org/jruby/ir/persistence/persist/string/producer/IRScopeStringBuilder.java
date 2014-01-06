package org.jruby.ir.persistence.persist.string.producer;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.persistence.persist.string.IRToStringTranslator;
import org.jruby.parser.StaticScope;

public class IRScopeStringBuilder extends AbstractIRStringBuilder<IRScope> {
    private static final String LINE_TERMINATOR = "\n";
    private static final String PARAMETER_LIST_START_MARKER = "<";
    private static final String PARAMETER_SEPARATOR = ", ";
    private static final String PARAMETER_LIST_END_MARKER = ">";

    public IRScopeStringBuilder(StringBuilder parentBuilder) {
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

    public void appendScopeInfo(IRScope irScope) {
        builder.append(irScope.getScopeType());

        String name = irScope.getName();
        int lineNumber = irScope.getLineNumber();
        IRScope lexicalParent = irScope.getLexicalParent();
        StaticScope staticScope = irScope.getStaticScope();

        if (irScope instanceof IRClosure) {
            IRClosure irClosure = (IRClosure) irScope;

            boolean forLoopBody = irClosure.isForLoopBody();
            int arityValue = irClosure.getArity().getValue();
            int argumentType = irClosure.getArgumentType();

            appendParameters(name, lineNumber, lexicalParent, staticScope, forLoopBody, arityValue, argumentType);
        } else {
            appendParameters(name, lineNumber, lexicalParent, staticScope);
        }
        finishLine();
    }

    public void appendInstructions(IRScope irScope) {
        appendEscapedString(irScope.getName()+":"+irScope.getLineNumber());
        finishLine();
        for (Instr instr : irScope.getInstrs()) {
            IRToStringTranslator.continueTranslation(builder, instr);
            finishLine();
        }
    }

    public void finishLine() {
        builder.append(LINE_TERMINATOR);
    }
}
