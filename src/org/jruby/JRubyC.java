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
import org.jruby.ast.Node;
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
            File srcfile = new File(filename);
            if (!srcfile.exists()) {
                System.err.println("Error -- file not found: " + filename);
                return;
            }
            File destfile = new File(System.getProperty("user.dir"));
            if (args.length == 2) {
                String dirname = args[1];
                destfile = new File(dirname);
                if (!destfile.exists()) {
                    System.err.println("Error -- destination not found: " + dirname);
                    return;
                }
                if (!destfile.isDirectory()) {
                    System.err.println("Error -- not a directory: " + dirname);
                }
            }

            int size = (int)srcfile.length();
            byte[] chars = new byte[size];
            new FileInputStream(srcfile).read(chars);
            // FIXME: encoding?
            String content = new String(chars);
            Node scriptNode = runtime.parse(content, filename, null, 0);
            
            // do the compile
            StandardASMCompiler compiler = new StandardASMCompiler(filename.substring(0, filename.lastIndexOf(".")), filename);
            NodeCompilerFactory.getCompiler(scriptNode).compile(scriptNode, compiler);
            
            compiler.writeClass(destfile);
        } catch (IOException ioe) {
            System.err.println("Error -- IO exception during compile: " + ioe.getMessage());
        } catch (NotCompilableException nce) {
            System.err.println("Error -- Not compilable: " + nce.getMessage());
        }
    }
    
}
