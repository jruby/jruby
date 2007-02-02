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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ThreadContext;
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
    
    // the default allocator
    private final ObjectAllocator allocator;
    
    private ObjectMarshal marshal;
    
    private static final ObjectMarshal DEFAULT_OBJECT_MARSHAL = new ObjectMarshal() {
        public void marshalTo(IRuby runtime, Object obj, RubyClass type,
                              MarshalStream marshalStream) throws IOException {
            IRubyObject object = (IRubyObject)obj;
            
            Map iVars = object.getInstanceVariablesSnapshot();
            
            marshalStream.dumpInstanceVars(iVars);
        }

        public Object unmarshalFrom(IRuby runtime, RubyClass type,
                                    UnmarshalStream unmarshalStream) throws IOException {
            IRubyObject result = type.allocate();
            
            unmarshalStream.registerLinkTarget(result);

            unmarshalStream.defaultInstanceVarsUnmarshal(result);

            return result;
        }
    };

    /**
     * @mri rb_boot_class
     */
    
    /**
     * @mri rb_boot_class
     */
    
    /**
     * @mri rb_boot_class
     */
    
    /**
     * @mri rb_boot_class
     */
    protected RubyClass(RubyClass superClass, ObjectAllocator allocator) {
        this(superClass.getRuntime(), superClass.getRuntime().getClass("Class"), superClass, allocator, null, null);

        infectBy(superClass);
    }

    protected RubyClass(IRuby runtime, RubyClass superClass, ObjectAllocator allocator) {
        this(runtime, null, superClass, allocator, null, null);
    }

    protected RubyClass(IRuby runtime, RubyClass metaClass, RubyClass superClass, ObjectAllocator allocator) {
        this(runtime, metaClass, superClass, allocator, null, null);
    }
    
    protected RubyClass(IRuby runtime, RubyClass metaClass, RubyClass superClass, ObjectAllocator allocator, SinglyLinkedList parentCRef, String name) {
        super(runtime, metaClass, superClass, parentCRef, name);
        this.allocator = allocator;
        this.runtime = runtime;
        
        // use parent's marshal, or default object marshal by default
        if (superClass != null) {
            this.marshal = superClass.getMarshal();
        } else {
            this.marshal = DEFAULT_OBJECT_MARSHAL;
        }
    }
    
    public int getNativeTypeIndex() {
        return ClassIndex.CLASS;
    }
    
    public final IRubyObject allocate() {
        return getAllocator().allocate(getRuntime(), this);
    }
    
    public final ObjectMarshal getMarshal() {
        return marshal;
    }
    
    public final void setMarshal(ObjectMarshal marshal) {
        this.marshal = marshal;
    }
    
    public final void marshal(Object obj, MarshalStream marshalStream) throws IOException {
        getMarshal().marshalTo(getRuntime(), obj, this, marshalStream);
    }
    
    public final Object unmarshal(UnmarshalStream unmarshalStream) throws IOException {
        return getMarshal().unmarshalFrom(getRuntime(), this, unmarshalStream);
    }
    
    public static RubyClass newClassClass(IRuby runtime, RubyClass moduleClass) {
        ObjectAllocator defaultAllocator = new ObjectAllocator() {
            public IRubyObject allocate(IRuby runtime, RubyClass klass) {
                IRubyObject instance = new RubyObject(runtime, klass);
                instance.setMetaClass(klass);

                return instance;
            }
        };
        
        RubyClass classClass = new RubyClass(
                runtime,
                null /* FIXME: should be something else? */,
                moduleClass,
                defaultAllocator,
                null,
                "Class");
        
        classClass.index = ClassIndex.CLASS;
        
        return classClass;
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
        classClass.getMetaClass().defineMethod("new", callbackFactory.getOptSingletonMethod("newClass"));
        classClass.defineFastMethod("allocate", callbackFactory.getFastMethod("allocate"));
        classClass.defineMethod("new", callbackFactory.getOptMethod("newInstance"));
        classClass.defineMethod("superclass", callbackFactory.getMethod("superclass"));
        classClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy", IRubyObject.class));
        classClass.getMetaClass().defineMethod("inherited", callbackFactory.getSingletonMethod("inherited", IRubyObject.class));
        classClass.undefineMethod("module_function");
    }
    
    public static IRubyObject inherited(IRubyObject recv, IRubyObject arg, Block block) {
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
        superType.callMethod(getRuntime().getCurrentContext(), "inherited", this);
    }

    /** rb_singleton_class_clone
     *
     */
    public RubyClass getSingletonClassClone() {
        if (!isSingleton()) {
            return this;
        }
        
        MetaClass clone = new MetaClass(getRuntime(), getSuperClass(), getMetaClass().getAllocator(), getSuperClass().getCRef());

        clone.setInstanceVariables(new HashMap(getInstanceVariables()));

        cloneMethods(clone);

        return clone;
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
        MetaClass newClass = new MetaClass(getRuntime(), this, this.getAllocator(), parentCRef);
        newClass.infectBy(this);
        return newClass;
    }

    public static RubyClass newClass(IRuby runtime, RubyClass superClass, SinglyLinkedList parentCRef, String name) {
        return new RubyClass(runtime, runtime.getClass("Class"), superClass, superClass.getAllocator(), parentCRef, name);
    }

    public static RubyClass cloneClass(IRuby runtime, RubyClass metaClass, RubyClass superClass, ObjectAllocator allocator, SinglyLinkedList parentCRef, String name) {
        return new RubyClass(runtime, metaClass, superClass, allocator, parentCRef, name);
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
        return new RubyClass(this, getAllocator());
    }

    /** rb_class_new_instance
     *
     */
    public IRubyObject newInstance(IRubyObject[] args, Block block) {
        IRubyObject obj = (IRubyObject) allocate();
        obj.callInit(args, block);
        return obj;
    }
    
    public ObjectAllocator getAllocator() {
        return allocator;
    }

    /** rb_class_s_new
     *
     */
    public static RubyClass newClass(IRubyObject recv, IRubyObject[] args, Block block) {
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

        ThreadContext tc = runtime.getCurrentContext();
        // use allocator of superclass, since this will be a pure Ruby class
        RubyClass newClass = superClass.newSubClass(null, superClass.getAllocator(),tc.peekCRef());

        // call "initialize" method
        newClass.callInit(args, block);

        // call "inherited" method of the superclass
        newClass.inheritedBy(superClass);

		if (block != null) block.yield(tc, null, newClass, newClass, false);

		return newClass;
    }

    /** Return the real super class of this class.
     * 
     * rb_class_superclass
     *
     */
    public IRubyObject superclass(Block block) {
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

    public static void marshalTo(RubyClass clazz, MarshalStream output) throws java.io.IOException {
        output.writeString(clazz.getName());
    }

    public static RubyModule unmarshalFrom(UnmarshalStream output) throws java.io.IOException {
        return (RubyClass) RubyModule.unmarshalFrom(output);
    }

    public RubyClass newSubClass(String name, ObjectAllocator allocator, SinglyLinkedList parentCRef) {
        RubyClass classClass = runtime.getClass("Class");
        
        // Cannot subclass 'Class' or metaclasses
        if (this == classClass) {
            throw runtime.newTypeError("can't make subclass of Class");
        } else if (this instanceof MetaClass) {
            throw runtime.newTypeError("can't make subclass of virtual class");
        }

        RubyClass newClass = new RubyClass(runtime, classClass, this, allocator, parentCRef, name);

        newClass.makeMetaClass(getMetaClass(), newClass.getCRef());
        newClass.inheritedBy(this);

        if(null != name) {
            ((RubyModule)parentCRef.getValue()).setConstant(name, newClass);
        }

        return newClass;
    }
    
    protected IRubyObject doClone() {
    	return RubyClass.cloneClass(getRuntime(), getMetaClass(), getSuperClass(), getAllocator(), null/*FIXME*/, null);
    }
    
    /** rb_class_init_copy
     * 
     */
    public IRubyObject initialize_copy(IRubyObject original){

        if (((RubyClass) original).isSingleton()){
            throw getRuntime().newTypeError("can't copy singleton class");
        }
        
        super.initialize_copy(original);
        
        return this;        
    }    
}
