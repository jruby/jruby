/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

package org.jruby.runtime.block;

import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.DynamicScope;

/**
 * A lambda block.
 */
public class LambdaBlock extends Block {
    protected LambdaBlock(BlockBody body, Binding binding) {
        super(body, binding, Type.LAMBDA);
    }

    protected LambdaBlock(BlockBody body, Binding binding, Block escapeBlock) {
        super(body, binding, Type.LAMBDA, escapeBlock);
    }

    public static LambdaBlock newBlock(BlockBody body) {
        return new LambdaBlock(body, Binding.DUMMY);
    }

    public static LambdaBlock newBlock(BlockBody body, Binding binding) {
        return new LambdaBlock(body, binding);
    }

    public static LambdaBlock newBlock(BlockBody body, Binding binding, Block escapeBlock) {
        return new LambdaBlock(body, binding, escapeBlock);
    }

    public LambdaBlock toLambda() {
        return this;
    }

    public Block toType(Type type) {
        switch (type) {
            case PROC: return toProc();
            case LAMBDA: return this;
            case THREAD: return toThread();
            case NORMAL: return toNormal();
        }
        throw new RuntimeException("should not get here");
    }

    public boolean isLambda() {
        return true;
    }

    public DynamicScope allocScope(DynamicScope parentScope) {
        // FIXME: Rather than modify static-scope, it seems we ought to set a field in block-body which is then
        // used to tell dynamic-scope that it is a dynamic scope for a thread body.  Anyway, to be revisited later!
        DynamicScope newScope = super.allocScope(parentScope);

        newScope.setLambda(true);

        return newScope;
    }

    public LambdaBlock cloneBlock() {
        return new LambdaBlock(body, binding, escapeBlock);
    }

    public LambdaBlock cloneBlockAndBinding() {
        return new LambdaBlock(body, binding.clone(), this);
    }

    public LambdaBlock cloneBlockAndFrame() {
        return new LambdaBlock(body, binding.cloneAndDupFrame(), this);
    }
}
