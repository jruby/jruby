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
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.SimpleReflectedMethod;
import org.jruby.internal.runtime.methods.FullFunctionReflectedMethod;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * <p>
 * The main meta class for all other meta classes.
 * </p>
 */
public abstract class AbstractMetaClass extends RubyClass {
	protected abstract class Meta {
        protected abstract void initializeClass();
        
		/**
		 * Base implementation uses the data-driven approach not used currently but
		 * possibly revisited in the future.
		 */
//		protected void initializeClass() {
//			includeModules(getIncludedModules());
//
//			defineMethods(Visibility.PUBLIC, getSingletonMethods(), true);
//			defineMethods(Visibility.PUBLIC, getPublicMethods(), false);
//			defineMethods(Visibility.PRIVATE, getPrivateMethods(), false);
//
//			defineAliases(getAliases());
//
//			undefineMethods(getUndefineMethods(), false);
//			undefineMethods(getUndefineSingletonMethods(), true);
//
//			defineConstants(getDefineConstants(), false);
//			setConstants(getSetConstants(), false);
//		}

		// Empty impls
		protected Object[][] getPublicMethods() {
			return new Object[][] {};
		}

		protected Object[][] getDefineConstants() {
			return new Object[][] {};
		}

		protected Object[][] getSetConstants() {
			return new Object[][] {};
		}

		protected Object[][] getSingletonMethods() {
			return new Object[][] {};
		}

		protected String[][] getAliases() {
			return new String[][] {};
		}

		protected Object[][] getPrivateMethods() {
			return new Object[][] {};
		}

		protected String[] getIncludedModules() {
			return new String[] {};
		}

		protected String[] getUndefineMethods() {
			return new String[] {};
		}

		protected String[] getUndefineSingletonMethods() {
			return new String[] {};
		}

//		public void defineMethods(Visibility visibility, Object[][] methods,
//				boolean singleton) {
//			for (int i = 0; i < methods.length; i++) {
//				String name = (String) methods[i][0];
//				Arity arity = (Arity) methods[i][1];
//				String java_name = null;
//				switch (methods[i].length) {
//				case 2:
//					java_name = (String) methods[i][0];
//					break;
//				case 3:
//					java_name = (String) methods[i][2];
//					break;
//				}
//
//				assert name != null;
//				assert arity != null;
//				assert java_name != null;
//
//				visibility = name.equals("initialize") ? Visibility.PRIVATE
//						: Visibility.PUBLIC;
//
//				if (singleton) {
//					getSingletonClass().addMethod(
//							name,
//							new ReflectedMethod(AbstractMetaClass.this, AbstractMetaClass.this
//									.getClass(), java_name, arity, visibility));
//				} else {
//					addMethod(name, new ReflectedMethod(AbstractMetaClass.this,
//							builtinClass, java_name, arity, visibility));
//				}
//			}
//		}

		public void undefineMethods(String[] undefineMethods, boolean singleton) {
			for (int i = 0; i < undefineMethods.length; i++) {
				if (singleton) {
					getSingletonClass().undefineMethod(undefineMethods[i]);
				} else {
					undefineMethod(undefineMethods[i]);
				}
			}
		}

		public void defineConstants(Object[][] constants, boolean singleton) {
			for (int i = 0; i < constants.length; i++) {
				if (singleton) {
					getSingletonClass().defineConstant(
							(String) constants[i][0],
							(IRubyObject) constants[i][1]);
				} else {
					defineConstant((String) constants[i][0],
							(IRubyObject) constants[i][1]);
				}
			}
		}

		public void setConstants(Object[][] constants, boolean singleton) {
			for (int i = 0; i < constants.length; i++) {
				if (singleton) {
					getSingletonClass().setConstant((String) constants[i][0],
							(IRubyObject) constants[i][1]);
				} else {
					setConstant((String) constants[i][0],
							(IRubyObject) constants[i][1]);
				}
			}
		}

		public void includeModules(String[] includedModules) {
			for (int i = 0; i < includedModules.length; i++) {
				includeModule(getRuntime().getModule(includedModules[i]));
			}
		}

		public void defineAliases(Object[][] aliases) {
			for (int i = 0; i < aliases.length; i++) {
				defineAlias((String) aliases[i][0], (String) aliases[i][1]);
			}
		}
	};

	protected Meta getMeta() {
		return null;
	}

	protected Class builtinClass;
    protected CallbackFactory fact;
    protected CallbackFactory sfact;

	// Only for other core modules/classes
	protected AbstractMetaClass(IRuby runtime, RubyClass metaClass,
			RubyClass superClass, SinglyLinkedList parentCRef, String name,
			Class builtinClass) {
		super(runtime, metaClass, superClass, parentCRef, name);

		this.builtinClass = builtinClass;
	}

	protected AbstractMetaClass(String name, Class builtinClass, RubyClass superClass) {
		this(name, builtinClass, superClass, superClass.getRuntime().getClass(
				"Object").getCRef(), true);
	}

	protected AbstractMetaClass(String name, Class builtinClass, RubyClass superClass,
            SinglyLinkedList parentCRef) {
		this(name, builtinClass, superClass, parentCRef, false);
	}

	protected AbstractMetaClass(String name, Class builtinClass, RubyClass superClass,
            SinglyLinkedList parentCRef, boolean init) {
		super(superClass.getRuntime(), superClass.getRuntime()
				.getClass("Class"), superClass, parentCRef, name);

		assert name != null;
		assert builtinClass != null;
		//assert RubyObject.class.isAssignableFrom(builtinClass) || RubyObject.class == builtinClass: "builtinClass have to be a subclass of RubyObject.";
		assert superClass != null;

		this.builtinClass = builtinClass;

		makeMetaClass(superClass.getMetaClass(), superClass.getRuntime()
				.getCurrentContext().peekCRef());
		inheritedBy(superClass);

                if(name != null) {
                    ((RubyModule)parentCRef.getValue()).setConstant(name, this);
                }

		if (init) {
			getMeta().initializeClass();
		}
	}

	public AbstractMetaClass(IRuby runtime, RubyClass metaClass, RubyClass superClass,
            SinglyLinkedList parentCRef, String name) {
		super(runtime, metaClass, superClass, parentCRef, name);
	}

	public void defineMethod(String name, Arity arity) {
		defineMethod(name, arity, name);
	}

	public void defineMethod(String name, Arity arity, String java_name) {
		assert name != null;
		assert arity != null;
		assert java_name != null;

		Visibility visibility = name.equals("initialize") ? Visibility.PRIVATE
				: Visibility.PUBLIC;

		addMethod(name, new FullFunctionReflectedMethod(this, builtinClass, java_name,
				arity, visibility));
	}

	public void definePrivateMethod(String name, Arity arity) {
		addMethod(name, new FullFunctionReflectedMethod(this, builtinClass, name, arity,
				Visibility.PRIVATE));
	}

	public void definePrivateMethod(String name, Arity arity, String java_name) {
		addMethod(name, new FullFunctionReflectedMethod(this, builtinClass, java_name,
				arity, Visibility.PRIVATE));
	}

	public void defineFastMethod(String name, Arity arity) {
		defineFastMethod(name, arity, name);
	}

	public void defineFastMethod(String name, Arity arity, String java_name) {
		assert name != null;
		assert arity != null;
		assert java_name != null;

		Visibility visibility = name.equals("initialize") ? Visibility.PRIVATE
				: Visibility.PUBLIC;

		addMethod(name, new SimpleReflectedMethod(this, builtinClass, java_name,
				arity, visibility));
	}

	public void defineFastPrivateMethod(String name, Arity arity) {
		addMethod(name, new SimpleReflectedMethod(this, builtinClass, name, arity,
				Visibility.PRIVATE));
	}

	public void defineFastPrivateMethod(String name, Arity arity, String java_name) {
		addMethod(name, new SimpleReflectedMethod(this, builtinClass, java_name,
				arity, Visibility.PRIVATE));
	}

	public void defineSingletonMethod(String name, Arity arity) {
		defineSingletonMethod(name, arity, name);
	}

	public void defineSingletonMethod(String name, Arity arity, String java_name) {
		assert name != null;
		assert arity != null;
		assert java_name != null;

		Visibility visibility = name.equals("initialize") ? Visibility.PRIVATE
				: Visibility.PUBLIC;

		getSingletonClass().addMethod(
				name,
				new FullFunctionReflectedMethod(this, getClass(), java_name, arity,
						visibility));
	}

	public void defineFastSingletonMethod(String name, Arity arity) {
		defineSingletonMethod(name, arity, name);
	}

	public void defineFastSingletonMethod(String name, Arity arity, String java_name) {
		assert name != null;
		assert arity != null;
		assert java_name != null;

		Visibility visibility = name.equals("initialize") ? Visibility.PRIVATE
				: Visibility.PUBLIC;

		getSingletonClass().addMethod(
				name,
				new SimpleReflectedMethod(this, getClass(), java_name, arity,
						visibility));
	}

	/**
	 * Only intended to be called by ModuleMetaClass and ClassMetaClass. Seems
	 * like a waste to subclass over this and there seems little risk of
	 * programmer error. We cannot define methods for there two classes since
	 * there is a circular dependency between them and ObjectMetaClass. We defer
	 * initialization until after construction and meta classes are made.
	 */
	public void initializeBootstrapClass() {
		getMeta().initializeClass();
	}
}
