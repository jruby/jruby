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

package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public class InstanceSuperInstr extends CallInstr {
    private final boolean isLiteralBlock;

    // clone constructor
    protected InstanceSuperInstr(IRScope scope, Variable result, Operand definingModule, RubySymbol name, Operand[] args,
                                 Operand closure, boolean isPotentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, Operation.INSTANCE_SUPER, CallType.SUPER, result, name, definingModule, args, closure,
                isPotentiallyRefined, callSite, callSiteId);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    // normal constructor
    public InstanceSuperInstr(IRScope scope, Variable result, Operand definingModule, RubySymbol name, Operand[] args,
                              Operand closure, boolean isPotentiallyRefined) {
        super(scope, Operation.INSTANCE_SUPER, CallType.SUPER, result, name, definingModule, args, closure, isPotentiallyRefined);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        super.computeScopeFlags(scope, flags);
        scope.setUsesSuper();

        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new InstanceSuperInstr(ii.getScope(), ii.getRenamedVariable(getResult()),
                getDefiningModule().cloneForInlining(ii), getName(), cloneCallArgs(ii),
                getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii),
                isPotentiallyRefined(), getCallSite(), getCallSiteId());
    }

    public static InstanceSuperInstr decode(IRReaderDecoder d) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super");
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, calltype(ord):  "+ callTypeOrdinal);
        RubySymbol name = d.decodeSymbol();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, methaddr:  "+ name);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return new InstanceSuperInstr(d.getCurrentScope(), d.decodeVariable(), receiver, name, args, closure, d.getCurrentScope().maybeUsingRefinements());
    }

    /*
    // We cannot convert this into a NoCallResultInstr
    @Override
    public Instr discardResult() {
        return this;
    }
    */

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);
        RubyModule definingModule = ((RubyModule) getDefiningModule().retrieve(context, self, currScope, currDynScope, temp));

        if (isLiteralBlock) {
            return IRRuntimeHelpers.instanceSuperIter(context, self, getId(), definingModule, args, block);
        } else {
            return IRRuntimeHelpers.instanceSuper(context, self, getId(), definingModule, args, block);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InstanceSuperInstr(this);
    }
}
