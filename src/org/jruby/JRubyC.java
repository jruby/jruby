/*
 * JRubyC.java
 *
 * Created on January 11, 2007, 11:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.jruby.ast.Node;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.NodeCompilerFactory;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.StandardASMCompiler;

/**
 *
 * @author headius
 */
public class JRubyC {
    
    public static void main(String args[]) {
        Ruby runtime = Ruby.getDefaultInstance();
        
        try {
            if (args.length < 1 || args.length > 2) {
                System.out.println("Usage: jrubyc <filename> [<dest>]");
                return;
            }
            String filename = args[0];
            if (filename.startsWith("./")) filename = filename.substring(2);
            File srcfile = new File(filename);
            if (!srcfile.exists()) {
                System.out.println("Error -- file not found: " + filename);
                return;
            }
            File destfile = new File(System.getProperty("user.dir"));
            if (args.length == 2) {
                String dirname = args[1];
                destfile = new File(dirname);
                if (!destfile.exists()) {
                    System.out.println("Error -- destination not found: " + dirname);
                    return;
                }
                if (!destfile.isDirectory()) {
                    System.out.println("Error -- not a directory: " + dirname);
                }
            }

            int size = (int)srcfile.length();
            byte[] chars = new byte[size];
            new FileInputStream(srcfile).read(chars);
            // FIXME: encoding?
            String content = new String(chars);
            Node scriptNode = runtime.parseFile(new StringReader(content), filename, null);
        
            ASTInspector inspector = new ASTInspector();
            inspector.inspect(scriptNode);
            
            // do the compile
            String classPath = filename.substring(0, filename.lastIndexOf("."));
            String classDotted = classPath.replace('/', '.').replace('\\', '.');
            StandardASMCompiler compiler = new StandardASMCompiler(classPath, filename);
            System.out.println("Compiling file \"" + filename + "\" as class \"" + classDotted + "\"");
            NodeCompilerFactory.compileRoot(scriptNode, compiler, inspector);
            
            compiler.writeClass(destfile);
        } catch (IOException ioe) {
            System.err.println("Error -- IO exception during compile: " + ioe.getMessage());
        } catch (NotCompilableException nce) {
            System.err.println("Error -- Not compilable: " + nce.getMessage());
        }
    }
    
}
