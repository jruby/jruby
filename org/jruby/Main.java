/*
 * Main.java - No description
 * Created on 18. September 2001, 21:48
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import java.io.*;

import org.jruby.parser.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class Main {

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        System.out.println("WARNING this is an ALPHA version of JRuby!!!");
        System.out.println("--------------------------------------------");
        System.out.println();
        if (args.length == 0) {
            printUsage();
        } else {
            /*if (args[0].equals("-version")) {
                printVersion();
            } else*/ if (args[0].equals("-help")) {
                printUsage();
            } else {
                runInterpreter(args[0]);
            }
        }
    }
    
    protected static void printUsage() {
        System.out.println("Usage: java -jar jruby.jar rubyfile.rb");
    }
    
    protected static void runInterpreter(String fileName) {
        File rubyFile = new File(fileName);
        if (!rubyFile.canRead()) {
            System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
        } else {
            try {
                StringBuffer sb = new StringBuffer((int)rubyFile.length());
                BufferedReader br = new BufferedReader(new FileReader(rubyFile));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                br.close();
                
                // Initialize Runtime
                Ruby ruby = new Ruby();
                
                // Initialize Parser
                parse p = new parse(ruby);
                
                // Parse and interpret file
                RubyString rs = RubyString.m_newString(ruby, sb.toString());
                ruby.getInterpreter().eval(ruby.getObjectClass(), p.rb_compile_string(fileName, rs, 0));
                
            } catch (IOException ioExcptn) {
                System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
                System.out.println("IOEception: " + ioExcptn.getMessage());
            }
        }
    }
}