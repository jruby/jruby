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
import org.jruby.ast.DefNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.AbstractNodeVisitor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    private volatile DefNode defNode;

    public IRMethod(IRManager manager, IRScope lexicalParent, DefNode defn, ByteList name,
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
        List<String> ivarNames = new ArrayList<>();

        DefNode def = defNode;
        if (def != null) {
            // walk AST
            def.getBodyNode().accept(new AbstractNodeVisitor<Object>() {
                @Override
                protected Object defaultVisit(Node node) {
                    if (node == null) return null;

                    if (node instanceof InstVarNode) {
                        ivarNames.add(((InstVarNode) node).getName().idString());
                    } else if (node instanceof InstAsgnNode) {
                        ivarNames.add(((InstAsgnNode) node).getName().idString());
                    }

                    node.childNodes().forEach(this::defaultVisit);

                    return null;
                }
            });
        } else {
            InterpreterContext context = lazilyAcquireInterpreterContext();

            // walk instructions
            for (Instr i : context.getInstructions()) {
                switch (i.getOperation()) {
                    case GET_FIELD:
                        ivarNames.add(((GetFieldInstr) i).getId());
                        break;
                    case PUT_FIELD:
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
    public synchronized InterpreterContext builtInterperterContextForJavaConstructor() {
        InterpreterContext interpreterContext = builtInterpreterContext();

        if (usesSuper()) { // We know at least one super is in here somewhere
            int ipc = 0;
            int superIPC = -1;
            CallBase superCall = null;
            Map<Label, Integer> labels = new HashMap<>();
            List<Label> earlyJumps = new ArrayList<>();

            for(Instr instr: interpreterContext.getInstructions()) {
                if (instr instanceof CallBase && ((CallBase) instr).getCallType() == CallType.SUPER) {
                    // We have already found one super call already.  No analysis yet to figure out if this is
                    // still ok or not so we will error.
                    if (superCall != null) throw getManager().getRuntime().newRuntimeError("Found multiple supers in java ctor");
                    superCall = ((CallBase) instr);
                    superIPC = ipc;
                } else if (instr instanceof JumpTargetInstr) {
                    Label label = ((JumpTargetInstr) instr).getJumpTarget();
                    Integer labelIPC = labels.get(label);

                    if (superIPC != -1) { // after super
                        if (labelIPC != null && labelIPC < superIPC) { // is label before super
                            throw getManager().getRuntime().newRuntimeError("backward control flow found around super");
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

            if (!earlyJumps.isEmpty()) throw getManager().getRuntime().newRuntimeError("forward control flow found around super");

            if (superIPC != -1) {
                return new ExitableInterpreterContext(interpreterContext, superCall, superIPC);
            }
        }

        return interpreterContext;
    }

    final InterpreterContext lazilyAcquireInterpreterContext() {
        if (!hasBeenBuilt()) buildMethodImpl();

        return interpreterContext;
    }

    private synchronized void buildMethodImpl() {
        if (hasBeenBuilt()) return;

        IRBuilder.topIRBuilder(getManager(), this).
                defineMethodInner(defNode, getLexicalParent(), getCoverageMode()); // sets interpreterContext
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
        assert scopeDepth == 0: "Local variable depth in IRMethod should always be zero (" + name + " had depth of " + scopeDepth + ")";
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
