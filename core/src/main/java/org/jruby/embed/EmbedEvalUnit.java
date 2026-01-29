/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed;

import org.jruby.ast.Node;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * Wrapper interface of {@link JavaEmbedUtils.EvalUnit} for embedding.
 * This interface defines an evaluation unit for a parsed or compiled script,
 * which are executable.
 *
 * @author Yoko Harada &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
public interface EmbedEvalUnit extends JavaEmbedUtils.EvalUnit {
    /**
     * Returns a root node of parsed Ruby script.
     *
     * @return parsed node.
     */
    public Node getNode();

    /**
     * Returns a scope used to parse a script. For example,
     * org.jruby.runtime.scope.ManyVarsDynamicScope.
     *
     * @return scope to refer local variables.
     */
    DynamicScope getLocalVarScope();

    @Deprecated(since = "9.3.0.0")
    default ManyVarsDynamicScope getScope() {
        return (ManyVarsDynamicScope) getLocalVarScope();
    }
}
