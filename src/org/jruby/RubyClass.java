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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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

import java.util.HashMap;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.collections.SinglyLinkedList;

/**
 *
 * @author  jpetersen
 */
public class RubyClass extends RubyModule {
	
	private final IRuby runtime;

    /**
     * @mri rb_boot_class
     */
    protected RubyClass(RubyClass superClass) {
        super(superClass.getRuntime(), superClass.getRuntime().getClass("Class"), superClass, null, null);

        infectBy(superClass);
        this.runtime = superClass.getRuntime();
    }

    protected RubyClass(IRuby runtime, RubyClass superClass) {
        super(runtime, null, superClass, null, null);
        this.runtime = runtime;
    }

    protected RubyClass(IRuby runtime, RubyClass metaClass, RubyClass superClass) {
        super(runtime, metaClass, superClass, null, null);
        this.runtime = runtime;
    }
    
    protected RubyClass(IRuby runtime, RubyClass metaClass, RubyClass superClass, SinglyLinkedList parentCRef, String name) {
        super(runtime, metaClass, superClass, parentCRef, name);
        this.runtime = runtime;
    }
    
    /* (non-Javadoc)
	 * @see org.jruby.RubyObject#getRuntime()
	 */
	public IRuby getRuntime() {
		return runtime;
	}

    public boolean isModule() {
        return false;
    }

    public boolean isClass() {
        return true;
    }

    public static void createClassClass(RubyClass classClass) {
        CallbackFactory callbackFactory = classClass.getRuntime().callbackFactory(RubyClass.class);
        classClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newClass"));
        classClass.defineMethod("allocate", callbackFactory.getMethod("allocate"));
        classClass.defineMethod("new", callbackFactory.getOptMethod("newInstance"));
        classClass.defineMethod("superclass", callbackFactory.getMethod("superclass"));
        classClass.defineSingletonMethod("inherited", callbackFactory.getSingletonMethod("inherited", IRubyObject.class));
        classClass.undefineMethod("module_function");
    }
    
    public static IRubyObject inherited(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().getNil();
    }

    /** Invokes if  a class is inherited from an other  class.
     * 
     * MRI: rb_class_inherited
     * 
     * @since Ruby 1.6.7
     * 
     */
    public void inheritedBy(RubyClass superType) {
        if (superType == null) {
            superType = getRuntime().getObject();
        }
        superType.callMethod("inherited", this);
    }

    /** rb_singleton_class_clone
     *
     */
    public RubyClass getSingletonClassClone() {
        if (!isSingleton()) {
            return this;
        }

        MetaClass clone = new MetaClass(getRuntime(), getMetaClass(), getSuperClass().getCRef());
        clone.initCopy(this);
        clone.setInstanceVariables(new HashMap(getInstanceVariables()));

        return (RubyClass) cloneMethods(clone);
    }

    public boolean isSingleton() {
        return false;
    }

    public RubyClass getMetaClass() {
        RubyClass type = super.getMetaClass();

        return type != null ? type : getRuntime().getClass("Class");
    }

    public RubyClass getRealClass() {
        return this;
    }

    public MetaClass newSingletonClass(SinglyLinkedList parentCRef) {
        MetaClass newClass = new MetaClass(getRuntime(), this, parentCRef);
        newClass.infectBy(this);
        return newClass;
    }

    public static RubyClass newClass(IRuby runtime, RubyClass superClass, SinglyLinkedList parentCRef, String name) {
        return new RubyClass(runtime, runtime.getClass("Class"), superClass, parentCRef, name);
    }

    /** Create a new subclass of this class.
     * @return the new sublass
     * @throws TypeError if this is class `Class'
     * @mri rb_class_new
     */
    protected RubyClass subclass() {
        if (this == getRuntime().getClass("Class")) {
            throw getRuntime().newTypeError("can't make subclass of Class");
        }
        return new RubyClass(this);
    }

    /** rb_class_new_instance
     *
     */
    public IRubyObject newInstance(IRubyObject[] args) {
        IRubyObject obj = allocate();
        obj.callInit(args);
        return obj;
    }

    /** rb_class_s_new
     *
     */
    public static RubyClass newClass(IRubyObject recv, IRubyObject[] args) {
        final IRuby runtime = recv.getRuntime();

        RubyClass superClass;
        if (args.length > 0) {
            if (args[0] instanceof RubyClass) {
                superClass = (RubyClass) args[0];
            } else {
                throw runtime.newTypeError(
                    "wrong argument type " + args[0].getType().getName() + " (expected Class)");
            }
        } else {
            superClass = runtime.getObject();
        }

        RubyClass newClass = superClass.subclass();

        newClass.makeMetaClass(superClass.getMetaClass(), runtime.getCurrentContext().peekCRef());

        // call "initialize" method
        newClass.callInit(args);

        // call "inherited" method of the superclass
        newClass.inheritedBy(superClass);

		if (runtime.getCurrentContext().isBlockGivenAndAvailable()) {
			runtime.getCurrentContext().yield(null, newClass, newClass, false);
		}

		return newClass;
    }

    /** Return the real super class of this class.
     * 
     * rb_class_superclass
     *
     */
    public IRubyObject superclass() {
        RubyClass superClass = getSuperClass();
        while (superClass != null && superClass.isIncluded()) {
            superClass = superClass.getSuperClass();
        }

        return superClass != null ? superClass : getRuntime().getNil();
    }

    /** rb_class_s_inherited
     *
     */
    public static IRubyObject inherited(RubyClass recv) {
        throw recv.getRuntime().newTypeError("can't make subclass of Class");
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('c');
        output.dumpString(getName());
    }

    public static RubyModule unmarshalFrom(UnmarshalStream output) throws java.io.IOException {
        return (RubyClass) RubyModule.unmarshalFrom(output);
    }

    /**
     * Creates a new object of this class by calling the 'allocateObject' method.
     * This class must be the type of the new object.
     * 
     * @return the new allocated object.
     */
    public IRubyObject allocate() {
        IRubyObject newObject = allocateObject();
        if (newObject.getType() != getRealClass()) {
            throw getRuntime().newTypeError("wrong instance allocation");
        }
        return newObject;
    }

    /**
     * <p>
     * This method is a constructor for ruby objects. It is called by the "Class#new",
     * "Object#clone" and Object#dup" to create new object instances.
     * </p>
     * <p>
     * Builtin meta classes (subclasses of {@link ObjectMetaClass}) have to override this method to
     * create instances of the corresponding subclass of RubyObject.
     * </p>
     * <p>
     * (mri: rb_class_allocate_instance)
     * </p>
     * 
     * @return a new RubyObject
     */
    protected IRubyObject allocateObject() {
        IRubyObject newObject = new RubyObject(runtime, this);
        return newObject;
    }

    public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
        RubyClass newClass = new RubyClass(runtime, runtime.getClass("Class"), this, parentCRef, name);

        newClass.makeMetaClass(getMetaClass(), newClass.getCRef());
        newClass.inheritedBy(this);

        ((RubyModule)parentCRef.getValue()).setConstant(name, newClass);

        return newClass;
    }
    
    protected IRubyObject doClone() {
    	return RubyClass.newClass(getRuntime(), getSuperClass(), null/*FIXME*/, getBaseName());
    }
}
