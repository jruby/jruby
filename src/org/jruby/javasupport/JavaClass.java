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
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby.javasupport;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.IdUtil;
import org.jruby.util.collections.IntHashMap;

public class JavaClass extends JavaObject {

    private static class AssignedName {
        // to override an assigned name, the type must be less than
        // or equal to the assigned type. so a field name in a subclass
        // will override an alias in a superclass, but not a method.
        static final int RESERVED = 0;
        static final int METHOD = 1;
        static final int FIELD = 2;
        static final int PROTECTED_METHOD = 3;
        static final int WEAKLY_RESERVED = 4; // we'll be peeved, but not devastated, if you override
        static final int ALIAS = 5;
        // yes, protected fields are weaker than aliases. many conflicts
        // in the old AWT code, for example, where you really want 'size'
        // to mean the public method getSize, not the protected field 'size'.
        static final int PROTECTED_FIELD = 6;
        String name;
        int type;
        AssignedName () {}
        AssignedName(String name, int type) {
            this.name = name;
            this.type = type;
        }
    }

    // TODO: other reserved names?
    private static final Map RESERVED_NAMES = new HashMap();
    static {
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", AssignedName.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", AssignedName.RESERVED));
        RESERVED_NAMES.put("class", new AssignedName("class", AssignedName.RESERVED));
        RESERVED_NAMES.put("initialize", new AssignedName("initialize", AssignedName.RESERVED));
        RESERVED_NAMES.put("object_id", new AssignedName("object_id", AssignedName.RESERVED));
        RESERVED_NAMES.put("private", new AssignedName("private", AssignedName.RESERVED));
        RESERVED_NAMES.put("protected", new AssignedName("protected", AssignedName.RESERVED));
        RESERVED_NAMES.put("public", new AssignedName("public", AssignedName.RESERVED));

        // weakly reserved names
        RESERVED_NAMES.put("id", new AssignedName("id", AssignedName.WEAKLY_RESERVED));
    }
    private static final Map STATIC_RESERVED_NAMES = new HashMap(RESERVED_NAMES);
    static {
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", AssignedName.RESERVED));
    }
    private static final Map INSTANCE_RESERVED_NAMES = new HashMap(RESERVED_NAMES);

    private static abstract class NamedCallback implements Callback {
        static final int STATIC_FIELD = 1;
        static final int STATIC_METHOD = 2;
        static final int INSTANCE_FIELD = 3;
        static final int INSTANCE_METHOD = 4;
        String name;
        int type;
        Visibility visibility = Visibility.PUBLIC;
        boolean isProtected;
        NamedCallback () {}
        NamedCallback (String name, int type) {
            this.name = name;
            this.type = type;
        }
        abstract void install(RubyClass proxy);
        // small hack to save a cast later on
        boolean hasLocalMethod() {
            return true;
        }
        boolean isPublic() {
            return visibility == Visibility.PUBLIC;
        }
        boolean isProtected() {
            return visibility == Visibility.PROTECTED;
        }
//        void logMessage(IRubyObject self, IRubyObject[] args) {
//            if (!DEBUG) {
//                return;
//            }
//            String type;
//            switch (this.type) {
//            case STATIC_FIELD: type = "static field"; break;
//            case STATIC_METHOD: type = "static method"; break;
//            case INSTANCE_FIELD: type = "instance field"; break;
//            case INSTANCE_METHOD: type = "instance method"; break;
//            default: type = "?"; break;
//            }
//            StringBuffer b = new StringBuffer(type).append(" => '").append(name)
//                .append("'; args.length = ").append(args.length);
//            for (int i = 0; i < args.length; i++) {
//                b.append("\n   arg[").append(i).append("] = ").append(args[i]);
//            }
//            System.out.println(b);
//        }
    }

    private static abstract class FieldCallback extends NamedCallback {
        Field field;
        JavaField javaField;
        FieldCallback(){}
        FieldCallback(String name, int type, Field field) {
            super(name,type);
            this.field = field;
//            if (Modifier.isProtected(field.getModifiers())) {
//                field.setAccessible(true);
//                this.visibility = Visibility.PROTECTED;
//            }
        }
    }

    private class StaticFieldGetter extends FieldCallback {
        StaticFieldGetter(){}
        StaticFieldGetter(String name, Field field) {
            super(name,STATIC_FIELD,field);
        }
        void install(RubyClass proxy) {
            proxy.getSingletonClass().defineFastMethod(this.name,this,this.visibility);
        }
        public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
            //logMessage(self,args);
            if (javaField == null) {
                javaField = new JavaField(getRuntime(),field);
            }
            return Java.java_to_ruby(self,javaField.static_value(),Block.NULL_BLOCK);
        }
        public Arity getArity() {
            return Arity.NO_ARGUMENTS;
        }
    }

    private class StaticFieldSetter extends FieldCallback {
        StaticFieldSetter(){}
        StaticFieldSetter(String name, Field field) {
            super(name,STATIC_FIELD,field);
        }
        void install(RubyClass proxy) {
            proxy.getSingletonClass().defineFastMethod(this.name,this,this.visibility);
        }
        public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
            //logMessage(self,args);
            if (javaField == null) {
                javaField = new JavaField(getRuntime(),field);
            }
            return Java.java_to_ruby(self,
                    javaField.set_static_value(Java.ruby_to_java(self,args[0],Block.NULL_BLOCK)),
                    Block.NULL_BLOCK);
        }
        public Arity getArity() {
            return Arity.ONE_ARGUMENT;
        }
    }

    private class InstanceFieldGetter extends FieldCallback {
        InstanceFieldGetter(){}
        InstanceFieldGetter(String name, Field field) {
            super(name,INSTANCE_FIELD,field);
        }
        void install(RubyClass proxy) {
            proxy.defineFastMethod(this.name,this,this.visibility);
        }
        public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
            //logMessage(self,args);
            if (javaField == null) {
                javaField = new JavaField(getRuntime(),field);
            }
            return Java.java_to_ruby(self,
                    javaField.value(self.getInstanceVariable("@java_object")),
                    Block.NULL_BLOCK);
        }
        public Arity getArity() {
            return Arity.NO_ARGUMENTS;
        }
    }

    private class InstanceFieldSetter extends FieldCallback {
        InstanceFieldSetter(){}
        InstanceFieldSetter(String name, Field field) {
            super(name,INSTANCE_FIELD,field);
        }
        void install(RubyClass proxy) {
            proxy.defineFastMethod(this.name,this,this.visibility);
        }
        public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
            //logMessage(self,args);
            if (javaField == null) {
                javaField = new JavaField(getRuntime(),field);
            }
            return Java.java_to_ruby(self,
                    javaField.set_value(self.getInstanceVariable("@java_object"),
                            Java.ruby_to_java(self,args[0],Block.NULL_BLOCK)),
                    Block.NULL_BLOCK);
        }
        public Arity getArity() {
            return Arity.ONE_ARGUMENT;
        }
    }

    private static abstract class MethodCallback extends NamedCallback {
        boolean haveLocalMethod;
        List methods;
        List aliases;
        IntHashMap javaMethods;
        IntHashMap matchingMethods;
        JavaMethod javaMethod;
        MethodCallback(){}
        MethodCallback(String name, int type) {
            super(name,type);
        }
        void addMethod(Method method, Class javaClass) {
            if (methods == null) {
                methods = new ArrayList();
            }
            methods.add(method);
//            if (Modifier.isProtected(method.getModifiers())) {
//                visibility = Visibility.PROTECTED;
//            }
            haveLocalMethod |= javaClass == method.getDeclaringClass();
        }
        void addAlias(String alias) {
            if (aliases == null) {
                aliases = new ArrayList();
            }
            if (!aliases.contains(alias))
                aliases.add(alias);
        }
        boolean hasLocalMethod () {
            return haveLocalMethod;
        }
        // TODO: varargs?
        // TODO: rework Java.matching_methods_internal and
        // ProxyData.method_cache, since we really don't need to be passing
        // around RubyArray objects anymore.
        void createJavaMethods(Ruby runtime) {
            if (methods != null) {
                if (methods.size() == 1) {
                    javaMethod = JavaMethod.create(runtime,(Method)methods.get(0));
                } else {
                    javaMethods = new IntHashMap();
                    matchingMethods = new IntHashMap(); 
                    for (Iterator iter = methods.iterator(); iter.hasNext() ;) {
                        Method method = (Method)iter.next();
                        // TODO: deal with varargs
                        //int arity = method.isVarArgs() ? -1 : method.getParameterTypes().length;
                        int arity = method.getParameterTypes().length;
                        RubyArray methodsForArity = (RubyArray)javaMethods.get(arity);
                        if (methodsForArity == null) {
                            methodsForArity = RubyArray.newArrayLight(runtime);
                            javaMethods.put(arity,methodsForArity);
                        }
                        methodsForArity.append(JavaMethod.create(runtime,method));
                    }
                }
                methods = null;
            }
        }
        void raiseNoMatchingMethodError(IRubyObject proxy, IRubyObject[] args, int start) {
            int len = args.length;
            List argTypes = new ArrayList(len - start);
            for (int i = start ; i < len; i++) {
                argTypes.add(((JavaClass)((JavaObject)args[i]).java_class()).getValue());
            }
            throw proxy.getRuntime().newNameError("no " + this.name + " with arguments matching " + argTypes + " on object " + proxy.callMethod(proxy.getRuntime().getCurrentContext(),"inspect"), null);
        }
    }

    private class StaticMethodInvoker extends MethodCallback {
        StaticMethodInvoker(){}
        StaticMethodInvoker(String name) {
            super(name,STATIC_METHOD);
        }
        void install(RubyClass proxy) {
            if (haveLocalMethod) {
                RubyClass singleton = proxy.getSingletonClass();
                singleton.defineFastMethod(this.name,this,this.visibility);
                if (aliases != null && isPublic() ) {
                    for (Iterator iter = aliases.iterator(); iter.hasNext(); ) {
                        singleton.defineAlias((String)iter.next(), this.name);
                    }
                    aliases = null;
                }
            }
        }
        public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
            //logMessage(self,args);
            if (javaMethod == null && javaMethods == null) {
                createJavaMethods(self.getRuntime());
            }
            // TODO: ok to convert args in place, rather than new array?
            int len = args.length;
            IRubyObject[] convertedArgs = new IRubyObject[len];
            for (int i = len; --i >= 0; ) {
                convertedArgs[i] = Java.ruby_to_java(self,args[i],Block.NULL_BLOCK);
            }
            if (javaMethods == null) {
                return Java.java_to_ruby(self,javaMethod.invoke_static(convertedArgs),Block.NULL_BLOCK); 
            } else {
                int argsTypeHash = 0;
                for (int i = len; --i >= 0; ) {
                    argsTypeHash += 3*args[i].getMetaClass().id;
                }
                IRubyObject match = (IRubyObject)matchingMethods.get(argsTypeHash);
                if (match == null) {
                    // TODO: varargs?
                    RubyArray methods = (RubyArray)javaMethods.get(len);
                    if (methods == null) {
                        raiseNoMatchingMethodError(self,convertedArgs,0);
                    }
                    match = Java.matching_method_internal(JAVA_UTILITIES, methods, convertedArgs, 0, len);
                }
                return Java.java_to_ruby(self, ((JavaMethod)match).invoke_static(convertedArgs), Block.NULL_BLOCK);
            }
        }
        public Arity getArity() {
            return Arity.OPTIONAL;
        }
    }

    private class InstanceMethodInvoker extends MethodCallback {
        InstanceMethodInvoker(){}
        InstanceMethodInvoker(String name) {
            super(name,INSTANCE_METHOD);
        }
        void install(RubyClass proxy) {
            if (haveLocalMethod) {
                proxy.defineFastMethod(this.name,this,this.visibility);
                if (aliases != null && isPublic()) {
                    for (Iterator iter = aliases.iterator(); iter.hasNext(); ) {
                        proxy.defineAlias((String)iter.next(), this.name);
                    }
                    aliases = null;
                }
            }
        }
        public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
            //logMessage(self,args);
            if (javaMethod == null && javaMethods == null) {
                createJavaMethods(self.getRuntime());
            }
            // TODO: ok to convert args in place, rather than new array?
            int len = args.length;
            if (block.isGiven()) { // convert block to argument
                len += 1;
                IRubyObject[] newArgs = new IRubyObject[args.length+1];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                newArgs[args.length] = RubyProc.newProc(self.getRuntime(), block, true);
                args = newArgs;
            }
            IRubyObject[] convertedArgs = new IRubyObject[len+1];
            convertedArgs[0] = self.getInstanceVariable("@java_object");
            int i = len;
            if (block.isGiven()) {
                convertedArgs[len] = args[len - 1];
                i -= 1;
            }
            for (; --i >= 0; ) {
                convertedArgs[i+1] = Java.ruby_to_java(self,args[i],Block.NULL_BLOCK);
            }

            if (javaMethods == null) {
                return Java.java_to_ruby(self,javaMethod.invoke(convertedArgs),Block.NULL_BLOCK);
            } else {
                int argsTypeHash = 0;
                for (i = len; --i >= 0; ) {
                    argsTypeHash += 3*args[i].getMetaClass().id;
                }
                IRubyObject match = (IRubyObject)matchingMethods.get(argsTypeHash);
                if (match == null) {
                    // TODO: varargs?
                    RubyArray methods = (RubyArray)javaMethods.get(len);
                    if (methods == null) {
                        raiseNoMatchingMethodError(self,convertedArgs,1);
                    }
                    match = Java.matching_method_internal(JAVA_UTILITIES, methods, convertedArgs, 1, len);
                    matchingMethods.put(argsTypeHash, match);
                }
                return Java.java_to_ruby(self,((JavaMethod)match).invoke(convertedArgs),Block.NULL_BLOCK);
            }
        }
        public Arity getArity() {
            return Arity.OPTIONAL;
        }
    }

    private static class ConstantField {
        static final int CONSTANT = Modifier.FINAL | Modifier.PUBLIC | Modifier.STATIC;
        final Field field;
        ConstantField(Field field) {
            this.field = field;
        }
        void install(final RubyModule proxy) {
            if (proxy.getConstantAt(field.getName()) == null) {
                final JavaField javaField = new JavaField(proxy.getRuntime(),field);
                proxy.const_set(javaField.name(),Java.java_to_ruby(proxy,javaField.static_value(),Block.NULL_BLOCK));
            }
        }
        static boolean isConstant(final Field field) {
            return (field.getModifiers() & CONSTANT) == CONSTANT &&
                Character.isUpperCase(field.getName().charAt(0));
        }
    }
    
    private final RubyModule JAVA_UTILITIES = getRuntime().getJavaSupport().getJavaUtilitiesModule();
    
    private Map staticAssignedNames;
    private Map instanceAssignedNames;
    private Map staticCallbacks;
    private Map instanceCallbacks;
    private List constantFields;
    // caching constructors, as they're accessed for each new instance
    private RubyArray constructors;
    
    private volatile ArrayList<IRubyObject> proxyExtenders;

    // proxy module for interfaces
    private volatile RubyModule proxyModule;
    
    // proxy class for concrete classes.  also used for
    // "concrete" interfaces, which is why we have two fields
    private volatile RubyClass proxyClass;

    // readable only by thread building proxy, so don't need to be
    // volatile. used to handle recursive calls to getProxyClass/Module
    // while proxy is being constructed (usually when a constant
    // defined by a class is of the same type as that class).
    private RubyModule unfinishedProxyModule;
    private RubyClass unfinishedProxyClass;
    
    private final ReentrantLock proxyLock = new ReentrantLock();
    
    public RubyModule getProxyModule() {
        // allow proxy to be read without synchronization. if proxy
        // is under construction, only the building thread can see it.
        RubyModule proxy;
        if ((proxy = proxyModule) != null) {
            // proxy is complete, return it
            return proxy;
        } else if (proxyLock.isHeldByCurrentThread()) {
            // proxy is under construction, building thread can
            // safely read non-volatile value
            return unfinishedProxyModule; 
        }
        return null;
    }
    
    public RubyClass getProxyClass() {
        // allow proxy to be read without synchronization. if proxy
        // is under construction, only the building thread can see it.
        RubyClass proxy;
        if ((proxy = proxyClass) != null) {
            // proxy is complete, return it
            return proxy;
        } else if (proxyLock.isHeldByCurrentThread()) {
            // proxy is under construction, building thread can
            // safely read non-volatile value
            return unfinishedProxyClass; 
        }
        return null;
    }
    
    public void lockProxy() {
        proxyLock.lock();
    }
    
    public void unlockProxy() {
        proxyLock.unlock();
    }

    protected Map getStaticAssignedNames() {
        return staticAssignedNames;
    }
    protected Map getInstanceAssignedNames() {
        return instanceAssignedNames;
    }
    
    private JavaClass(Ruby runtime, Class javaClass) {
        super(runtime, (RubyClass) runtime.getJavaSupport().getJavaClassClass(), javaClass);
        if (javaClass.isInterface()) {
            initializeInterface(javaClass);
        } else if (!(javaClass.isArray() || javaClass.isPrimitive())) {
            // TODO: public only?
            initializeClass(javaClass);
        }
    }
    
    private void initializeInterface(Class javaClass) {
        Map staticNames  = new HashMap(STATIC_RESERVED_NAMES);
        List constantFields = new ArrayList(); 
        Field[] fields;
        try {
            fields = javaClass.getDeclaredFields();
        } catch (SecurityException e) {
            fields = javaClass.getFields();
        }
        for (int i = fields.length; --i >= 0; ) {
            Field field = fields[i];
            if (javaClass != field.getDeclaringClass()) continue;
            if (ConstantField.isConstant(field)) {
                constantFields.add(new ConstantField(field));
            }
        }
        this.staticAssignedNames = staticNames;
        this.constantFields = constantFields;
    }

    private void initializeClass(Class javaClass) {
        Class superclass = javaClass.getSuperclass();
        Map staticNames;
        Map instanceNames;
        if (superclass == null) {
            staticNames = new HashMap();
            instanceNames = new HashMap();
        } else {
            JavaClass superJavaClass = get(getRuntime(),superclass);
            staticNames = new HashMap(superJavaClass.getStaticAssignedNames());
            instanceNames = new HashMap(superJavaClass.getInstanceAssignedNames());
        }
        staticNames.putAll(STATIC_RESERVED_NAMES);
        instanceNames.putAll(INSTANCE_RESERVED_NAMES);
        Map staticCallbacks = new HashMap();
        Map instanceCallbacks = new HashMap();
        List constantFields = new ArrayList(); 
        Field[] fields = javaClass.getFields();
        for (int i = fields.length; --i >= 0; ) {
            Field field = fields[i];
            if (javaClass != field.getDeclaringClass()) continue;

            if (ConstantField.isConstant(field)) {
                constantFields.add(new ConstantField(field));
                continue;
            }
            String name = field.getName();
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                AssignedName assignedName = (AssignedName)staticNames.get(name);
                if (assignedName != null && assignedName.type < AssignedName.FIELD)
                    continue;
                staticNames.put(name,new AssignedName(name,AssignedName.FIELD));
                staticCallbacks.put(name,new StaticFieldGetter(name,field));
                if (!Modifier.isFinal(modifiers)) {
                    String setName = name + '=';
                    staticCallbacks.put(setName,new StaticFieldSetter(setName,field));
                }
            } else {
                AssignedName assignedName = (AssignedName)instanceNames.get(name);
                if (assignedName != null && assignedName.type < AssignedName.FIELD)
                    continue;
                instanceNames.put(name,new AssignedName(name,AssignedName.FIELD));
                instanceCallbacks.put(name,new InstanceFieldGetter(name,field));
                if (!Modifier.isFinal(modifiers)) {
                    String setName = name + '=';
                    instanceCallbacks.put(setName,new InstanceFieldSetter(setName,field));
                }
            }
        }
        // TODO: protected methods.  this is going to require a rework 
        // of some of the mechanism.  
        Method[] methods = javaClass.getMethods();
        for (int i = methods.length; --i >= 0; ) {
            // we need to collect all methods, though we'll only
            // install the ones that are named in this class
            Method method = methods[i];
            String name = method.getName();
            if (Modifier.isStatic(method.getModifiers())) {
                AssignedName assignedName = (AssignedName)staticNames.get(name);
                if (assignedName == null) {
                    staticNames.put(name,new AssignedName(name,AssignedName.METHOD));
                } else {
                    if (assignedName.type < AssignedName.METHOD)
                        continue;
                    if (assignedName.type != AssignedName.METHOD) {
                        staticCallbacks.remove(name);
                        staticCallbacks.remove(name+'=');
                        staticNames.put(name,new AssignedName(name,AssignedName.METHOD));
                    }
                }
                StaticMethodInvoker invoker = (StaticMethodInvoker)staticCallbacks.get(name);
                if (invoker == null) {
                    invoker = new StaticMethodInvoker(name);
                    staticCallbacks.put(name,invoker);
                }
                invoker.addMethod(method,javaClass);
            } else {
                AssignedName assignedName = (AssignedName)instanceNames.get(name);
                if (assignedName == null) {
                    instanceNames.put(name,new AssignedName(name,AssignedName.METHOD));
                } else {
                    if (assignedName.type < AssignedName.METHOD)
                        continue;
                    if (assignedName.type != AssignedName.METHOD) {
                        instanceCallbacks.remove(name);
                        instanceCallbacks.remove(name+'=');
                        instanceNames.put(name,new AssignedName(name,AssignedName.METHOD));
                    }
                }
                InstanceMethodInvoker invoker = (InstanceMethodInvoker)instanceCallbacks.get(name);
                if (invoker == null) {
                    invoker = new InstanceMethodInvoker(name);
                    instanceCallbacks.put(name,invoker);
                }
                invoker.addMethod(method,javaClass);
            }
        }
        this.staticAssignedNames = staticNames;
        this.instanceAssignedNames = instanceNames;
        this.staticCallbacks = staticCallbacks;
        this.instanceCallbacks = instanceCallbacks;
        this.constantFields = constantFields;
    }
    
    public void setupProxy(final RubyClass proxy) {
        assert proxyLock.isHeldByCurrentThread();
        proxy.defineFastMethod("__jsend!", __jsend_method);
        final Class javaClass = javaClass();
        if (javaClass.isInterface()) {
            setupInterfaceProxy(proxy);
            return;
        }
        assert this.proxyClass == null;
        this.unfinishedProxyClass = proxy;
        if (javaClass.isArray() || javaClass.isPrimitive()) {
            // see note below re: 2-field kludge
            this.proxyClass = proxy;
            this.proxyModule = proxy;
            return;
        }

        for (Iterator iter = constantFields.iterator(); iter.hasNext(); ) {
            ((ConstantField)iter.next()).install(proxy);
        }
        for (Iterator iter = staticCallbacks.values().iterator(); iter.hasNext(); ) {
            NamedCallback callback = (NamedCallback)iter.next();
            if (callback.type == NamedCallback.STATIC_METHOD && callback.hasLocalMethod()) {
                assignAliases((MethodCallback)callback,staticAssignedNames);
            }
            callback.install(proxy);
        }
        for (Iterator iter = instanceCallbacks.values().iterator(); iter.hasNext(); ) {
            NamedCallback callback = (NamedCallback)iter.next();
            if (callback.type == NamedCallback.INSTANCE_METHOD && callback.hasLocalMethod()) {
                assignAliases((MethodCallback)callback,instanceAssignedNames);
            }
            callback.install(proxy);
        }
        // setup constants for public inner classes
        Class[] classes = javaClass.getClasses();
        for (int i = classes.length; --i >= 0; ) {
            if (javaClass == classes[i].getDeclaringClass()) {
                Class clazz = classes[i];
                String simpleName = getSimpleName(clazz);
                
                if (simpleName.length() == 0) continue;
                
                // Ignore bad constant named inner classes pending JRUBY-697
                if (IdUtil.isConstant(simpleName) && proxy.getConstantAt(simpleName) == null) {
                    proxy.setConstant(simpleName,
                        Java.get_proxy_class(JAVA_UTILITIES,get(getRuntime(),clazz)));
                }
            }
        }
        // FIXME: bit of a kludge here (non-interface classes assigned to both
        // class and module fields). simplifies proxy extender code, will go away
        // when JI is overhauled (and proxy extenders are deprecated).
        this.proxyClass = proxy;
        this.proxyModule = proxy;

        applyProxyExtenders();

        // TODO: we can probably release our references to the constantFields
        // array and static/instance callback hashes at this point. 
    }

    private static void assignAliases(MethodCallback callback, Map assignedNames) {
        String name = callback.name;
        addUnassignedAlias(getRubyCasedName(name),assignedNames,callback);
        // logic adapted from java.beans.Introspector
        if (!(name.length() > 3 || name.startsWith("is")))
            return;

        String javaPropertyName = getJavaPropertyName(name);
        if (javaPropertyName == null)
            return; // not a Java property name, done with this method

        for (Iterator iter = callback.methods.iterator(); iter.hasNext(); ) {
            Method method = (Method)iter.next();
            Class[] argTypes = method.getParameterTypes();
            Class resultType = method.getReturnType();
            int argCount = argTypes.length;
            if (argCount == 0) {
                if (name.startsWith("get")) {
                    addUnassignedAlias(getRubyCasedName(javaPropertyName),assignedNames,callback);
                    addUnassignedAlias(javaPropertyName,assignedNames,callback);
                } else if (resultType == boolean.class && name.startsWith("is")) {
                    String rubyName = getRubyCasedName(name.substring(2));
                    if (rubyName != null) {
                        addUnassignedAlias(rubyName,assignedNames,callback);
                        addUnassignedAlias(rubyName+'?',assignedNames,callback);
                    }
                    if (!javaPropertyName.equals(rubyName)) {
                        addUnassignedAlias(javaPropertyName,assignedNames,callback);
                        addUnassignedAlias(javaPropertyName+'?',assignedNames,callback);
                    }
                }
            } else if (argCount == 1) {
                // indexed get
                if (argTypes[0] == int.class && name.startsWith("get")) {
                    addUnassignedAlias(getRubyCasedName(name.substring(3)),assignedNames,callback);
                    addUnassignedAlias(javaPropertyName,assignedNames,callback);
                } else if (resultType == void.class && name.startsWith("set")) {
                    String rubyName = getRubyCasedName(name.substring(3));
                    if (rubyName != null) {
                        addUnassignedAlias(rubyName + '=',assignedNames,callback);
                    }
                    if (!javaPropertyName.equals(rubyName)) {
                        addUnassignedAlias(javaPropertyName + '=',assignedNames,callback);
                    }
                }
            }
        }
    }
    
    private static void addUnassignedAlias(String name, Map assignedNames,
            MethodCallback callback) {
        if (name != null) {
            AssignedName assignedName = (AssignedName)assignedNames.get(name);
            if (assignedName == null) {
                callback.addAlias(name);
                assignedNames.put(name,new AssignedName(name,AssignedName.ALIAS));
            } else if (assignedName.type == AssignedName.ALIAS) {
                callback.addAlias(name);
            } else if (assignedName.type > AssignedName.ALIAS) {
                // TODO: there will be some additional logic in this branch
                // dealing with conflicting protected fields. 
                callback.addAlias(name);
                assignedNames.put(name,new AssignedName(name,AssignedName.ALIAS));
            }
        }
    }

    private static final Pattern JAVA_PROPERTY_CHOPPER = Pattern.compile("(get|set|is)([A-Z0-9])(.*)");
    public static String getJavaPropertyName(String beanMethodName) {
        Matcher m = JAVA_PROPERTY_CHOPPER.matcher(beanMethodName);

        if (!m.find()) return null;
        String javaPropertyName = m.group(2).toLowerCase() + m.group(3);
        return javaPropertyName;
    }

    private static final Pattern CAMEL_CASE_SPLITTER = Pattern.compile("([a-z][0-9]*)([A-Z])");    
    public static String getRubyCasedName(String javaCasedName) {
        Matcher m = CAMEL_CASE_SPLITTER.matcher(javaCasedName);
        String rubyCasedName = m.replaceAll("$1_$2").toLowerCase();
        if (rubyCasedName.equals(javaCasedName)) {
            return null;
        }
        return rubyCasedName;
    }
    
    
    // old (quasi-deprecated) interface class
    private void setupInterfaceProxy(final RubyClass proxy) {
        assert javaClass().isInterface();
        assert proxyLock.isHeldByCurrentThread();
        assert this.proxyClass == null;
        this.proxyClass = proxy;
        // nothing else to here - the module version will be
        // included in the class.
    }
    
    public void setupInterfaceModule(final RubyModule module) {
        assert javaClass().isInterface();
        assert proxyLock.isHeldByCurrentThread();
        assert this.proxyModule == null;
        this.unfinishedProxyModule = module;
        final Class javaClass = javaClass();
        for (Iterator iter = constantFields.iterator(); iter.hasNext(); ){
            ((ConstantField)iter.next()).install(module);
        }
        // setup constants for public inner classes
        final Class[] classes = javaClass.getClasses();
        for (int i = classes.length; --i >= 0; ) {
            if (javaClass == classes[i].getDeclaringClass()) {
                Class clazz = classes[i];
                String simpleName = getSimpleName(clazz);
                if (simpleName.length() == 0) continue;
                
                // Ignore bad constant named inner classes pending JRUBY-697
                if (IdUtil.isConstant(simpleName) && module.getConstantAt(simpleName) == null) {
                    module.const_set(getRuntime().newString(simpleName),
                        Java.get_proxy_class(JAVA_UTILITIES,get(getRuntime(),clazz)));
                }
            }
        }
        
        this.proxyModule = module;
        applyProxyExtenders();
    }

    public synchronized void addProxyExtender(final IRubyObject extender) {
        lockProxy();
        try {
            if (!extender.respondsTo("extend_proxy")) {
                throw getRuntime().newTypeError("proxy extender must have an extend_proxy method");
            }
            if (proxyModule == null) {
                if (proxyExtenders == null) {
                    proxyExtenders = new ArrayList<IRubyObject>();
                }
                proxyExtenders.add(extender);
            } else {
                getRuntime().getWarnings().warn(" proxy extender added after proxy class created for " + this);
                extendProxy(extender);
            }
        } finally {
            unlockProxy();
        }
    }
    
    private void applyProxyExtenders() {
        ArrayList<IRubyObject> extenders;
        if ((extenders = proxyExtenders) != null) {
            for (IRubyObject extender : extenders) {
                extendProxy(extender);
            }
            proxyExtenders = null;
        }
    }

    private void extendProxy(final IRubyObject extender) {
        extender.callMethod(getRuntime().getCurrentContext(), "extend_proxy", proxyModule);
    }
    
    public IRubyObject extend_proxy(final IRubyObject extender) {
        addProxyExtender(extender);
        return getRuntime().getNil();
    }
    
    public static JavaClass get(final Ruby runtime, final Class klass) {
        JavaClass javaClass = runtime.getJavaSupport().getJavaClassFromCache(klass);
        if (javaClass == null) {
            javaClass = createJavaClass(runtime,klass);
        }
        return javaClass;
    }

    private static synchronized JavaClass createJavaClass(final Ruby runtime, final Class klass) {
        // double-check the cache now that we're synchronized
        JavaClass javaClass = runtime.getJavaSupport().getJavaClassFromCache(klass);
        if (javaClass == null) {
            javaClass = new JavaClass(runtime, klass);
            runtime.getJavaSupport().putJavaClassIntoCache(javaClass);
        }
        return javaClass;
    }

    public static RubyClass createJavaClassClass(Ruby runtime, RubyModule javaModule) {
        // FIXME: Determine if a real allocator is needed here. Do people want to extend
        // JavaClass? Do we want them to do that? Can you Class.new(JavaClass)? Should
        // you be able to?
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result = javaModule.defineClassUnder("JavaClass", javaModule.getClass("JavaObject"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR); 

    	CallbackFactory callbackFactory = runtime.callbackFactory(JavaClass.class);
        
        result.includeModule(runtime.getModule("Comparable"));
        
        JavaObject.registerRubyMethods(runtime, result);

        result.getMetaClass().defineFastMethod("for_name", 
                callbackFactory.getFastSingletonMethod("for_name", IRubyObject.class));
        result.defineFastMethod("public?", 
                callbackFactory.getFastMethod("public_p"));
        result.defineFastMethod("protected?", 
                callbackFactory.getFastMethod("protected_p"));
        result.defineFastMethod("private?", 
                callbackFactory.getFastMethod("private_p"));
        result.defineFastMethod("final?", 
                callbackFactory.getFastMethod("final_p"));
        result.defineFastMethod("interface?", 
                callbackFactory.getFastMethod("interface_p"));
        result.defineFastMethod("array?", 
                callbackFactory.getFastMethod("array_p"));
        result.defineFastMethod("name", 
                callbackFactory.getFastMethod("name"));
        result.defineFastMethod("simple_name",
                callbackFactory.getFastMethod("simple_name"));
        result.defineFastMethod("to_s", 
                callbackFactory.getFastMethod("name"));
        result.defineFastMethod("superclass", 
                callbackFactory.getFastMethod("superclass"));
        result.defineFastMethod("<=>", 
                callbackFactory.getFastMethod("op_cmp", IRubyObject.class));
        result.defineFastMethod("java_instance_methods", 
                callbackFactory.getFastMethod("java_instance_methods"));
        result.defineFastMethod("java_class_methods", 
                callbackFactory.getFastMethod("java_class_methods"));
        result.defineFastMethod("java_method", 
                callbackFactory.getFastOptMethod("java_method"));
        result.defineFastMethod("constructors", 
                callbackFactory.getFastMethod("constructors"));
        result.defineFastMethod("constructor", 
                callbackFactory.getFastOptMethod("constructor"));
        result.defineFastMethod("array_class", 
                callbackFactory.getFastMethod("array_class"));
        result.defineFastMethod("new_array", 
                callbackFactory.getFastMethod("new_array", IRubyObject.class));
        result.defineFastMethod("fields", 
                callbackFactory.getFastMethod("fields"));
        result.defineFastMethod("field", 
                callbackFactory.getFastMethod("field", IRubyObject.class));
        result.defineFastMethod("interfaces", 
                callbackFactory.getFastMethod("interfaces"));
        result.defineFastMethod("primitive?", 
                callbackFactory.getFastMethod("primitive_p"));
        result.defineFastMethod("assignable_from?", 
                callbackFactory.getFastMethod("assignable_from_p", IRubyObject.class));
        result.defineFastMethod("component_type", 
                callbackFactory.getFastMethod("component_type"));
        result.defineFastMethod("declared_instance_methods", 
                callbackFactory.getFastMethod("declared_instance_methods"));
        result.defineFastMethod("declared_class_methods", 
                callbackFactory.getFastMethod("declared_class_methods"));
        result.defineFastMethod("declared_fields", 
                callbackFactory.getFastMethod("declared_fields"));
        result.defineFastMethod("declared_field", 
                callbackFactory.getFastMethod("declared_field", IRubyObject.class));
        result.defineFastMethod("declared_constructors", 
                callbackFactory.getFastMethod("declared_constructors"));
        result.defineFastMethod("declared_constructor", 
                callbackFactory.getFastOptMethod("declared_constructor"));
        result.defineFastMethod("declared_classes", 
                callbackFactory.getFastMethod("declared_classes"));
        result.defineFastMethod("declared_method", 
                callbackFactory.getFastOptMethod("declared_method"));

        result.defineFastMethod("extend_proxy", 
                callbackFactory.getFastMethod("extend_proxy", IRubyObject.class));

        result.getMetaClass().undefineMethod("new");
        result.getMetaClass().undefineMethod("allocate");

        return result;
    }
    
    public static synchronized JavaClass forName(Ruby runtime, String className) {
        Class klass = runtime.getJavaSupport().loadJavaClass(className);
        return JavaClass.get(runtime, klass);
    }

    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        return forName(recv.getRuntime(), name.asSymbol());
    }
    
    private static final Callback __jsend_method = new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                String name = args[0].asSymbol();
                
                DynamicMethod method = self.getMetaClass().searchMethod(name);
                int v = method.getArity().getValue();
                
                IRubyObject[] newArgs = new IRubyObject[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                if(v < 0 || v == (newArgs.length)) {
                    return self.callMethod(self.getRuntime().getCurrentContext(), name, newArgs, CallType.FUNCTIONAL, block);
                } else {
                    return self.callMethod(self.getRuntime().getCurrentContext(),self.getMetaClass().getSuperClass(), name, newArgs, CallType.SUPER, block);
                }
            }

            public Arity getArity() {
                return Arity.optional();
            }
        };

    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(javaClass().getModifiers()));
    }

    public RubyBoolean protected_p() {
        return getRuntime().newBoolean(Modifier.isProtected(javaClass().getModifiers()));
    }

    public RubyBoolean private_p() {
        return getRuntime().newBoolean(Modifier.isPrivate(javaClass().getModifiers()));
    }

	public Class javaClass() {
		return (Class) getValue();
	}

    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(javaClass().getModifiers()));
    }

    public RubyBoolean interface_p() {
        return getRuntime().newBoolean(javaClass().isInterface());
    }

    public RubyBoolean array_p() {
        return getRuntime().newBoolean(javaClass().isArray());
    }
    
    public RubyString name() {
        return getRuntime().newString(javaClass().getName());
    }
    

    private static String getSimpleName(Class class_) {
 		if (class_.isArray()) {
 			return getSimpleName(class_.getComponentType()) + "[]";
 		}
 
 		String className = class_.getName();
 
        int i = className.lastIndexOf('$');
 		if (i != -1) {
            do {
 				i++;
 			} while (i < className.length() && Character.isDigit(className.charAt(i)));
 			return className.substring(i);
 		}
 
 		return className.substring(className.lastIndexOf('.') + 1);
 	}

    public RubyString simple_name() {
        return getRuntime().newString(getSimpleName(javaClass()));
    }

    public IRubyObject superclass() {
        Class superclass = javaClass().getSuperclass();
        if (superclass == null) {
            return getRuntime().getNil();
        }
        return JavaClass.get(getRuntime(), superclass);
    }

    public RubyFixnum op_cmp(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw getRuntime().newTypeError("<=> requires JavaClass (" + other.getType() + " given)");
        }
        JavaClass otherClass = (JavaClass) other;
        if (this.javaClass() == otherClass.javaClass()) {
            return getRuntime().newFixnum(0);
        }
        if (otherClass.javaClass().isAssignableFrom(this.javaClass())) {
            return getRuntime().newFixnum(-1);
        }
        return getRuntime().newFixnum(1);
    }

    public RubyArray java_instance_methods() {
        return java_methods(javaClass().getMethods(), false);
    }

    public RubyArray declared_instance_methods() {
        return java_methods(javaClass().getDeclaredMethods(), false);
    }

	private RubyArray java_methods(Method[] methods, boolean isStatic) {
        RubyArray result = getRuntime().newArray(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (isStatic == Modifier.isStatic(method.getModifiers())) {
                result.append(JavaMethod.create(getRuntime(), method));
            }
        }
        return result;
	}

	public RubyArray java_class_methods() {
	    return java_methods(javaClass().getMethods(), true);
    }

	public RubyArray declared_class_methods() {
	    return java_methods(javaClass().getDeclaredMethods(), true);
    }

	public JavaMethod java_method(IRubyObject[] args) throws ClassNotFoundException {
        String methodName = args[0].asSymbol();
        Class[] argumentTypes = buildArgumentTypes(args);
        return JavaMethod.create(getRuntime(), javaClass(), methodName, argumentTypes);
    }

    public JavaMethod declared_method(IRubyObject[] args) throws ClassNotFoundException {
        String methodName = args[0].asSymbol();
        Class[] argumentTypes = buildArgumentTypes(args);
        return JavaMethod.createDeclared(getRuntime(), javaClass(), methodName, argumentTypes);
    }

    private Class[] buildArgumentTypes(IRubyObject[] args) throws ClassNotFoundException {
        if (args.length < 1) {
            throw getRuntime().newArgumentError(args.length, 1);
        }
        Class[] argumentTypes = new Class[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            JavaClass type = for_name(this, args[i]);
            argumentTypes[i - 1] = type.javaClass();
        }
        return argumentTypes;
    }

    public RubyArray constructors() {
        if (constructors == null) {
            constructors = buildConstructors(javaClass().getConstructors());
        }
        return constructors;
    }
    
    public RubyArray declared_classes() {
        Ruby runtime = getRuntime();
        RubyArray result = runtime.newArray();
        Class javaClass = javaClass();
        try {
            Class[] classes = javaClass.getDeclaredClasses();
            for (int i = 0; i < classes.length; i++) {
                if (Modifier.isPublic(classes[i].getModifiers())) {
                    result.append(get(runtime, classes[i]));
                }
            }
        } catch (SecurityException e) {
            // restrictive security policy; no matter, we only want public
            // classes anyway
            try {
                Class[] classes = javaClass.getClasses();
                for (int i = 0; i < classes.length; i++) {
                    if (javaClass == classes[i].getDeclaringClass()) {
                        result.append(get(runtime, classes[i]));
                    }
                }
            } catch (SecurityException e2) {
                // very restrictive policy (disallows Member.PUBLIC)
                // we'd never actually get this far in that case
            }
        }
        return result;
    }

    public RubyArray declared_constructors() {
        return buildConstructors(javaClass().getDeclaredConstructors());
    }

    private RubyArray buildConstructors(Constructor[] constructors) {
        RubyArray result = getRuntime().newArray(constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            result.append(new JavaConstructor(getRuntime(), constructors[i]));
        }
        return result;
    }

    public JavaConstructor constructor(IRubyObject[] args) {
        try {
            Class[] parameterTypes = buildClassArgs(args);
            Constructor constructor;
            constructor = javaClass().getConstructor(parameterTypes);
            return new JavaConstructor(getRuntime(), constructor);
        } catch (NoSuchMethodException nsme) {
            throw getRuntime().newNameError("no matching java constructor", null);
        }
    }

    public JavaConstructor declared_constructor(IRubyObject[] args) {
        try {
            Class[] parameterTypes = buildClassArgs(args);
            Constructor constructor;
            constructor = javaClass().getDeclaredConstructor (parameterTypes);
            return new JavaConstructor(getRuntime(), constructor);
        } catch (NoSuchMethodException nsme) {
            throw getRuntime().newNameError("no matching java constructor", null);
        }
    }

    private Class[] buildClassArgs(IRubyObject[] args) {
        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            String name = args[i].asSymbol();
            parameterTypes[i] = getRuntime().getJavaSupport().loadJavaClass(name);
        }
        return parameterTypes;
    }

    public JavaClass array_class() {
        return JavaClass.get(getRuntime(), Array.newInstance(javaClass(), 0).getClass());
    }
   
    public JavaObject new_array(IRubyObject lengthArgument) {
        if (lengthArgument instanceof RubyInteger) {
            // one-dimensional array
        int length = (int) ((RubyInteger) lengthArgument).getLongValue();
        return new JavaArray(getRuntime(), Array.newInstance(javaClass(), length));
        } else if (lengthArgument instanceof RubyArray) {
            // n-dimensional array
            List list = ((RubyArray)lengthArgument).getList();
            int length = list.size();
            if (length == 0) {
                throw getRuntime().newArgumentError("empty dimensions specifier for java array");
    }
            int[] dimensions = new int[length];
            for (int i = length; --i >= 0; ) {
                IRubyObject dimensionLength = (IRubyObject)list.get(i);
                if ( !(dimensionLength instanceof RubyInteger) ) {
                    throw getRuntime()
                        .newTypeError(dimensionLength, getRuntime().getClass("Integer"));
                }
                dimensions[i] = (int) ((RubyInteger) dimensionLength).getLongValue();
            }
            return new JavaArray(getRuntime(), Array.newInstance(javaClass(), dimensions));
        } else {
            throw getRuntime().newArgumentError(
                  "invalid length or dimensions specifier for java array" +
                  " - must be Integer or Array of Integer");
        }
    }

    public RubyArray fields() {
        return buildFieldResults(javaClass().getFields());
    }

    public RubyArray declared_fields() {
        return buildFieldResults(javaClass().getDeclaredFields());
    }
    
	private RubyArray buildFieldResults(Field[] fields) {
        RubyArray result = getRuntime().newArray(fields.length);
        for (int i = 0; i < fields.length; i++) {
            result.append(new JavaField(getRuntime(), fields[i]));
        }
        return result;
	}

	public JavaField field(IRubyObject name) {
		String stringName = name.asSymbol();
        try {
            Field field = javaClass().getField(stringName);
			return new JavaField(getRuntime(),field);
        } catch (NoSuchFieldException nsfe) {
            throw undefinedFieldError(stringName);
        }
    }

	public JavaField declared_field(IRubyObject name) {
		String stringName = name.asSymbol();
        try {
            Field field = javaClass().getDeclaredField(stringName);
			return new JavaField(getRuntime(),field);
        } catch (NoSuchFieldException nsfe) {
            throw undefinedFieldError(stringName);
        }
    }

    private RaiseException undefinedFieldError(String name) {
        return getRuntime().newNameError("undefined field '" + name + "' for class '" + javaClass().getName() + "'", name);
    }

    public RubyArray interfaces() {
        Class[] interfaces = javaClass().getInterfaces();
        RubyArray result = getRuntime().newArray(interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            result.append(JavaClass.get(getRuntime(), interfaces[i]));
        }
        return result;
    }

    public RubyBoolean primitive_p() {
        return getRuntime().newBoolean(isPrimitive());
    }

    public RubyBoolean assignable_from_p(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw getRuntime().newTypeError("assignable_from requires JavaClass (" + other.getType() + " given)");
        }

        Class otherClass = ((JavaClass) other).javaClass();
        return assignable(javaClass(), otherClass) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    static boolean assignable(Class thisClass, Class otherClass) {
        if(!thisClass.isPrimitive() && otherClass == Void.TYPE ||
            thisClass.isAssignableFrom(otherClass)) {
            return true;
        }

        otherClass = JavaUtil.primitiveToWrapper(otherClass);
        thisClass = JavaUtil.primitiveToWrapper(thisClass);

        if(thisClass.isAssignableFrom(otherClass)) {
            return true;
        }
        if(Number.class.isAssignableFrom(thisClass)) {
            if(Number.class.isAssignableFrom(otherClass)) {
                return true;
            }
            if(otherClass.equals(Character.class)) {
                return true;
            }
        }
        if(thisClass.equals(Character.class)) {
            if(Number.class.isAssignableFrom(otherClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPrimitive() {
        return javaClass().isPrimitive();
    }

    public JavaClass component_type() {
        if (! javaClass().isArray()) {
            throw getRuntime().newTypeError("not a java array-class");
        }
        return JavaClass.get(getRuntime(), javaClass().getComponentType());
    }
    
}
