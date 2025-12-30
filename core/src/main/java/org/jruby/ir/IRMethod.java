/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.ir.builder.IRBuilder;
import org.jruby.ir.builder.LazyMethodDefinition;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpTargetInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ivars.MethodData;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.api.Error.runtimeError;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    private volatile LazyMethodDefinition defNode;

    public IRMethod(IRManager manager, IRScope lexicalParent, LazyMethodDefinition defn, ByteList name,
                    boolean isInstanceMethod, int lineNumber, StaticScope staticScope, int coverageMode) {
        super(manager, lexicalParent, name, lineNumber, staticScope, coverageMode);

        this.defNode = defn;
        this.isInstanceMethod = isInstanceMethod;


        if (staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    @Override
    public boolean hasBeenBuilt() {
        return defNode == null;
    }

    public MethodData getMethodData() {
        List<String> ivarNames;

        LazyMethodDefinition def = defNode;
        if (def != null) { // walk AST
            ivarNames = def.getMethodData();
        } else {
            ivarNames = Collections.EMPTY_LIST;
            InterpreterContext context = lazilyAcquireInterpreterContext();

            // walk instructions
            for (Instr i : context.getInstructions()) {
                switch (i.getOperation()) {
                    case GET_FIELD:
                        if (ivarNames == Collections.EMPTY_LIST) ivarNames = new ArrayList<>(4);
                        ivarNames.add(((GetFieldInstr) i).getId());
                        break;
                    case PUT_FIELD:
                        if (ivarNames == Collections.EMPTY_LIST) ivarNames = new ArrayList<>(4);
                        ivarNames.add(((PutFieldInstr) i).getId());
                        break;
                }
            }
        }

        return new MethodData(getId(), getFile(), ivarNames);
    }

    @Override
    public InterpreterContext builtInterpreterContext() {
        return lazilyAcquireInterpreterContext();
    }

    /**
     * initialize methods in reified Java types will try and dispatch to the Java base classes
     * constructor when the Ruby in the initialize:
     *
     *  a) The super call is still valid in terms of Java (e.g. you cannot access self/this before the super call
     *  b) We can detect the validity of 'a'.  Limitations like super in all paths of branches is not supported (for now).
     *
     * In cases where no super exists or it is unsupported we will return a normal interpreter (and a warning when
     * unsupported):
     *
     * @return appropriate interpretercontext
     */
    public ExitableInterpreterContext builtInterpreterContextForJavaConstructor() {
        ExitableInterpreterContext interpreterContextForJavaConstructor = this.interpreterContextForJavaConstructor;
        if (interpreterContextForJavaConstructor == null) {
            synchronized (this) {
                interpreterContextForJavaConstructor = this.interpreterContextForJavaConstructor;
                if (interpreterContextForJavaConstructor == null) {
                    interpreterContextForJavaConstructor = builtInterpreterContextForJavaConstructorImpl();
                    this.interpreterContextForJavaConstructor = interpreterContextForJavaConstructor;
                }
            }
        }
        return interpreterContextForJavaConstructor == ExitableInterpreterContext.NULL ? null : interpreterContextForJavaConstructor;
    }

    private volatile ExitableInterpreterContext interpreterContextForJavaConstructor;

    private synchronized ExitableInterpreterContext builtInterpreterContextForJavaConstructorImpl() {
        final InterpreterContext interpreterContext = builtInterpreterContext();
        if (usesSuper()) {
            // We know at least one super is in here somewhere
            int ipc = 0;
            int superIPC = -1;
            CallBase superCall = null;
            Map<Label, Integer> labels = new HashMap<>(1);
            List<Label> earlyJumps = new ArrayList<>(1);
            var context = getManager().getRuntime().getCurrentContext();

            for (Instr instr: interpreterContext.getInstructions()) {
                if (instr instanceof CallBase && ((CallBase) instr).getCallType() == CallType.SUPER) {
                    // We have already found one super call already.  No analysis yet to figure out if this is
                    // still ok or not so we will error.
                    if (superCall != null) throw runtimeError(context, "Found multiple supers in Java-calling constructor. See https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class");
                    superCall = ((CallBase) instr);
                    superIPC = ipc;
                } else if (instr instanceof JumpTargetInstr) {
                    Label label = ((JumpTargetInstr) instr).getJumpTarget();
                    Integer labelIPC = labels.get(label);

                    if (superIPC != -1) { // after super
                        if (labelIPC != null && labelIPC < superIPC) { // is label before super
                            throw runtimeError(context, "Backward control flow found around Java-calling super. See https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class");
                        }
                    } else if (labelIPC == null) { // forward jump since we have not seen label yet.
                        earlyJumps.add(label);
                    }
                } else if (instr instanceof LabelInstr) {
                    Label label = ((LabelInstr) instr).getLabel();
                    labels.put(label, ipc);

                    if (superIPC == -1) { // before
                        // We found forward jump from an pre-super label and it is still before super.
                        if (earlyJumps.contains(label)) earlyJumps.remove(label);
                    }
                }

                ipc++;
            }

            if (!earlyJumps.isEmpty()) throw runtimeError(context, "Forward control flow found around Java-calling super. See https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class");

            if (superIPC != -1) {
                return new ExitableInterpreterContext(interpreterContext, superCall, superIPC);
            }
        }

        return ExitableInterpreterContext.NULL;
    }

    /**
     * This method was renamed (due a typo).
     * @see #builtInterpreterContextForJavaConstructor()
     */
    @Deprecated(since = "9.3.11.0")
    public ExitableInterpreterContext builtInterperterContextForJavaConstructor() {
        return builtInterpreterContextForJavaConstructor();
    }

    public final InterpreterContext lazilyAcquireInterpreterContext() {
        if (!hasBeenBuilt()) buildMethodImpl();

        return interpreterContext;
    }

    private synchronized void buildMethodImpl() {
        if (hasBeenBuilt()) return;

        IRBuilder builder = defNode.getBuilder(getManager(), this);
        builder.executesOnce = false; // set up so nested things (modules+) which think it could execute once knows it cannot (it is in a method).
        builder.defineMethodInner(defNode, getLexicalParent(), getCoverageMode()); // sets interpreterContext
        this.defNode = null;
    }

    public BasicBlock[] prepareForCompilation() {
        buildMethodImpl();

        return super.prepareForCompilation();
    }

    @Override
    public IRScopeType getScopeType() {
        return isInstanceMethod ? IRScopeType.INSTANCE_METHOD : IRScopeType.CLASS_METHOD;
    }

    @Override
    protected LocalVariable findExistingLocalVariable(RubySymbol name, int scopeDepth) {
        return localVars.get(name);
    }

    @Override
    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        return lvar;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return argDesc;
    }


    /**
     * Set upon completion of IRBuild of this IRMethod.
     */
    public void setArgumentDescriptors(ArgumentDescriptor[] argDesc) {
        this.argDesc = argDesc;
    }
}
