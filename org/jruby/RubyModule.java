/*
 * RubyModule.java - No description
 * Created on 09. Juli 2001, 21:38
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jruby;

import java.util.*;

import org.jruby.core.*;
import org.jruby.exceptions.*;
import org.jruby.nodes.*;
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {
	private RubyModule superClass;
	private RubyMap methods = new RubyHashMap();

	// Flags
	private boolean singleton = false;

	/** Holds value of property included. */
	private boolean included;

	public RubyModule(Ruby ruby) {
		this(ruby, null);
	}

	public RubyModule(Ruby ruby, RubyModule rubyClass) {
		this(ruby, rubyClass, null);
	}

	public RubyModule(Ruby ruby, RubyModule rubyClass, RubyModule superClass) {
		super(ruby, rubyClass);

		this.superClass = superClass;
	}

	/** Getter for property superClass.
	 * @return Value of property superClass.
	 */
	public RubyModule getSuperClass() {
		return this.superClass;
	}

	/** Setter for property superClass.
	 * @param superClass New value of property superClass.
	 */
	public void setSuperClass(RubyModule superClass) {
		this.superClass = superClass;
	}

	public boolean isSingleton() {
		return this.singleton;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public RubyMap getMethods() {
		return this.methods;
	}

	public void setMethods(RubyMap methods) {
		this.methods = methods;
	}

	public boolean isModule() {
		return !isIncluded();
	}

	public boolean isClass() {
		return false;
	}

	/** classname
	 *
	 */
	public RubyString getClassname() {
		RubyString path = null;

		RubyModule rbModule = this;
		while (rbModule.isIncluded() || rbModule.isSingleton()) {
			rbModule = ((RubyClass) rbModule).getSuperClass();
		}

		if (rbModule == null) {
			rbModule = getRuby().getClasses().getObjectClass();
		}

		path =
			(RubyString) getInstanceVariables().get(getRuby().intern("__classpath__"));
		if (path == null) {
			if (getInstanceVariables().get(getRuby().intern("__classid__")) != null) {
				path =
					RubyString.m_newString(
						getRuby(),
						((RubyId) getInstanceVariables().get(getRuby().intern("__classid__")))
							.toName());
				// todo: convert from symbol to string

				getInstanceVariables().put(getRuby().intern("__classpath__"), path);
				getInstanceVariables().remove(getRuby().intern("__classid__"));
			}
		}

		if (path == null) {
			RubyObject tmp = rbModule.findClassPath();

			if (tmp.isNil()) {
				return null;
			}

			return (RubyString) tmp;
		}

		/*if (!(path instanceof RubyString)) {
		    throw new RubyBugException("class path is not set properly");
		}*/

		return path;
	}

	private static class FindClassPathResult {
		public RubyId name;
		public RubyModule klass;
		public RubyString path;
		public RubyObject track;
		public FindClassPathResult prev;
	}

	private class FindClassPathMapMethod implements RubyMapMethod {
		public int execute(Object _key, Object _value, Object _res) {
			// Cast the values.
			RubyId key = null;
			try {
				key = (RubyId) _key;
			} catch (ClassCastException ccExcptn) {
				key = getRuby().intern((String) _key);
				System.out.println("key isn't ID: \"" + _key + "\"");
			}
			if (!(_value instanceof RubyObject)) {
				return RubyMapMethod.CONTINUE;
			}
			RubyObject value = (RubyObject) _value;
			FindClassPathResult res = (FindClassPathResult) _res;

			RubyString path = null;

			if (!key.isConstId()) {
				return RubyMapMethod.CONTINUE;
			}

			String name = key.toName();
			if (res.path != null) {
				path = (RubyString) res.path.m_dup();
				path.m_cat("::");
				path.m_cat(name);
			} else {
				path = RubyString.m_newString(getRuby(), name);
			}

			if (value == res.klass) {
				res.name = key;
				res.path = path;
				return RubyMapMethod.STOP;
			}

			if (value.m_kind_of(getRuby().getClasses().getModuleClass()).isTrue()) {
				if (value.getInstanceVariables() == null) {
					return RubyMapMethod.CONTINUE;
				}

				FindClassPathResult list = res;

				while (list != null) {
					if (list.track == value) {
						return RubyMapMethod.CONTINUE;
					}
					list = list.prev;
				}

				FindClassPathResult arg = new FindClassPathResult();
				arg.name = null;
				arg.path = path;
				arg.klass = res.klass;
				arg.track = value;
				arg.prev = res;

				value.getInstanceVariables().foreach(this, arg);

				if (arg.name != null) {
					res.name = arg.name;
					res.path = arg.path;
					return RubyMapMethod.STOP;
				}
			}
			return RubyMapMethod.CONTINUE;
		}
	}

	/** findclasspath
	 *
	 */
	public RubyObject findClassPath() {
		FindClassPathResult arg = new FindClassPathResult();
		arg.klass = this;
		arg.track = getRuby().getClasses().getObjectClass();
		arg.prev = null;

		if (getRuby().getClasses().getObjectClass().getInstanceVariables() != null) {
			getRuby().getClasses().getObjectClass().getInstanceVariables().foreach(
				new FindClassPathMapMethod(),
				arg);
		}

		if (arg.name == null) {
			getRuby().getClasses().getClassMap().foreach(new FindClassPathMapMethod(), arg);
		}

		if (arg.name != null) {
			getInstanceVariables().put(getRuby().intern("__classpath__"), arg.path);
			return arg.path;
		}
		return getRuby().getNil();
	}

	/** include_class_new
	 *
	 */
	public RubyClass newIncludeClass(RubyModule superClass) {
		RubyClass newClass = new RubyClass(getRuby(), superClass);
		newClass.setIncluded(true);

		newClass.setInstanceVariables(getInstanceVariables());
		newClass.setMethods(methods);

		if (isIncluded()) {
			newClass.setRubyClass(getRubyClass());
		} else {
			newClass.setRubyClass(this);
		}

		return newClass;
	}

	public void setName(RubyId id) {
		getInstanceVariables().put(getRuby().intern("__classid__"), id);
	}

	/** rb_set_class_path
	 *
	 */
	public void setClassPath(RubyModule under, String name) {
		RubyString value = null;

		if (under == getRuby().getClasses().getObjectClass()) {
			value = RubyString.m_newString(getRuby(), name);
		} else {
			value = (RubyString) under.getClassPath().m_dup();
			value.m_cat("::");
			value.m_cat(name);
		}

		getInstanceVariables().put(getRuby().intern("__classpath__"), value);
	}

	/** rb_class_path
	 *
	 */
	public RubyString getClassPath() {
		RubyString path = getClassname();

		if (path != null) {
			return path;
		}

		String s = "Module";
		if (isClass()) {
			s = "Class";
		}

		return RubyString.m_newString(
			getRuby(),
			"<" + s + " 01x" + Integer.toHexString(hashCode()) + ">");
		// 0 = pointer
	}

	/** rb_cvar_singleton
	 *
	 */
	public RubyModule getClassVarSingleton() {
		return this;
	}

	/** rb_cvar_set
	 *
	 */
	public void setClassVar(RubyId id, RubyObject value) {
		RubyModule tmp = this;
		while (tmp != null) {
			if (tmp.getInstanceVariables() != null
				&& tmp.getInstanceVariables().get(id) != null) {
				if (tmp.isTaint() && getRuby().getSecurityLevel() >= 4) {
					throw new RubySecurityException(
						getRuby(),
						"Insecure: can't modify class variable");
				}
				tmp.getInstanceVariables().put(id, value);
			}
			tmp = tmp.getSuperClass();
		}
		throw new RubyNameException(
			getRuby(),
			"uninitialized class variable " + id.toName() + " in " + toName());
	}

	/** rb_cvar_declare
	 *
	 */
	public void declareClassVar(RubyId id, RubyObject value) {
		RubyModule tmp = this;
		while (tmp != null) {
			if (tmp.getInstanceVariables() != null
				&& tmp.getInstanceVariables().get(id) != null) {
				if (tmp.isTaint() && getRuby().getSecurityLevel() >= 4) {
					throw new RubySecurityException(
						getRuby(),
						"Insecure: can't modify class variable");
				}
				tmp.getInstanceVariables().put(id, value);
			}
			tmp = tmp.getSuperClass();
		}
		setAv(id, value, false);
	}

	/** rb_cvar_get
	 *
	 */
	public RubyObject getClassVar(RubyId id) {
		RubyModule tmp = this;
		while (tmp != null) {
			if (tmp.getInstanceVariables() != null
				&& tmp.getInstanceVariables().get(id) != null) {
				return (RubyObject) tmp.getInstanceVariables().get(id);
			}
			tmp = tmp.getSuperClass();
		}
		throw new RubyNameException(
			getRuby(),
			"uninitialized class variable " + id.toName() + " in " + toName());
	}

	/** rb_cvar_defined
	 *
	 */
	public boolean isClassVarDefined(RubyId id) {
		RubyModule tmp = this;
		while (tmp != null) {
			if (tmp.getInstanceVariables() != null
				&& tmp.getInstanceVariables().get(id) != null) {
				return true;
			}
			tmp = tmp.getSuperClass();
		}
		return false;
	}

	/**
	 *
	 */
	public void setConstant(RubyId id, RubyObject value) {
		setAv(id, value, true);
	}

	/** rb_const_get
	 *
	 */
	public RubyObject getConstant(RubyId id) {
		boolean mod_retry = false;
		RubyModule tmp = this;

		while (true) {
			while (tmp != null) {
				if (tmp.getInstanceVariables().get(id) != null) {
					return (RubyObject) tmp.getInstanceVariables().get(id);
				}
				if (tmp == getRuby().getClasses().getObjectClass()
					&& getRuby().getTopConstant(id) != null) {
					return getRuby().getTopConstant(id);
				}
				tmp = tmp.getSuperClass();
			}
			if (!mod_retry && isModule()) {
				mod_retry = true;
				tmp = getRuby().getClasses().getObjectClass();
				continue;
			}
			break;
		}

		/* Uninitialized constant */
		if (this != getRuby().getClasses().getObjectClass()) {
			throw new RubyNameException(
				getRuby(),
				"uninitialized constant " + id.toName() + " at " + getClassPath().getValue());
		} else {
			throw new RubyNameException(getRuby(), "uninitialized constant " + id.toName());
		}

		// return getRuby().getNil();
	}

	/** rb_include_module
	 *
	 */
	public void includeModule(RubyModule rubyModule) {
		if (rubyModule == null || rubyModule == this) {
			return;
		}

		/* Fixed to Ruby 1.6.5 */
		for (RubyModule actClass = this;
			rubyModule != null;
			rubyModule = rubyModule.getSuperClass()) {
			for (RubyModule rbClass = actClass.getSuperClass();
				rbClass != null;
				rbClass = rbClass.getSuperClass()) {
				if (rbClass.isIncluded() && rbClass.methods == rubyModule.methods) {
					continue;
				}
			}
			actClass.setSuperClass(rubyModule.newIncludeClass(actClass.getSuperClass()));
			actClass = actClass.getSuperClass();
		}
	}

	/** mod_av_set
	 *
	 */
	protected void setAv(RubyId id, RubyObject value, boolean constant) {
		String dest = constant ? "constant" : "class variable";

		if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
			throw new RubySecurityException(getRuby(), "Insecure: can't set " + dest);
		}
		if (isFrozen()) {
			throw new RubyFrozenException(getRuby(), "class/module");
		}
		if (constant && (getInstanceVariables().get(id) != null)) {
			//getRuby().warn("already initialized " + dest + " " + name);
		}

		if (getInstanceVariables() == null) {
			setInstanceVariables(new RubyHashMap());
		}

		getInstanceVariables().put(id, value);
	}

	/** rb_add_method
	 *
	 */
	public void addMethod(RubyId id, Node node, int noex) {
		if (this == getRuby().getClasses().getObjectClass()) {
			getRuby().secure(4);
		}

		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new RubySecurityException(getRuby(), "Insecure: can't define method");
		}
		if (isFrozen()) {
			throw new RubyFrozenException(getRuby(), "class/module");
		}
		Node body = new NodeFactory(getRuby()).newMethod(node, noex);
		methods.put(id, body);
	}

	public void defineMethod(String name, RubyCallbackMethod method) {
		RubyId id = getRuby().intern(name);

		int noex =
			(name.charAt(0) == 'i' && id == getRuby().intern("initialize"))
				? Constants.NOEX_PRIVATE
				: Constants.NOEX_PUBLIC;

		addMethod(
			id,
			new NodeFactory(getRuby()).newCFunc(method),
			noex | Constants.NOEX_CFUNC);
	}

	public void defineMethodId(RubyId id, RubyCallbackMethod method) {
		addMethod(
			id,
			new NodeFactory(getRuby()).newCFunc(method),
			Constants.NOEX_PUBLIC | Constants.NOEX_CFUNC);
	}

	public void defineProtectedMethod(String name, RubyCallbackMethod method) {
		addMethod(
			getRuby().intern(name),
			new NodeFactory(getRuby()).newCFunc(method),
			Constants.NOEX_PROTECTED | Constants.NOEX_CFUNC);
	}

	public void definePrivateMethod(String name, RubyCallbackMethod method) {
		addMethod(
			getRuby().intern(name),
			new NodeFactory(getRuby()).newCFunc(method),
			Constants.NOEX_PRIVATE | Constants.NOEX_CFUNC);
	}

	/*public void undefMethod(RubyId id) {
	    if (isFrozen()) {
	        throw new RubyFrozenException();
	    }
	 
	    getMethods().remove(id);
	}*/

	public void undefMethod(String name) {
		addMethod(getRuby().intern(name), null, Constants.NOEX_UNDEF);
	}

	/** rb_frozen_class_p
	 *
	 */
	protected void testFrozen() {
		String desc = "something(?!)";
		if (isFrozen()) {
			if (isSingleton()) {
				desc = "object";
			} else {
				if (isIncluded() || isModule()) {
					desc = "module";
				} else
					if (isClass()) {
						desc = "class";
					}
			}
			throw new RubyFrozenException(getRuby(), desc);
		}
	}

	/** rb_undef
	 *
	 */
	public void undef(RubyId id) {
		if (this == getRuby().getClasses().getObjectClass()) {
			getRuby().secure(4);
		}
		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new SecurityException("Insecure: can't undef");
		}
		testFrozen();
		if (id == getRuby().intern("__id__") || id == getRuby().intern("__send__")) {
			/*rb_warn("undefining `%s' may cause serious problem",
			         rb_id2name( id ) );*/
		}
		MethodNode methodNode = searchMethod(id);
		if (methodNode == null || methodNode.getBodyNode() == null) {
			String s0 = " class";
			RubyModule c = this;

			if (c.isSingleton()) {
				RubyObject obj = getInstanceVar("__attached__");

				if (obj instanceof RubyModule) {
					c = (RubyModule) obj;
					s0 = "";
				}
			} else
				if (c.isModule()) {
					s0 = " module";
				}
			throw new RubyNameException(
				getRuby(),
				"undefined method " + id.toName() + " for" + s0 + " '" + c.toName() + "'");
		}
		addMethod(id, null, Constants.NOEX_PUBLIC);
	}

	/** rb_define_module_function
	 *
	 */
	public void defineModuleFunction(String name, RubyCallbackMethod method) {
		definePrivateMethod(name, method);
		defineSingletonMethod(name, method);
	}

	/** rb_define_alias
	 *
	 */
	public void defineAlias(String oldName, String newName) {

	}

	/** rb_define_attr
	 *
	 */
	public void defineAttribute(String name, boolean read, boolean write) {

	}

	/** rb_const_defined
	 *
	 */
	public boolean isConstantDefined(RubyId id) {
		for (RubyModule tmp = this; tmp != null; tmp = tmp.getSuperClass()) {
			if (tmp.getInstanceVariables() != null
				&& tmp.getInstanceVariables().get(id) != null) {
				return true;
			}
		}

		if (isModule()) {
			return getRuby().getClasses().getObjectClass().isConstantDefined(id);
		}

		if (getRuby().isClassDefined(id)) {
			return true;
		}

		return getRuby().isAutoloadDefined(id);
	}

	/** search_method
	 *
	 */
	public MethodNode searchMethod(RubyId id) {
		MethodNode body = (MethodNode) methods.get(id);
		if (body == null) {
			if (getSuperClass() != null) {
				return getSuperClass().searchMethod(id);
			} else {
				return null;
			}
		} else {
			body.setMethodOrigin(this);
			return body;
		}
	}

	/** rb_get_method_body
	 *
	 */
	public GetMethodBodyResult getMethodBody(RubyId id, int noex) {
		GetMethodBodyResult result = new GetMethodBodyResult(this, id, noex);

		MethodNode methodNode = searchMethod(id);

		if (methodNode == null || methodNode.getBodyNode() == null) {
			System.out.println(
				"Cant find method \"" + id.toName() + "\" in class " + toName());

			RubyMethodCacheEntry.saveEmptyEntry(getRuby(), this, id);

			return result;
		}

		RubyMethodCacheEntry ent = new RubyMethodCacheEntry(this, methodNode.getNoex());

		Node body = methodNode.getBodyNode();

		if (body instanceof FBodyNode) {
			FBodyNode fbody = (FBodyNode) body;

			ent.setMid(id);
			ent.setOrigin((RubyModule) fbody.getOrigin());
			ent.setMid0(fbody.getMId());
			ent.setMethod(fbody.getBodyNode());

			result.setRecvClass((RubyModule) fbody.getOrigin());
			result.setId(fbody.getMId());
			body = fbody.getBodyNode();
		} else {
			ent.setMid(id);
			ent.setMid0(id);
			ent.setOrigin(methodNode.getMethodOrigin());
			ent.setMethod(body);

			result.setRecvClass(methodNode.getMethodOrigin());
		}

		RubyMethodCacheEntry.saveEntry(getRuby(), this, id, ent);

		result.setNoex(ent.getNoex());
		result.setBody(body);
		return result;
	}

	/** rb_call
	 *
	 */
	public RubyObject call(
		RubyObject recv,
		RubyId mid,
		RubyPointer args,
		int scope) {
		RubyMethodCacheEntry ent = RubyMethodCacheEntry.getEntry(getRuby(), this, mid);

		RubyModule klass = this;
		RubyId id = mid;
		int noex;
		Node body;

		if (ent != null) {
			if (ent.getMethod() == null) {
				throw new RuntimeException("Undefined");
			}

			klass = ent.getOrigin();
			id = ent.getMid0();
			noex = ent.getNoex();
			body = ent.getMethod();
		} else {
			GetMethodBodyResult gmbr = getMethodBody(id, 0);
			klass = gmbr.getRecvClass();
			id = gmbr.getId();
			noex = gmbr.getNoex();
			body = gmbr.getBody();

			if (body == null) {
				if (scope == 3) {
					throw new RubyNameException(
						getRuby(),
						"super: no superclass method '" + mid.toName() + "'");
				}
				throw new RuntimeException("Undefined");
			}
		}

		// if (mid != missing) {
		//     /* receiver specified form for private method */
		//     if ((noex & NOEX_PRIVATE) && scope == 0)
		//         return rb_undefined(recv, mid, argc, argv, CSTAT_PRIV);

		//     /* self must be kind of a specified form for private method */
		//     if ((noex & NOEX_PROTECTED)) {
		//         VALUE defined_class = klass;
		//         while (TYPE(defined_class) == T_ICLASS)
		//             defined_class = RBASIC(defined_class)->klass;
		//         if (!rb_obj_is_kind_of(ruby_frame->self, defined_class))
		//             return rb_undefined(recv, mid, argc, argv, CSTAT_PROT);
		//     }
		// }

		// ...

		return klass.call0(recv, id, args, body, false);
	}

	/** rb_call0
	 *
	 */
	public RubyObject call0(
		RubyObject recv,
		RubyId id,
		RubyPointer args,
		Node body,
		boolean noSuper) {

		// ...

		if (getRuby().getIter().getIter() == RubyIter.ITER_PRE) {
			getRuby().getIter().push(RubyIter.ITER_CUR);
		} else {
			getRuby().getIter().push(RubyIter.ITER_NOT);
		}

		RubyFrame frame = getRuby().getRubyFrame();
		frame.push();
		frame.setLastFunc(id);
		frame.setLastClass(noSuper ? null : this);
		frame.setSelf(recv);
		frame.setArgs(args);

		RubyObject result =
			((CallableNode) body).call(getRuby(), recv, id, args, noSuper);

		getRuby().getRubyFrame().pop();
		getRuby().getIter().pop();

		return result;
	}

	/** rb_singleton_class_new
	 *
	 */
	public RubyClass newSingletonClass() {
		RubyClass newClass = RubyClass.m_newClass(getRuby(), (RubyClass) this);
		newClass.setSingleton(true);

		return newClass;
	}

	/** rb_alias
	 *
	 */
	public void aliasMethod(RubyId newId, RubyId oldId) {
		testFrozen();

		if (oldId == newId) {
			return;
		}

		if (this == getRuby().getClasses().getObjectClass()) {
			getRuby().secure(4);
		}

		MethodNode methodNode = searchMethod(oldId);
		RubyModule origin = methodNode.getMethodOrigin();

		if (methodNode == null || methodNode.getBodyNode() == null) {
			if (isModule()) {
				methodNode = getRuby().getClasses().getObjectClass().searchMethod(oldId);
				origin = methodNode.getMethodOrigin();
			}
		}
		if (methodNode == null || methodNode.getBodyNode() == null) {
			// print_undef( klass, def );
		}

		Node body = methodNode.getBodyNode();
		// methodNode.setCnt(methodNode.nd_cnt() + 1);
		if (body instanceof FBodyNode) { /* was alias */
			oldId = body.getMId();
			origin = (RubyModule) body.getOrigin();
			body = body.getBodyNode();
		}

		NodeFactory nf = new NodeFactory(getRuby());

		methods.put(
			newId,
			nf.newMethod(nf.newFBody(body, oldId, origin), methodNode.getNoex()));
	}

	/** rb_singleton_class_clone
	 *
	 */
	public RubyModule getSingletonClassClone() {
		if (!isSingleton()) {
			return this;
		}

		RubyModule clone = new RubyClass(getRuby(), getSuperClass());
		clone.setupClone(this);
		clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());

		//clone.setMethods();

		// st_foreach(RCLASS(klass)->m_tbl, clone_method, clone->m_tbl);

		clone.setSingleton(true);

		return clone;
	}

	/** rb_singleton_class_attached
	 *
	 */
	public void attachSingletonClass(RubyObject rbObject) {
		if (isSingleton()) {
			if (getInstanceVariables() == null) {
				setInstanceVariables(new RubyHashMap());
			}

			getInstanceVariables().put(getRuby().intern("__atached__"), rbObject);
		}
	}

	/** rb_define_class_under
	 *
	 */
	public RubyClass defineClassUnder(String name, RubyClass superClass) {
		RubyClass newClass =
			getRuby().defineClassId(getRuby().intern(name), superClass);

		setConstant(getRuby().intern(name), newClass);
		newClass.setClassPath(this, name);

		return newClass;
	}

	/** rb_define_module_under
	 *
	 */
	public RubyModule defineModuleUnder(String name) {
		RubyModule newModule = getRuby().defineModuleId(getRuby().intern(name));

		setConstant(getRuby().intern(name), newModule);
		newModule.setClassPath(this, name);

		return newModule;
	}

	/** rb_class2name
	 *
	 */
	public String toName() {
		if (this == getRuby().getClasses().getNilClass()) {
			return "nil";
		}
		if (this == getRuby().getClasses().getTrueClass()) {
			return "true";
		}
		if (this == getRuby().getClasses().getFalseClass()) {
			return "false";
		}

		return ((RubyString) getClassPath()).getValue();
	}

	/** rb_define_const
	 *
	 */
	public void defineConstant(String name, RubyObject value) {
		RubyId id = getRuby().intern(name);

		if (this == getRuby().getClasses().getClassClass()) {
			getRuby().secure(4);
		}

		if (!id.isConstId()) {
			throw new RubyNameException(getRuby(), "bad constant name " + name);
		}

		setConstant(id, value);
	}

	/** rb_mod_remove_cvar
	 *
	 */
	public RubyObject removeCvar(RubyObject name) { // Wrong Parameter ?
		RubyId id = getRuby().toId(name);

		if (!id.isClassId()) {
			throw new RubyNameException(getRuby(), "wrong class variable name " + name);
		}

		if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't remove class variable");
		}

		if (isFrozen()) {
			throw new RubyFrozenException(getRuby(), "class/module");
		}

		RubyObject value = (RubyObject) getInstanceVariables().remove(id);

		if (value != null) {
			return value;
		}

		if (isClassVarDefined(id)) {
			throw new RubyNameException(
				getRuby(),
				"cannot remove " + id.toName() + " for " + toName());
		}

		throw new RubyNameException(
			getRuby(),
			"class variable " + id.toName() + " not defined for " + toName());
	}

	/** rb_define_class_variable
	 *
	 */
	public void defineClassVariable(String name, RubyObject value) {
		RubyId id = getRuby().intern(name);

		if (!id.isClassId()) {
			throw new RubyNameException(getRuby(), "wrong class variable name " + name);
		}

		declareClassVar(id, value);
	}

	/** rb_attr
	 *
	 */
	public void addAttribute(RubyId id, boolean read, boolean write, boolean ex) {
		// RubyInterpreter intrprtr = getRuby().getInterpreter();

		int noex = Constants.NOEX_PUBLIC;

		if (ex) {
			if (getRuby().getActMethodScope() == Constants.SCOPE_PRIVATE) {
				noex = Constants.NOEX_PRIVATE;
			} else
				if (getRuby().getActMethodScope() == Constants.SCOPE_PROTECTED) {
					noex = Constants.NOEX_PROTECTED;
				} else {
					noex = Constants.NOEX_PUBLIC;
				}
		}

		String name = id.toName();

		RubyId attrIV = getRuby().intern("@" + name);

		if (read) {
			addMethod(id, new NodeFactory(getRuby()).newIVar(attrIV), noex);
			// id.clearCache();
			funcall(getRuby().intern("method_added"), id.toSymbol());
		}

		if (write) {
			id = getRuby().intern(name + "=");
			addMethod(id, new NodeFactory(getRuby()).newAttrSet(attrIV), noex);
			// id.clearCache();
			funcall(getRuby().intern("method_added"), id.toSymbol());
		}
	}

	/** Getter for property included.
	 * @return Value of property included.
	 */
	public boolean isIncluded() {
		return this.included;
	}

	/** Setter for property included.
	 * @param included New value of property included.
	 */
	public void setIncluded(boolean included) {
		this.included = included;
	}

	/** method_list
	 *
	 */
	public RubyArray methodList(boolean option, RubyMapMethod method) {
		RubyArray ary = RubyArray.m_newArray(getRuby());

		for (RubyModule klass = this; klass != null; klass = klass.getSuperClass()) {
			klass.methods.foreach(method, ary);
			if (!option) {
				break;
			}
		}

		Iterator iter = ary.getList().iterator();
		while (iter.hasNext()) {
			if (getRuby().getNil() == iter.next()) {
				iter.remove();
				iter.next();
			}
		}

		return ary;
	}

	public RubyArray getConstOf(RubyArray ary) {
		RubyModule klass = this;
		while (klass != null) {
			getConstAt(ary);
			klass = klass.getSuperClass();
		}
		return ary;
	}

	public RubyArray getConstAt(RubyArray ary) {
		RubyMapMethod sv_i = new RubyMapMethod() {
			public int execute(Object key, Object value, Object arg) {
				if (((RubyId) key).isConstId()) {
					RubyString name = RubyString.m_newString(getRuby(), ((RubyId) key).toName());
					if (((RubyArray) arg).m_includes(name).isFalse()) {
						((RubyArray) arg).m_push(name);
					}
				}
				return RubyMapMethod.CONTINUE;
			}
		};

		if (getInstanceVariables() != null) {
			getInstanceVariables().foreach(sv_i, ary);
		}
		if (this == getRuby().getClasses().getObjectClass()) {
			getRuby().getClasses().getClassMap().foreach(sv_i, ary);
			/*if (autoload_tbl) {
			    st_foreach(autoload_tbl, autoload_i, ary);
			}*/
		}
		return ary;
	}

	/** set_method_visibility
	 *
	 */
	public void setMethodVisibility(RubyObject[] methods, int noex) {
		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't change method visibility");
		}

		for (int i = 0; i < methods.length; i++) {
			exportMethod(methods[i].toId(), noex);
		}
	}

	/** rb_export_method
	 *
	 */
	public void exportMethod(RubyId name, int noex) {
		if (this == getRuby().getClasses().getObjectClass()) {
			getRuby().secure(4);
		}

		MethodNode body = searchMethod(name);
		RubyModule origin = body.getMethodOrigin();

		if (body == null && isModule()) {
			body = getRuby().getClasses().getObjectClass().searchMethod(name);
			origin = body.getMethodOrigin();
		}

		if (body == null) {
		}

		if (body.getNoex() != noex) {
			if (this == origin) {
				body.setNoex(noex);
			} else {
				addMethod(name, new NodeFactory(getRuby()).newZSuper(), noex);
			}
		}
	}

	// Methods of the Module Class (rb_mod_*):

	/** rb_mod_new
	 *
	 */
	public static RubyModule m_newModule(Ruby ruby) {
		RubyModule newModule = new RubyModule(ruby, ruby.getClasses().getModuleClass());

		return newModule;
	}

	/** rb_mod_name
	 *
	 */
	public RubyString m_name() {
		RubyString path = getClassname();
		if (path != null) {
			return (RubyString) path.m_dup();
		}
		return RubyString.m_newString(getRuby(), "");
	}

	/** rb_mod_class_variables
	 *
	 */
	public RubyArray m_class_variables() {
		RubyArray ary = RubyArray.m_newArray(getRuby());

		RubyModule rbModule = this;

		if (isSingleton()) {
			rbModule =
				((RubyObject) rbModule.getInstanceVar("__atached__")).getClassVarSingleton();
		}

		while (rbModule != null) {
			if (rbModule.getInstanceVariables() != null) {
				Iterator iter = rbModule.getInstanceVariables().keySet().iterator();
				while (iter.hasNext()) {
					RubyId id = (RubyId) iter.next();
					if (id.isClassId()) {
						RubyString kval = RubyString.m_newString(getRuby(), id.toName());
						if (ary.m_includes(kval).isFalse()) {
							ary.push(kval);
						}
					}
				}
			}
			rbModule = rbModule.getSuperClass();
		}

		return ary;
	}

	/** rb_mod_clone
	 *
	 */
	public RubyObject m_clone() {
		RubyModule clone = new RubyModule(getRuby(), getRubyClass(), getSuperClass());
		clone.setupClone(this);

		if (getInstanceVariables() != null) {
			clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());
		}

		// clone the methods.
		if (methods != null) {
			clone.setMethods(new RubyHashMap());
			methods.foreach(new RubyMapMethod() {
				NodeFactory nf = new NodeFactory(getRuby());

				public int execute(Object key, Object value, Object arg) {
					MethodNode methodNode = (MethodNode) value;

					((RubyMap) arg).put(
						key,
						nf.newMethod(methodNode.getBodyNode(), methodNode.getNoex()));
					return RubyMapMethod.CONTINUE;
				}
			}, clone.methods);
		}

		return clone;
	}

	/** rb_mod_dup
	 *
	 */
	public RubyObject m_dup() {
		RubyModule dup = (RubyModule) m_clone();
		dup.setupObject(getRubyClass());

		dup.setSingleton(isSingleton());

		return dup;
	}

	/** rb_mod_included_modules
	 *
	 */
	public RubyArray m_included_modules() {
		RubyArray ary = RubyArray.m_newArray(getRuby());

		for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
			if (p.isIncluded()) {
				ary.push(p.getRubyClass());
			}
		}

		return ary;
	}

	/** rb_mod_ancestors
	 *
	 */
	public RubyArray m_ancestors() {
		RubyArray ary = RubyArray.m_newArray(getRuby());

		for (RubyModule p = this; p != null; p = p.getSuperClass()) {
			if (p.isSingleton()) {
				continue;
			}

			if (p.isIncluded()) {
				ary.push(p.getRubyClass());
			} else {
				ary.push(p);
			}
		}

		return ary;
	}

	/** rb_mod_to_s
	 *
	 */
	public RubyString m_to_s() {
		return (RubyString) getClassPath().m_dup();
	}

	/** rb_mod_eqq
	 *
	 */
	public RubyBoolean op_eqq(RubyObject obj) {
		return obj.m_kind_of(this);
	}

	/** rb_mod_le
	 *
	 */
	public RubyBoolean op_le(RubyObject obj) {
		if (!(obj instanceof RubyModule)) {
			throw new RubyTypeException(getRuby(), "compared with non class/module");
		}

		RubyModule mod = this;
		while (mod != null) {
			if (mod.methods == ((RubyModule) obj).methods) {
				return getRuby().getTrue();
			}
			mod = mod.getSuperClass();
		}

		return getRuby().getFalse();
	}

	/** rb_mod_lt
	 *
	 */
	public RubyBoolean op_lt(RubyObject obj) {
		if (obj == this) {
			return getRuby().getFalse();
		}
		return op_le(obj);
	}

	/** rb_mod_ge
	 *
	 */
	public RubyBoolean op_ge(RubyObject obj) {
		if (!(obj instanceof RubyModule)) {
			throw new RubyTypeException(getRuby(), "compared with non class/module");
		}

		return ((RubyModule) obj).op_le(this);
	}

	/** rb_mod_gt
	 *
	 */
	public RubyBoolean op_gt(RubyObject obj) {
		if (this == obj) {
			return getRuby().getFalse();
		}
		return op_ge(obj);
	}

	/** rb_mod_cmp
	 *
	 */
	public RubyFixnum op_cmp(RubyObject obj) {
		if (this == obj) {
			return RubyFixnum.m_newFixnum(getRuby(), 0);
		}

		if (!(obj instanceof RubyModule)) {
			throw new RubyTypeException(
				getRuby(),
				"<=> requires Class or Module (" + getRubyClass().toName() + " given)");
		}

		if (op_le(obj).isTrue()) {
			return RubyFixnum.m_newFixnum(getRuby(), -1);
		}

		return RubyFixnum.m_newFixnum(getRuby(), 1);
	}

	/** rb_mod_initialize
	 *
	 */
	public RubyObject m_initialize(RubyObject[] args) {
		return getRuby().getNil();
	}

	/** rb_module_s_new
	 *
	 */
	public static RubyModule m_new(Ruby ruby, RubyObject recv) {
		RubyModule mod = RubyModule.m_newModule(ruby);

		mod.setRubyClass((RubyModule) recv);
		ruby.getClasses().getModuleClass().callInit(null);

		return mod;
	}

	/** rb_mod_attr
	 *
	 */
	public RubyObject m_attr(RubySymbol symbol, RubyObject[] args) {
		boolean writeable = false;
		if (args.length > 0) {
			writeable = args[0].isTrue();
		}

		addAttribute(symbol.toId(), true, writeable, true);

		return getRuby().getNil();
	}

	/** rb_mod_attr_reader
	 *
	 */
	public RubyObject m_attr_reader(RubyObject[] args) {
		for (int i = 0; i < args.length; i++) {
			addAttribute(((RubySymbol) args[i]).toId(), true, false, true);
		}

		return getRuby().getNil();
	}

	/** rb_mod_attr_writer
	 *
	 */
	public RubyObject m_attr_writer(RubyObject[] args) {
		for (int i = 0; i < args.length; i++) {
			addAttribute(((RubySymbol) args[i]).toId(), false, true, true);
		}

		return getRuby().getNil();
	}

	/** rb_mod_attr_accessor
	 *
	 */
	public RubyObject m_attr_accessor(RubyObject[] args) {
		for (int i = 0; i < args.length; i++) {
			addAttribute(((RubySymbol) args[i]).toId(), true, true, true);
		}

		return getRuby().getNil();
	}

	/** rb_mod_const_get
	 *
	 */
	public RubyObject m_const_get(RubySymbol symbol) {
		RubyId id = symbol.toId();

		if (!id.isConstId()) {
			throw new RubyNameException(
				getRuby(),
				"wrong constant name " + symbol.getName());
		}

		return getConstant(id);
	}

	/** rb_mod_const_set
	 *
	 */
	public RubyObject m_const_set(RubySymbol symbol, RubyObject value) {
		RubyId id = symbol.toId();

		if (!id.isConstId()) {
			throw new RubyNameException(
				getRuby(),
				"wrong constant name " + symbol.getName());
		}

		setConstant(id, value);

		return value;
	}

	/** rb_mod_const_defined
	 *
	 */
	public RubyBoolean m_const_defined(RubySymbol symbol) {
		RubyId id = symbol.toId();

		if (!id.isConstId()) {
			throw new RubyNameException(
				getRuby(),
				"wrong constant name " + symbol.getName());
		}

		return RubyBoolean.m_newBoolean(getRuby(), isConstantDefined(id));
	}

	/** rb_class_instance_methods
	 *
	 */
	public RubyObject m_instance_methods(RubyObject[] args) {
		boolean includeSuper = false;

		if (args.length > 0) {
			includeSuper = args[0].isTrue();
		}

		return methodList(includeSuper, new RubyMapMethod() {
			public int execute(Object key, Object value, Object arg) {
				// cast args
				RubyId id = (RubyId) key;
				MethodNode body = (MethodNode) value;
				RubyArray ary = (RubyArray) arg;

				if ((body.getNoex() & (Constants.NOEX_PRIVATE | Constants.NOEX_PROTECTED))
					== 0) {
					RubyString name = RubyString.m_newString(getRuby(), id.toName());

					if (ary.m_includes(name).isFalse()) {
						if (body.getBodyNode() == null) {
							ary.push(getRuby().getNil());
						}
						ary.push(name);
					}
				} else
					if (body.getBodyNode() != null && body.getBodyNode() instanceof ZSuperNode) {
						ary.push(getRuby().getNil());
						ary.push(RubyString.m_newString(getRuby(), id.toName()));
					}
				return RubyMapMethod.CONTINUE;
			}
		});
	}

	/** rb_class_protected_instance_methods
	 *
	 */
	public RubyObject m_protected_instance_methods(RubyObject[] args) {
		boolean includeSuper = false;

		if (args.length > 0) {
			includeSuper = args[0].isTrue();
		}

		return methodList(includeSuper, new RubyMapMethod() {
			public int execute(Object key, Object value, Object arg) {
				// cast args
				RubyId id = (RubyId) key;
				MethodNode body = (MethodNode) value;
				RubyArray ary = (RubyArray) arg;

				if (body.getBodyNode() == null) {
					ary.push(getRuby().getNil());
					ary.push(RubyString.m_newString(getRuby(), id.toName()));
				} else
					if ((body.getNoex() & Constants.NOEX_PROTECTED) != 0) {
						RubyString name = RubyString.m_newString(getRuby(), id.toName());

						if (ary.m_includes(name).isFalse()) {
							ary.push(name);
						}
					} else
						if (body.getBodyNode() instanceof ZSuperNode) {
							ary.push(getRuby().getNil());
							ary.push(RubyString.m_newString(getRuby(), id.toName()));
						}
				return RubyMapMethod.CONTINUE;
			}
		});
	}

	/** rb_class_private_instance_methods
	 *
	 */
	public RubyObject m_private_instance_methods(RubyObject[] args) {
		boolean includeSuper = false;

		if (args.length > 0) {
			includeSuper = args[0].isTrue();
		}

		return methodList(includeSuper, new RubyMapMethod() {
			public int execute(Object key, Object value, Object arg) {
				// cast args
				RubyId id = (RubyId) key;
				MethodNode body = (MethodNode) value;
				RubyArray ary = (RubyArray) arg;

				if (body.getBodyNode() == null) {
					ary.push(getRuby().getNil());
					ary.push(RubyString.m_newString(getRuby(), id.toName()));
				} else
					if ((body.getNoex() & Constants.NOEX_PRIVATE) != 0) {
						RubyString name = RubyString.m_newString(getRuby(), id.toName());

						if (ary.m_includes(name).isFalse()) {
							ary.push(name);
						}
					} else
						if (body.getBodyNode() instanceof ZSuperNode) {
							ary.push(getRuby().getNil());
							ary.push(RubyString.m_newString(getRuby(), id.toName()));
						}
				return RubyMapMethod.CONTINUE;
			}
		});
	}

	/** rb_mod_constants
	 *
	 */
	public RubyObject m_constants() {
		RubyArray ary = RubyArray.m_newArray(getRuby());

		return getConstOf(ary);
	}

	/** rb_mod_remove_cvar
	 *
	 */
	public RubyObject m_remove_class_variable(RubyObject name) {
		RubyId id = null;
		if (name instanceof RubySymbol) {
			id = ((RubySymbol) name).toId();
		} else
			if (name instanceof RubyString) {
				id = getRuby().intern(((RubyString) name).getValue());
			}

		if (!id.isClassId()) {
			throw new RubyNameException(getRuby(), "wrong class variable name " + name);
		}
		if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't remove class variable");
		}
		if (isFrozen()) {
			throw new RubyFrozenException(getRuby(), "class/module");
		}

		if (getInstanceVariables() != null) {
			Object value = getInstanceVariables().remove(id);
			if (value != null) {
				return (RubyObject) value;
			}
		}

		if (isClassVarDefined(id)) {
			throw new RubyNameException(
				getRuby(),
				"cannot remove " + id.toName() + " for " + toName());
		}
		throw new RubyNameException(
			getRuby(),
			"class variable " + id.toName() + " not defined for " + toName());
	}

	/** rb_mod_append_features
	 *
	 */
	public RubyObject m_append_features(RubyModule module) {
		module.includeModule(this);
		return this;
	}

	/** rb_mod_extend_object
	 *
	 */
	public RubyObject m_extend_object(RubyObject obj) {
		obj.extendObject(this);
		return obj;
	}

	/** rb_mod_include
	 *
	 */
	public RubyObject m_include(RubyObject[] modules) {
		for (int i = 0; i < modules.length; i++) {
			funcall(getRuby().intern("append_features"), modules[i]);
		}

		return this;
	}

	/** rb_mod_public
	 *
	 */
	public RubyObject m_public(RubyObject[] args) {
		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't change method visibility");
		}

		if (args.length == 0) {
			getRuby().setActMethodScope(Constants.SCOPE_PUBLIC);
		} else {
			setMethodVisibility(args, Constants.NOEX_PUBLIC);
		}

		return this;
	}

	/** rb_mod_protected
	 *
	 */
	public RubyObject m_protected(RubyObject[] args) {
		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't change method visibility");
		}

		if (args.length == 0) {
			getRuby().setActMethodScope(Constants.SCOPE_PROTECTED);
		} else {
			setMethodVisibility(args, Constants.NOEX_PROTECTED);
		}

		return this;
	}

	/** rb_mod_private
	 *
	 */
	public RubyObject m_private(RubyObject[] args) {
		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't change method visibility");
		}

		if (args.length == 0) {
			getRuby().setActMethodScope(Constants.SCOPE_PRIVATE);
		} else {
			setMethodVisibility(args, Constants.NOEX_PRIVATE);
		}

		return this;
	}

	/** rb_mod_modfunc
	 *
	 */
	public RubyObject m_module_function(RubyObject[] args) {
		if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
			throw new RubySecurityException(
				getRuby(),
				"Insecure: can't change method visibility");
		}

		if (args.length == 0) {
			getRuby().setActMethodScope(Constants.SCOPE_MODFUNC);
		} else {
			setMethodVisibility(args, Constants.NOEX_PRIVATE);

			for (int i = 0; i < args.length; i++) {
				RubyId id = args[i].toId();
				MethodNode body = searchMethod(id);
				if (body == null || body.getBodyNode() == null) {
					throw new RubyBugException(
						"undefined method '" + id.toName() + "'; can't happen");
				}
				getSingletonClass().addMethod(id, body.getBodyNode(), Constants.NOEX_PUBLIC);
				// rb_clear_cache_by_id(id);
				funcall(getRuby().intern("singleton_added"), id.toSymbol());
			}
		}

		return this;
	}

	private static class GetMethodBodyResult {
		private Node body;
		private RubyModule recvClass;
		private RubyId id;
		private int noex;

		public GetMethodBodyResult(RubyModule recvClass, RubyId id, int noex) {
			this.recvClass = recvClass;
			this.id = id;
			this.noex = noex;
		}

		/** Getter for property id.
		 * @return Value of property id.
		 */
		public RubyId getId() {
			return id;
		}

		/** Setter for property id.
		 * @param id New value of property id.
		 */
		public void setId(RubyId id) {
			this.id = id;
		}

		/** Getter for property klass.
		 * @return Value of property klass.
		 */
		public RubyModule getRecvClass() {
			return recvClass;
		}

		/** Setter for property klass.
		 * @param klass New value of property klass.
		 */
		public void setRecvClass(RubyModule recvClass) {
			this.recvClass = recvClass;
		}

		/** Getter for property node.
		 * @return Value of property node.
		 */
		public Node getBody() {
			return body;
		}

		/** Setter for property node.
		 * @param node New value of property node.
		 */
		public void setBody(Node body) {
			this.body = body;
		}

		/** Getter for property scope.
		 * @return Value of property scope.
		 */
		public int getNoex() {
			return noex;
		}

		/** Setter for property scope.
		 * @param scope New value of property scope.
		 */
		public void setNoex(int noex) {
			this.noex = noex;
		}
	}

}