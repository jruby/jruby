package org.jruby.javasupport;

import java.lang.reflect.*;
import java.util.*;
import org.jruby.*;
import org.jruby.exceptions.*;

public class JavaSupport {
    private Ruby ruby;

    private Map loadedJavaClasses = new HashMap();
    private List importedPackages = new LinkedList();

    private ClassLoader javaClassLoader = ClassLoader.getSystemClassLoader();

    public JavaSupport(Ruby ruby) {
        this.ruby = ruby;
    }

    public RubyModule loadClass(Class javaClass, String rubyName) {
        if (javaClass == Object.class) {
            return ruby.getClasses().getJavaObjectClass();
        }

        if (loadedJavaClasses.get(javaClass) != null) {
            return (RubyModule) loadedJavaClasses.get(javaClass);
        }

        if (rubyName == null) {
            String javaName = javaClass.getName();
            rubyName = javaName.substring(javaName.lastIndexOf('.') + 1);
        }

        // Interfaces
        if (javaClass.isInterface()) {
            RubyModule newInterface = ruby.defineModule(rubyName);
            newInterface.setInstanceVar("interfaceName", RubyString.newString(ruby, rubyName));
            // ruby.defineGlobalConstant(rubyName, newInterface);
            return newInterface;
        }

        RubyClass superClass = (RubyClass) loadClass(javaClass.getSuperclass(), null);
        RubyClass newRubyClass = ruby.defineClass(rubyName, superClass);

        // add constants
        Field[] fields = javaClass.getFields();
        for (int i = 0; i < fields.length; i++) {
            int modifiers = fields[i].getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                try {
                    String name = fields[i].getName();
                    if (Character.isLowerCase(name.charAt(0))) {
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
                    newRubyClass.defineConstant(name, JavaUtil.convertJavaToRuby(ruby, fields[i].get(null), fields[i].getType()));
                } catch (IllegalAccessException iaExcptn) {
                }
            }
        }

        loadedJavaClasses.put(javaClass, newRubyClass);

        return newRubyClass;
    }

    public void defineWrapperMethods(Class javaClass, RubyClass rubyClass) {
        Map methodMap = new HashMap();
        Map singletonMethodMap = new HashMap();

        Method[] methods = javaClass.getDeclaredMethods();

        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            if (Modifier.isStatic(methods[i].getModifiers())) {
                if (singletonMethodMap.get(methods[i].getName()) == null) {
                    singletonMethodMap.put(methods[i].getName(), new LinkedList());
                }
                ((List) singletonMethodMap.get(methods[i].getName())).add(methods[i]);
            } else {
                if (methodMap.get(methods[i].getName()) == null) {
                    methodMap.put(methods[i].getName(), new LinkedList());
                }
                ((List) methodMap.get(methods[i].getName())).add(methods[i]);
            }
        }

        if (javaClass.getConstructors().length > 0) {
            rubyClass.defineSingletonMethod("new", new JavaConstructor(javaClass.getConstructors()));
        } else {
            rubyClass.getSingletonClass().undefMethod("new");
        }

        Iterator iter = methodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            methods = (Method[]) ((List) entry.getValue()).toArray(new Method[((List) entry.getValue()).size()]);

            rubyClass.defineMethod(convertMethodName((String) entry.getKey()), new JavaMethod(methods));
        }

        iter = singletonMethodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            methods = (Method[]) ((List) entry.getValue()).toArray(new Method[((List) entry.getValue()).size()]);

            String javaName = (String) entry.getKey();
            if (javaName.startsWith("get")) {
                javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4);
            } else if (javaName.startsWith("is")) {
                javaName = Character.toLowerCase(javaName.charAt(2)) + javaName.substring(3) + "?";
            } else if (javaName.startsWith("can")) {
                javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
            } else if (javaName.startsWith("has")) {
                javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
            } else if (javaName.startsWith("set")) {
                javaName = Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "=";
            }

            rubyClass.defineSingletonMethod(javaName, new JavaMethod(methods, true));
        }

    }

    private String convertMethodName(String javaName) {
        if (javaName.equals("getElementAt")) {
            return "[]";
        } else if (javaName.equals("getValueAt")) {
            return "[]";
        } else if (javaName.equals("setValueAt")) {
            return "[]=";
        } else if (javaName.startsWith("get")) {
            return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4);
        } else if (javaName.startsWith("is")) {
            return Character.toLowerCase(javaName.charAt(2)) + javaName.substring(3) + "?";
        } else if (javaName.startsWith("can")) {
            return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
        } else if (javaName.startsWith("has")) {
            return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "?";
        } else if (javaName.startsWith("set")) {
            return Character.toLowerCase(javaName.charAt(3)) + javaName.substring(4) + "=";
        } else if (javaName.equals("compareTo")) {
            return "<=>";
        }
        return javaName;
    }

    private void addDefaultModules(Set methodNames, RubyClass rubyClass) {
        if (methodNames.contains("hasNext") && methodNames.contains("next")) {
            rubyClass.includeModule(ruby.getClasses().getEnumerableModule());
            rubyClass.defineMethod("each", new JavaEachMethod(convertMethodName("hasNext"), convertMethodName("next")));
        } else if (methodNames.contains("hasMoreElements") && methodNames.contains("nextElement")) {
            rubyClass.includeModule(ruby.getClasses().getEnumerableModule());
            rubyClass.defineMethod(
                "each",
                new JavaEachMethod(convertMethodName("hasMoreElements"), convertMethodName("nextElement")));
        } else if (methodNames.contains("next")) {
            rubyClass.includeModule(ruby.getClasses().getEnumerableModule());
            rubyClass.defineMethod("each", new JavaEachMethod(convertMethodName("next"), null));
        }

        if (methodNames.contains("compareTo")) {
            rubyClass.includeModule(ruby.getClasses().getComparableModule());
        }
    }

    public Class loadJavaClass(RubyString name) {
        String className = name.getValue();

        try {
            return javaClassLoader.loadClass(className);
        } catch (ClassNotFoundException cnfExcptn) {
            Iterator iter = importedPackages.iterator();
            while (iter.hasNext()) {
                String packageName = (String) iter.next();
                try {
                    return javaClassLoader.loadClass(packageName + "." + className);
                } catch (ClassNotFoundException cnfExcptn_) {
                }
            }
        }
        throw new RubyNameException(ruby, "cannot load Java class: " + name.getValue());
    }
    
    public void addImportPackage(String packageName) {
        importedPackages.add(packageName);
    }
}