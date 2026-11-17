package org.jruby.embed;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.embed.variable.BiVariable;
import org.jruby.embed.variable.VariableInterceptor;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class VariableInterceptorTest {
    @Test
    public void terminateLocalVariablesShouldRemoveAllLocalVariables() {
        List<String> varNames = new ArrayList<>();
        List<BiVariable> variables = new ArrayList<>();
        int numberOfVariables = 10;
        for (int i = 0; i < numberOfVariables; i++) {
            String name = "var_" + i;
            variables.add(new MyBiVariable(name));
            varNames.add(name);
        }
        assertEquals("Variables before: " + varNames.toString(), numberOfVariables, varNames.size());
        VariableInterceptor.terminateLocalVariables(LocalVariableBehavior.TRANSIENT, varNames, variables);
        assertEquals("Variables after: " + varNames.toString(), 0, varNames.size());
        assertEquals(0, variables.size());
    }

    private static class MyBiVariable implements BiVariable {
        private final String name;
        public MyBiVariable(String name) {
            this.name = name;
        }

        @Override
        public Type getType() {
            return Type.LocalVariable;
        }

        @Override
        public IRubyObject getReceiver() {
            return null;
        }

        @Override
        public boolean isReceiverIdentical(RubyObject rubyObject) {
            return false;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getJavaObject() {
            return null;
        }

        @Override
        public void setJavaObject(Ruby ruby, Object o) {

        }

        @Override
        public void inject() {

        }

        @Override
        public IRubyObject getRubyObject() {
            return null;
        }

        @Override
        public void setRubyObject(IRubyObject iRubyObject) {

        }

        @Override
        public void remove() {

        }
    }
}

