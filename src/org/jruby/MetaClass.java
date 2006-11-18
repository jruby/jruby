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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class MetaClass extends RubyClass {

    public MetaClass(IRuby runtime, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(runtime, runtime.getClass("Class"), superClass, parentCRef, null);
    }
 
    public boolean isSingleton() {
        return true;
    }
    
    public boolean isImmediate() {
        return true;
    }

    protected RubyClass subclass() {
        throw getRuntime().newTypeError("can't make subclass of virtual class");
    }

    public void attachToObject(IRubyObject object) {
        setInstanceVariable("__attached__", object);
    }
    
	public String getName() {
		return "#<Class:" + getInstanceVariable("__attached__").toString() + ">";
	}
	
	/**
	 * If an object uses an anonymous class 'class << obj', then this grabs the original 
	 * metaclass and not the one that get injected as a result of 'class << obj'.
	 */
	public RubyClass getRealClass() {
        return getSuperClass().getRealClass();
    }
    
    public void methodAdded(RubySymbol symbol) {
        getAttachedObject().callMethod(getRuntime().getCurrentContext(), "singleton_method_added", symbol);
    }

    public IRubyObject getAttachedObject() {
    	// Though it may not be obvious, attachToObject is always called just after instance
    	// creation.  Kind of a brittle arrangement here...
        return getInstanceVariable("__attached__");
    }

    public IRubyObject allocateObject() {
        throw getRuntime().newTypeError("can't create instance of virtual class");
    }
}
