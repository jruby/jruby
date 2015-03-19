/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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

package org.jruby.javasupport;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.javasupport.binding.AssignedName;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.ClassValue;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.Set;

public abstract class JavaSupport {
    public abstract Class loadJavaClass(String className) throws ClassNotFoundException;

    public abstract Class loadJavaClassVerbose(String className);

    public abstract Class loadJavaClassQuiet(String className);

    public abstract JavaClass getJavaClassFromCache(Class clazz);

    public abstract RubyModule getProxyClassFromCache(Class clazz);

    public abstract void handleNativeException(Throwable exception, Member target);

    public abstract ObjectProxyCache<IRubyObject,RubyClass> getObjectProxyCache();

    public abstract Map<String, JavaClass> getNameClassMap();

    public abstract void setJavaObjectVariable(Object o, int i, Object v);

    public abstract Object getJavaObjectVariable(Object o, int i);

    public abstract RubyModule getJavaModule();

    public abstract RubyModule getJavaUtilitiesModule();

    public abstract RubyModule getJavaArrayUtilitiesModule();

    public abstract RubyClass getJavaObjectClass();

    public abstract JavaClass getObjectJavaClass();

    public abstract void setObjectJavaClass(JavaClass objectJavaClass);

    public abstract RubyClass getJavaArrayClass();

    public abstract RubyClass getJavaClassClass();

    public abstract RubyModule getJavaInterfaceTemplate();

    public abstract RubyModule getPackageModuleTemplate();

    public abstract RubyClass getJavaProxyClass();

    public abstract RubyClass getArrayJavaProxyCreatorClass();

    public abstract RubyClass getConcreteProxyClass();

    public abstract RubyClass getMapJavaProxyClass();

    public abstract RubyClass getArrayProxyClass();

    public abstract RubyClass getJavaFieldClass();

    public abstract RubyClass getJavaMethodClass();

    public abstract RubyClass getJavaConstructorClass();

    public abstract Map<Set<?>, JavaProxyClass> getJavaProxyClassCache();

    public abstract ClassValue<ThreadLocal<RubyModule>> getUnfinishedProxyClassCache();

    public abstract ClassValue<Map<String, AssignedName>> getStaticAssignedNames();

    public abstract ClassValue<Map<String, AssignedName>> getInstanceAssignedNames();
}
