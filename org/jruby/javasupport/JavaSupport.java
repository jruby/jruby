package org.jruby.javasupport;

import java.lang.reflect.*;
import java.util.*;
import org.jruby.*;
import org.jruby.exceptions.*;

public class JavaSupport
{
	private Ruby ruby;

	private Map loadedJavaClasses = new HashMap();
	private List importedPackages = new LinkedList();

	private ClassLoader javaClassLoader = ClassLoader.getSystemClassLoader();

	public JavaSupport(Ruby ruby)
	{
		this.ruby = ruby;
	}
	/**
	 * translate java naming convention in ruby naming convention.
	 * translate getter and setter in ruby style accessor and 
	 * boolean getter in ruby style ? method
	 * @param javaName the name of the java method
	 * @return the name of the equivalent rubyMethod if a translation
	 * was needed null otherwise
	 **/
	private static String getRubyStyleInstanceMethodName(String javaName)
	{
			if (javaName.startsWith("get"))
			{
				return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4);
			} else if (javaName.startsWith("is"))
			{
				return Character.toLowerCase(javaName.charAt(2)) + javaName.substring(3) + "?";
			} else if (javaName.startsWith("can"))
			{
				return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
			} else if (javaName.startsWith("has"))
			{
				return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
			} else if (javaName.startsWith("set"))
			{
				return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "=";
			}
			return null;
	}

	/**
	 * translate java naming convention in ruby naming convention.
	 * does all the translation that getRubyStyleInstanceMethodName
	 * does plus translate getElementAt, getValueAt, setValueAt in
	 * [] and []=.  Also treat compareTo.
	 * @param javaName the name of the java method
	 * @return the name of the equivalent rubyMethod if a translation
	 * was needed null otherwise
	 **/
	private static String getRubyStyleMethodName(Ruby ruby, String javaName, RubyClass newRubyClass)
	{
if (javaName.equals("getElementAt"))
			if(javaName.equals("get"))		//java.util.List interface
			{
				return "[]";
			} else if (javaName.equals("getValueAt"))
			{
				return "[]";
			} else if (javaName.equals("setValueAt"))
			{
				return "[]=";
			} else if (javaName.equals("set"))			//java.util.List interface
			{
				return "[]=";
			} else if (javaName.equals("compareTo"))
			{
				newRubyClass.includeModule(ruby.getClasses().getComparableModule());
				return "<=>";
			}
			return getRubyStyleInstanceMethodName(javaName);
	}
	
	public RubyModule loadClass(Class javaClass, String rubyName)
	{
		if (javaClass == Object.class)
		{
			return ruby.getClasses().getJavaObjectClass();
		}

		if (loadedJavaClasses.get(javaClass) != null)
		{
			return (RubyModule) loadedJavaClasses.get(javaClass);
		}

		if (rubyName == null)
		{
			String javaName = javaClass.getName();
			rubyName = javaName.substring(javaName.lastIndexOf('.') + 1);
		}
		
		// Interfaces
		if (javaClass.isInterface())
		{
			RubyModule newInterface = ruby.defineModule(rubyName);
			newInterface.setInstanceVar("interfaceName", RubyString.newString(ruby, rubyName));
			defineModuleConstants(javaClass, newInterface);
			// ruby.defineGlobalConstant(rubyName, newInterface);
			return newInterface;
		}

		RubyClass superClass = (RubyClass) loadClass(javaClass.getSuperclass(), null);
		RubyClass newRubyClass = ruby.defineClass(rubyName, superClass);


		//Benoit start put back method definition for java objects
		Map methodMap = new HashMap();
		Map singletonMethodMap = new HashMap();

		Method[] methods = javaClass.getMethods(); 	//FIXME, this gets all the methods including the inherited one, not sure it is wanted
		
		for (int i = 0; i < methods.length; i++)
		{
			Method lCurMethod = methods[i];
			String methodName = lCurMethod.getName();
			if (lCurMethod.getDeclaringClass() != Object.class)
			{
				Map lCurMap;
				if (Modifier.isStatic(lCurMethod.getModifiers())) //choose the appropriate map depending on the staticness of the method
					lCurMap = singletonMethodMap;
				else
					lCurMap = methodMap;
				//due to overloading in java each methodName correspond to a list of methods
				List lMethods = (List)lCurMap.get(methodName);
				if (lMethods == null)		//this is not an overload, create the list
				{
					lMethods = new LinkedList();
					lCurMap.put(methodName, lMethods);
				}	
				lMethods.add(lCurMethod);	//add to the list
			}
		}


		newRubyClass.defineSingletonMethod("new", new JavaConstructor(javaClass.getConstructors()));

		Iterator iter = methodMap.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry entry = (Map.Entry) iter.next();

			String javaName = (String)entry.getKey();
			String lRubyStyleName = getRubyStyleMethodName(ruby, javaName, newRubyClass);
			//make sure we add the method and not replace a previously existing version
			if (lRubyStyleName != null) //need to treat the case where the javaName was modified specially 
			{ 
				List lMethodList = (List)methodMap.get(javaName);
				if (lMethodList != null)
					lMethodList.addAll((List)entry.getValue());
				else 
					lMethodList = (List)entry.getValue();
				methods = (Method[]) lMethodList.toArray(new Method[lMethodList.size()]);
				newRubyClass.defineMethod(lRubyStyleName, new JavaMethod(methods));
			}
			List lMethodList = (List) entry.getValue();
			methods = (Method[]) lMethodList.toArray(new Method[lMethodList.size()]);
			newRubyClass.defineMethod(javaName, new JavaMethod(methods));
		}

		if (methodMap.keySet().contains("hasNext") && methodMap.keySet().contains("next"))
		{
			newRubyClass.includeModule(ruby.getClasses().getEnumerableModule());
			newRubyClass.defineMethod("each", new JavaEachMethod("next?", "next"));
		} else if (methodMap.keySet().contains("hasMoreElements") && methodMap.keySet().contains("nextElement"))
		{
			newRubyClass.includeModule(ruby.getClasses().getEnumerableModule());
			newRubyClass.defineMethod("each", new JavaEachMethod("moreElements?", "nextElement"));
		}/* 
			Benoit FIXME: commented because it doesn't make sense, JavaEachMethod is incomplete and use
			next? which is potentially undefined and next is tested when it is garanteed to be there
			else if (ResultSet.class.isAssignableFrom(javaClass) && methodMap.keySet().contains("next"))		//second test should not be necessary since it is (first test) a subclass of ResultSet which defines next
			{
			newRubyClass.includeModule(ruby.getClasses().getEnumerableModule());
			newRubyClass.defineMethod("each", new JavaEachMethod("next?", null));
			}
		  */
		iter = singletonMethodMap.entrySet().iterator();
		while (iter.hasNext())
		{

			Map.Entry entry = (Map.Entry) iter.next();
			//make sure we add the method and not replace a previously existing version


			String javaName = (String)entry.getKey();
			String lRubyStyleName = getRubyStyleInstanceMethodName(javaName);

			if (lRubyStyleName != null)
			{ 
				List lMethodList = (List)singletonMethodMap.get(lRubyStyleName);
				if (lMethodList != null)
					lMethodList.addAll((List)entry.getValue());
				else 
					lMethodList = (List)entry.getValue();
				methods = (Method[]) lMethodList.toArray(new Method[lMethodList.size()]);
				newRubyClass.defineSingletonMethod(lRubyStyleName, new JavaMethod(methods, true));
			}
			List lMethodList = (List) entry.getValue();
			methods = (Method[]) lMethodList.toArray(new Method[lMethodList.size()]);
			newRubyClass.defineSingletonMethod(javaName, new JavaMethod(methods, true));
		}

		defineModuleConstants(javaClass, newRubyClass);

		//Benoit end
		loadedJavaClasses.put(javaClass, newRubyClass);

		return newRubyClass;
	}

	private void defineModuleConstants(Class javaClass, RubyModule rubyModule)
	{
		// add constants
		Field[] fields = javaClass.getFields();
		for (int i = 0; i < fields.length; i++)
		{
			int modifiers = fields[i].getModifiers();
			if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))
			{
				try
				{
					String name = fields[i].getName();
					if (Character.isLowerCase(name.charAt(0)))
					{
						name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
					}
					rubyModule.defineConstant(name, JavaUtil.convertJavaToRuby(ruby, fields[i].get(null), fields[i].getType()));
				} catch (IllegalAccessException iaExcptn)
				{
				}
			}
		}
	}
	
	
	public void defineWrapperMethods(Class javaClass, RubyModule rubyClass)
	{
		Map methodMap = new HashMap();
		Map singletonMethodMap = new HashMap();

		Method[] methods = javaClass.getDeclaredMethods();

		for (int i = 0; i < methods.length; i++)
		{
			String methodName = methods[i].getName();
			if (Modifier.isStatic(methods[i].getModifiers()))
			{
				if (singletonMethodMap.get(methods[i].getName()) == null)
				{
					singletonMethodMap.put(methods[i].getName(), new LinkedList());
				}
				((List) singletonMethodMap.get(methods[i].getName())).add(methods[i]);
			} else
			{
				if (methodMap.get(methods[i].getName()) == null)
				{
					methodMap.put(methods[i].getName(), new LinkedList());
				}
				((List) methodMap.get(methods[i].getName())).add(methods[i]);
			}
		}

		if (javaClass.getConstructors().length > 0)
		{
			rubyClass.defineSingletonMethod("new", new JavaConstructor(javaClass.getConstructors()));
		} else
		{
			rubyClass.getSingletonClass().undefMethod("new");
		}

		Iterator iter = methodMap.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry entry = (Map.Entry) iter.next();
			methods = (Method[]) ((List) entry.getValue()).toArray(new Method[((List) entry.getValue()).size()]);

			rubyClass.defineMethod(convertMethodName((String) entry.getKey()), new JavaMethod(methods));
		}

		iter = singletonMethodMap.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry entry = (Map.Entry) iter.next();
			methods = (Method[]) ((List) entry.getValue()).toArray(new Method[((List) entry.getValue()).size()]);

			String javaName = (String) entry.getKey();
			if (javaName.startsWith("get"))
			{
				javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4);
			} else if (javaName.startsWith("is"))
			{
				javaName = Character.toLowerCase(javaName.charAt(2)) + javaName.substring(3) + "?";
			} else if (javaName.startsWith("can"))
			{
				javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
			} else if (javaName.startsWith("has"))
			{
				javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
			} else if (javaName.startsWith("set"))
			{
				javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "=";
			}

			rubyClass.defineSingletonMethod(javaName, new JavaMethod(methods, true));
		}

	}

	private String convertMethodName(String javaName)
	{
		if (javaName.equals("getElementAt"))
		{
			return "[]";
		} else if (javaName.equals("getValueAt"))
		{
			return "[]";
		} else if (javaName.equals("setValueAt"))
		{
			return "[]=";
		} else if (javaName.startsWith("get"))
		{
			return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4);
		} else if (javaName.startsWith("is"))
		{
			return Character.toLowerCase(javaName.charAt(2)) + javaName.substring(3) + "?";
		} else if (javaName.startsWith("can"))
		{
			return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
		} else if (javaName.startsWith("has"))
		{
			return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
		} else if (javaName.startsWith("set"))
		{
			return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "=";
		} else if (javaName.equals("compareTo"))
		{
			return "<=>";
		}
		return javaName;
	}

	private void addDefaultModules(Set methodNames, RubyClass rubyClass)
	{
		if (methodNames.contains("hasNext") && methodNames.contains("next"))
		{
			rubyClass.includeModule(ruby.getClasses().getEnumerableModule());
			rubyClass.defineMethod("each", new JavaEachMethod(convertMethodName("hasNext"), convertMethodName("next")));
		} else if (methodNames.contains("hasMoreElements") && methodNames.contains("nextElement"))
		{
			rubyClass.includeModule(ruby.getClasses().getEnumerableModule());
			rubyClass.defineMethod(
					"each",
					new JavaEachMethod(convertMethodName("hasMoreElements"), convertMethodName("nextElement")));
		} else if (methodNames.contains("next"))
		{
			rubyClass.includeModule(ruby.getClasses().getEnumerableModule());
			rubyClass.defineMethod("each", new JavaEachMethod(convertMethodName("next"), null));
		}

		if (methodNames.contains("compareTo"))
		{
			rubyClass.includeModule(ruby.getClasses().getComparableModule());
		}
	}

	public Class loadJavaClass(RubyString name)
	{
		String className = name.getValue();

		try
		{
			return javaClassLoader.loadClass(className);
		} catch (ClassNotFoundException cnfExcptn)
		{
			Iterator iter = importedPackages.iterator();
			while (iter.hasNext())
			{
				String packageName = (String) iter.next();
				try
				{
					return javaClassLoader.loadClass(packageName + "." + className);
				} catch (ClassNotFoundException cnfExcptn_)
				{
				}
			}
		}
		throw new RubyNameException(ruby, "cannot load Java class: " + name.getValue());
	}

	public void addImportPackage(String packageName)
	{
		importedPackages.add(packageName);
	}
}
