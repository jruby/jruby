/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import java.util.Arrays;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.AbstractStack;
import org.jruby.util.collections.StackElement;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class ScopeStack extends AbstractStack {
    private Ruby runtime;

    public ScopeStack(Ruby runtime) {
        this.runtime = runtime;
    }

    public void push(String[] localNames) {
        push(Arrays.asList(localNames));
    }

    public void push(List localNames) {
        push();
        resetLocalVariables(localNames);
    }

    public void push(StackElement newElement) {
        if (current() != null) {
            //current().setVisibility(ruby.getCurrentVisibility());
        }
        super.push(newElement);
        //ruby.setCurrentVisibility(Visibility.PUBLIC);
    }

    public void push() {
        this.push(new Scope(runtime));
    }

    public StackElement pop() {
        Scope result = (Scope) super.pop();
        //ruby.setCurrentVisibility(result.getVisibility());
        return result;
    }
    
    public void setTop(StackElement newElement) {
        top = newElement;
    }

    public Scope current() {
        return (Scope) top;
    }

    public IRubyObject getSuperObject() {
        return current().getSuperObject();
    }

    public void setSuperObject(IRubyObject superObject) {
        current().setSuperObject(superObject);
    }

    public List getLocalNames() {
        return current().getLocalNames();
    }

    public void resetLocalVariables(List localNames) {
        current().resetLocalVariables(localNames);
    }

    public void addLocalVariables(List localNames) {
        current().addLocalVariables(localNames);
    }

    public boolean hasLocalVariables() {
        return current().hasLocalVariables();
    }

    public IRubyObject getValue(int count) {
        return current().getValue(count);
    }

    public void setValue(int count, IRubyObject value) {
        current().setValue(count, value);
    }

    public IRubyObject getLastLine() {
        return current().getLastLine();
    }

    public void setLastLine(IRubyObject value) {
        current().setLastLine(value);
    }

    public IRubyObject getBackref() {
        return current().getBackref();
    }

    public void setBackref(IRubyObject match) {
        current().setBackref(match);
    }


}
