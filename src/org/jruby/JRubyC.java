/*
 * JRubyC.java
 *
 * Created on January 11, 2007, 11:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.charset.CharsetDecoder;
import org.jruby.ast.Node;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.util.KCode;

/**
 *
 * @author headius
 */
public class JRubyC {
    
    public static void main(String args[]) {
        Ruby runtime = Ruby.newInstance();
        
        try {
            if (args.length < 1) {
                System.out.println("Usage: jrubyc <filename> [<filename> ...]");
                return;
            }
            for (int i = 0; i < args.length; i++) {
                String filename = args[i];
                if (filename.startsWith("./")) filename = filename.substring(2);
                File srcfile = new File(filename);
                if (!srcfile.exists()) {
                    System.out.println("Error -- file not found: " + filename);
                    return;
                }
                
                // destination directory
                File destfile = new File(System.getProperty("user.dir"));

                int size = (int)srcfile.length();
                byte[] chars = new byte[size];
                new FileInputStream(srcfile).read(chars);
                // FIXME: -K encoding?
                String content = new String(chars);
                Node scriptNode = runtime.parseFile(new ByteArrayInputStream(content.getBytes("ISO-8859-1")), filename, null);

                ASTInspector inspector = new ASTInspector();
                inspector.inspect(scriptNode);

                // do the compile
                String classPath = filename.substring(0, filename.lastIndexOf(".")).replace('-', '_').replace('.', '_');
                int lastSlashIndex = classPath.lastIndexOf('/');
                if (!Character.isJavaIdentifierStart(classPath.charAt(lastSlashIndex + 1))) {
                    if (lastSlashIndex == -1) {
                        classPath = "_" + classPath;
                    } else {
                        classPath = classPath.substring(0, lastSlashIndex + 1) + "_" + classPath.substring(lastSlashIndex + 1);
                    }
                }
                StandardASMCompiler compiler = new StandardASMCompiler(classPath, filename);
                ASTCompiler.compileRoot(scriptNode, compiler, inspector);

                compiler.writeClass(destfile);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Error -- IO exception during compile: " + ioe.getMessage());
        } catch (NotCompilableException nce) {
            System.err.println("Error -- Not compilable: " + nce.getMessage());
        }
    }
    
}
