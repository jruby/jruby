/*
 * Main.java - No description
 * Created on 18. September 2001, 21:48
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;

import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.RubyGlobalEntry;
import org.jruby.nodes.Node;

/**
 * Class used to launch the interpreter.
 * This is the main class as defined in the jruby.mf manifest.
 * It is very basic and does not support yet the same array of switches
 * as the C interpreter.
 *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
 *           -e 'command'    one line of script. Several -e's allowed. Omit [programfile]
 * @author  jpetersen
 * @version 0.1
 */
public class Main
{

	private static Class sRegexpAdapter;

	// print bugs
	// FIXME: remove if really not used Benoit.
	//	private static boolean printBugs = false;
	private static ArrayList sLoadDirectories = new ArrayList();
	private static String sScript = null;
	private static String sFileName = null;
	private static boolean sBenchmarkMode = false;
	private static boolean sCheckOnly = false;
	//list of libraries to require first
	private static ArrayList sRequireFirst = new ArrayList();
	/**
	 * process the command line arguments.
	 * This method will consume the appropriate arguments and valuate
	 * the static variables corresponding to the options.
	 * @param args the command line arguments
	 * @return the arguments left
	 **/
	private static String[] processArgs(String args[])
	{
		int lenArg = args.length;
		StringBuffer lBuf = new StringBuffer();
		int i = 0;
		for (; i < lenArg; i++)
		{
			if (args[i].equals("-h") || args[i].equals("-help"))
			{
				printUsage();
			} else if (args[i].startsWith("-I"))
			{
				sLoadDirectories.add(args[i].substring(2));
			} else if (args[i].startsWith("-r"))
			{
				sRequireFirst.add(args[i].substring(2));
			} else if (args[i].equals("-e"))
			{
				if (i++ >= lenArg)
				{
					System.err.println("invalid argument " + i);
					System.err.println(" -e must be followed by an expression to evaluate");
					printUsage();
				} else
				{
					lBuf.append(args[i]);
				}
			} else if (args[i].equals("-b"))
			{
				// Benchmark
				sBenchmarkMode = true;
				//FIXME remove if really not used Benoit
				//				 else if (args[i].equals("-bugs")) 
				//					printBugs = true;
			} else if (args[i].equals("-rx"))
			{
				if (++i >= lenArg)
				{
					System.err.println("invalid argument " + i);
					System.err.println(" -rx must be followed by an expression to evaluate");
					printUsage();
				} else
				{
					try
					{
						sRegexpAdapter = Class.forName(args[i]);
					} catch (Exception e)
					{
						System.err.println("invalid argument " + i);
						System.err.println("failed to load RegexpAdapter: " + args[i]);
						System.err.println("defaulting to default RegexpAdapter: GNURegexpAdapter");
					}
				}
			} else if (args[i].equals("-c"))
			{
				sCheckOnly = true;
			}
			else
			{
				if (lBuf.length() == 0)		//only get a filename if there were no -e
					sFileName = args[i++];	//consume the file name
				break;						//the rests are args for the script
			}
		}
		sScript = lBuf.toString();
		String[] lRet = new String[lenArg - i];
		System.arraycopy(args, i, lRet, 0, lRet.length);
		return lRet;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		/*
		   System.out.println();
		   System.out.println("----------------------------------------------------");
		   System.out.println("--- WARNING this is an ALPHA version of JRuby!!! ---");
		   System.out.println("----------------------------------------------------");
		   System.out.println();
		 */

		// Benchmark
		long now = -1;
		String[] argv =	processArgs(args);
		if (sBenchmarkMode)
			now = System.currentTimeMillis();
		if (sScript.length() > 0)
		{
			runInterpreter(sScript, "-e", argv);
		} else if (sFileName != null)
		{
			runInterpreterOnFile(sFileName, argv);
		} else
		{
			printUsage();	//interpreting from the command line not supported yet
			return;
		}
		// Benchmark
		if (now != -1)
		{
			System.out.println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
		}
	}
	static boolean sPrintedUsage = false;
	/**
	 * Prints the usage for the class.
	 *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
	 *           -e 'command'   one line of script. Several -e's allowed. Omit [programfile]
	 *           -b             benchmark mode
	 *           -Idirectory    specify $LOAD_PATH directory (may be used more than once)
	 *           -rx 'adapter'  used to select a regexp engine
	 *           -c 			check syntax and dump parse tree
	 */
	protected static void printUsage()
	{
		if (!sPrintedUsage)
		{
			System.out.println("Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]");
			System.out.println("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]");
			System.out.println("    -b              benchmark mode, times the script execution");
			System.out.println("    -Idirectory     specify $LOAD_PATH directory (may be used more than once)");
			System.out.println("    -rx 'class'     The adapter class for the regexp engine, for now can be:");
			System.out.println("                    org.jruby.regexp.GNURegexpAdapter or org.jruby.regexp.JDKRegexpAdapter");
			System.out.println("    -c 				check syntax and dump parse tree");

		}
	}

	/**
	 * Launch the interpreter on a specific String.
	 *
	 * @param iString2Eval the string to evaluate
	 * @param iFileName the name of the File from which the string comes.
	 */
	protected static void runInterpreter(String iString2Eval, String iFileName, String[] args)
	{
		// Initialize Runtime
		Ruby ruby = new Ruby();
		//FIXME: remove if really not used Benoit
		//		ruby.getRuntime().setPrintBugs(printBugs);
		if (sRegexpAdapter == null)
		{
			try
			{
				sRegexpAdapter = Class.forName("org.jruby.regexp.GNURegexpAdapter");
			} catch (Exception e)
			{
				throw new RuntimeException("Class GNURegexpAdapter not found");
			}
		}
		ruby.setRegexpAdapterClass(sRegexpAdapter);
		ruby.init();

		// Parse and interpret file
		RubyString rs = RubyString.newString(ruby, iString2Eval);
		RubyObject lArgv = JavaUtil.convertJavaToRuby(ruby, args, String[].class);
		ruby.defineGlobalConstant("ARGV", lArgv);
		RubyGlobalEntry.defineReadonlyVariable(ruby, "$*",  lArgv);
		ruby.initLoad(sLoadDirectories);
		//require additional libraries
		int lNbRequire = sRequireFirst.size();
		for (int i = 0; i < lNbRequire; i++)
		    RubyKernel.require(ruby, null, new RubyString(ruby, (String)sRequireFirst.get(i)));
		// +++
		try
		{	
			Node lScript = ruby.getRubyParser().compileString(iFileName, rs, 0);
			if (sCheckOnly)
				ruby.getRuntime().getOutputStream().println(lScript.toString());
			else
				ruby.getRubyTopSelf().eval(lScript);
		} catch (RaiseException rExcptn)
		{
			System.out.println(rExcptn.getActException().to_s().getValue());
		}
		// ---
	}

	/**
	 * Run the interpreter on a File.
	 * open a file and feeds it to the interpreter.
	 *
	 * @param fileName the name of the file to interpret
	 */
	protected static void runInterpreterOnFile(String fileName, String[] args)
	{
		File rubyFile = new File(fileName);
		if (!rubyFile.canRead())
		{
			System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
		} else
		{
			try
			{
				StringBuffer sb = new StringBuffer((int) rubyFile.length());
				BufferedReader br = new BufferedReader(new FileReader(rubyFile));
				String line;
				while ((line = br.readLine()) != null)
				{
					sb.append(line).append('\n');
				}
				br.close();
				runInterpreter(sb.toString(), fileName, args);

			} catch (IOException ioExcptn)
			{
				System.out.println("Cannot read Rubyfile: \"" + fileName + "\"");
				System.out.println("IOEception: " + ioExcptn.getMessage());
			}
		}
	}
}
