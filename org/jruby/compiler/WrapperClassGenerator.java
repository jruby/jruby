/*
 * WrapperClassGenerator.java
 * Created on 16.02.2002, 17:35:47
 * 
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "JRuby" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For 
 *    written permission, please contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called 
 *    "JRuby", nor may "JRuby" appear in their name, without prior 
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * ====================================================================
 *
 */
package org.jruby.compiler;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class WrapperClassGenerator {

    public static void main(String[] args) throws IOException {
        Ruby ruby = Ruby.getDefaultInstance(null);

        String fc = loadFile(args[0]);
        ruby.evalScript(fc, null);
        
        RubyClass type = ruby.getRubyClass(args[1]);
        
        generateWrapper(type);
    }
    
    private static void generateWrapper(RubyClass type) throws IOException {
        String name = type.name().toString();
        
        RubyClass superClass = type.getSuperClass();
        Class javaClass = type.getRuby().getJavaSupport().getJavaClass(superClass);
        
        PrintWriter pw = new PrintWriter(new FileWriter(name + ".java"));
        
        pw.println("import org.jruby.*;");
        pw.println("import org.jruby.javasupport.*;");
        pw.println();
        pw.println("public class " + name + " extends " + javaClass.getName() + "{");
        pw.println("    private static Ruby ruby = null;");
        pw.println();
        pw.println("    private RubyObject __object__ = null;");
        pw.println();
        pw.println("    static {");
        pw.println("        ruby = new " + name + "Script().__start__(new String[]{});");
        pw.println("    }");
        pw.println();
        pw.println("    public " + name + "() {");
        pw.println("        __object__ = ruby.getRubyClass(\"" + name + "\").funcall(\"new\");");
        pw.println("    }");
        pw.println();
        
        Iterator iter = type.getMethods().keySet().iterator();
        while (iter.hasNext()) {
            String methodname = (String) iter.next();
            if (!methodname.equals("new")) {
                for (int i = 0; i < javaClass.getMethods().length; i++) {
                    if (javaClass.getMethods()[i].getName().equals(methodname)) {
                        generateMethod(methodname, javaClass.getMethods()[i], pw);
                    }
                }
                for (int i = 0; i < javaClass.getDeclaredMethods().length; i++) {
                    if (javaClass.getDeclaredMethods()[i].getName().equals(methodname)) {
                        generateMethod(methodname, javaClass.getDeclaredMethods()[i], pw);
                    }
                }
            }
        }

		pw.println("}");
        pw.close();
    }

    private static void generateMethod(String name, Method method, PrintWriter pw) throws IOException {
        pw.print("    public " + method.getReturnType().getName() + " " + name + "(");
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(method.getParameterTypes()[i].getName());
            pw.print(" arg" + i);
        }
        pw.println(") {");
        pw.print("    RubyObject result = __object__.funcall(\"" + name + "\", JavaUtil.convertJavaArrayToRuby(ruby, new Object[] {");
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            pw.print("arg" + i + ", ");
        }
        pw.println("}));");
        if (method.getReturnType() != Void.TYPE) {
            pw.println("    return JavaUtil.convertRubyToJava(ruby, result, " + method.getReturnType() + ".class)");
        }
        pw.println("    }");
        pw.println();
    }
    
    
    private static String loadFile(String fileName) {
        try {
            File rubyFile = new File(fileName);
            StringBuffer sb = new StringBuffer((int) rubyFile.length());
            BufferedReader br = new BufferedReader(new FileReader(rubyFile));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            return sb.toString();

        } catch (IOException ioExcptn) {
            return "";
        }
    }
}