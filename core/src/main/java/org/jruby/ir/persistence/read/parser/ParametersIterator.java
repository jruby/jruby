package org.jruby.ir.persistence.read.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Operand;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;

public class ParametersIterator {
    private final IRParsingContext context;
    private final Iterator<Object> parametersIterator;

    public ParametersIterator(IRParsingContext context, List<Object> parameters) {
        this.context = context;
        this.parametersIterator = parameters.iterator();
    }

    public ParametersIterator(IRParsingContext context, Object parameter) {
        List<Object> listWithSingleParam = new ArrayList<Object>(1);
        listWithSingleParam.add(parameter);
        this.parametersIterator = listWithSingleParam.iterator();

        this.context = context;
    }

    public Operand nextOperand() {
        return (Operand) parametersIterator.next();
    }

    public IRScope nextScope() {
        String scopeName = nextString();

        IRScope scope = context.getScopeByName(scopeName);
        if(scope == null) {
            if(scopeName != null && scopeName.startsWith("Object")) {
                return context.getIRManager().getObject();
            } else {
                return null;
            }
        } else {
            return scope;
        }
    }

    public IRStaticScope nextStaticScope(IRScope lexicalParent) {
        String typeString = nextString();
        StaticScope.Type type = NonIRObjectFactory.createStaticScopeType(typeString);

        List<Object> namesList = nextList();
        String[] names = new String[namesList.size()];
        namesList.toArray(names);

        StaticScope parent = null;
        if(lexicalParent != null) {
            parent = lexicalParent.getStaticScope();
        }

        IRStaticScope staticScope = IRStaticScopeFactory.newStaticScope(parent, type, names);

        int requiredArgs = nextInt();

        staticScope.setRequiredArgs(requiredArgs);


        return staticScope;
    }

    public String nextString() {
        return (String) parametersIterator.next();
    }

    public List<Operand> nextOperandList() {
        @SuppressWarnings("unchecked")
        List<Operand> operands = (List<Operand>) (List<?>) nextList();
        return operands;
    }

    public Operand[] nextOperandArray() {
        List<Operand> argsList = nextOperandList();
        Operand[] args;
        if (argsList != null) {
            args = new Operand[argsList.size()];
            argsList.toArray(args);
        } else {
            args = Operand.EMPTY_ARRAY;
        }
        return args;
    }

    public List<Object> nextList() {
        @SuppressWarnings("unchecked")
        List<Object> parameters = (List<Object>) parametersIterator.next();
        return parameters;
    }

    public boolean nextBoolean() {
        return Boolean.parseBoolean(nextString());
    }

    public int nextInt() {
        return Integer.parseInt(nextString());
    }

    public Object next() {
        return parametersIterator.next();
    }

    public boolean hasNext() {
        return parametersIterator.hasNext();
    }

}

