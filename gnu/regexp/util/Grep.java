/*
 *  gnu/regexp/util/Grep.java
 *  Copyright (C) 1998 Wes Biggs
 *  Copyright (C) 2001 Lee Sau Dan for the use of Reader for handling file I/O
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published
 *  by the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package gnu.regexp.util;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Grep is a pure-Java clone of the GNU grep utility.  As such, it is much
 * slower and not as full-featured, but it has the advantage of being
 * available on any system with a Java virtual machine.
 *
 * @author <A HREF="mailto:wes@cacas.org">Wes Biggs</A>
 *         <A HREF="http://www.csis.hku.hk/~sdlee/">Lee Sau Dan</A>
 * @version 1.02
 * @use gnu.getopt
 */
public class Grep {
  private static final int BYTE_OFFSET = 0;
  private static final int COUNT = 1;
  private static final int LINE_NUMBER = 2;
  private static final int QUIET = 3;
  private static final int SILENT = 4;
  private static final int NO_FILENAME = 5;
  private static final int REVERT_MATCH = 6;
  private static final int FILES_WITH_MATCHES = 7;
  private static final int LINE_REGEXP = 8;
  private static final int FILES_WITHOUT_MATCH = 9;

  private static final String PROGNAME = "gnu.regexp.util.Grep";
  private static final String PROGVERSION = "1.02";

  private Grep() { }
  /**
   * Invokes the grep() function below with the command line arguments
   * and using the RESyntax.RE_SYNTAX_GREP syntax, which attempts to
   * emulate the traditional UNIX grep syntax.
   */
  public static void main(String[] argv) {
    System.exit(grep(argv, RESyntax.RE_SYNTAX_GREP, System.out));
  }

  /**
   * Runs Grep with the specified arguments.  For a list of 
   * supported options, specify "--help".
   *
   * This is the meat of the grep routine, but unlike main(), you can
   * specify your own syntax and PrintStream to use for output.
   */
  public static int grep(String[] argv, RESyntax syntax, PrintStream out) {
    // use gnu.getopt to read arguments
    int cflags = 0;
    
    boolean[] options = new boolean [10];

    String encoding = null;
    
    LongOpt[] longOptions = { 
        new LongOpt("byte-offset",         LongOpt.NO_ARGUMENT, null, 'b'),
	new LongOpt("count",               LongOpt.NO_ARGUMENT, null, 'c'),
	new LongOpt("no-filename",         LongOpt.NO_ARGUMENT, null, 'h'),
	new LongOpt("ignore-case",         LongOpt.NO_ARGUMENT, null, 'i'),
	new LongOpt("files-with-matches",  LongOpt.NO_ARGUMENT, null, 'l'),
	new LongOpt("help",                LongOpt.NO_ARGUMENT, null, '!'),
	new LongOpt("line-number",         LongOpt.NO_ARGUMENT, null, 'n'),
	new LongOpt("quiet",               LongOpt.NO_ARGUMENT, null, 'q'),
	new LongOpt("silent",              LongOpt.NO_ARGUMENT, null, 'q'),
	new LongOpt("no-messages",         LongOpt.NO_ARGUMENT, null, 's'),
	new LongOpt("revert-match",        LongOpt.NO_ARGUMENT, null, 'v'),
	new LongOpt("line-regexp",         LongOpt.NO_ARGUMENT, null, 'x'),
	new LongOpt("extended-regexp",     LongOpt.NO_ARGUMENT, null, 'E'),
	new LongOpt("fixed-strings",       LongOpt.NO_ARGUMENT, null, 'F'), // TODO
	new LongOpt("basic-regexp",        LongOpt.NO_ARGUMENT, null, 'G'),
	new LongOpt("files-without-match", LongOpt.NO_ARGUMENT, null, 'L'),
	new LongOpt("version",             LongOpt.NO_ARGUMENT, null, 'V'),
	new LongOpt("encoding",      LongOpt.REQUIRED_ARGUMENT, null, 'N')
	  };

    Getopt g = new Getopt(PROGNAME, argv, "bchilnqsvxyEFGLVN:", longOptions);
    int c;
    String arg;
    while ((c = g.getopt()) != -1) {
      switch (c) {
      case 'b':
	options[BYTE_OFFSET] = true;
	break;
      case 'c':
	options[COUNT] = true;
	break;
      case 'h':
	options[NO_FILENAME] = true;
	break;
      case 'i':
      case 'y':
	cflags |= RE.REG_ICASE;
	break;
      case 'l':
	options[FILES_WITH_MATCHES] = true;
	break;
      case 'n':
	options[LINE_NUMBER] = true;
	break;
      case 'q':
	options[QUIET] = true;
	break;
      case 's':
	options[SILENT] = true;
	break;
      case 'v':
	options[REVERT_MATCH] = true;
	break;
      case 'x':
	options[LINE_REGEXP] = true;
	break;
      case 'E':  // TODO: check compatibility with grep
	syntax = RESyntax.RE_SYNTAX_EGREP;
	break;
      case 'F':  // TODO: fixed strings
	break;
      case 'G':
	syntax = RESyntax.RE_SYNTAX_GREP;
	break;
      case 'L':
	options[FILES_WITHOUT_MATCH] = true;
	break;
      case 'V':
	System.err.println(PROGNAME+' '+PROGVERSION);
	return 0;
      case 'N':
	encoding = g.getOptarg();
	try { // try out this encoding now.  If not found, fall back to default
	  "".getBytes(encoding);
	} catch (UnsupportedEncodingException uee) {
	  System.err.println(PROGNAME+": (Warning)"
			     + " Unsupported Encoding: " + encoding 
			     + "; reverting to default");
	  encoding = null;
	}
	break;
      case '!': // help
	  try {
	      BufferedReader br = new BufferedReader(new InputStreamReader((Grep.class).getResourceAsStream("GrepUsage.txt"),"UTF8"));
	      String line;
	      while ((line = br.readLine()) != null)
		  out.println(line);
	  } catch (IOException ie) { }
	return 0;
      }
    }	      
    
    InputStream is = null;
    RE pattern = null;
    if (g.getOptind() >= argv.length) {
      System.err.println("Usage: java " + PROGNAME + " [OPTION]... PATTERN [FILE]...");
      System.err.println("Try `java " + PROGNAME + " --help' for more information.");
      return 2;
    }
    try {
      pattern = new RE(argv[g.getOptind()],cflags,syntax);
    } catch (REException e) {
      System.err.println("Error in expression: "+e);
      return 2;
    }

    boolean notFound = true;
    if (argv.length >= g.getOptind()+2) {
      for (int i = g.getOptind() + 1; i < argv.length; i++) {
	  boolean no_filename = (argv.length == g.getOptind()+2)
	      || options[NO_FILENAME];
	if (argv[i].equals("-")) {
	    final String filename = no_filename ? null : "(standard input)";
	    if (processStream(pattern,System.in,encoding,options,filename,out))
		notFound = false;
	} else {
	    final String filename = no_filename ? null : argv[i];
	    try {
		File file = new File(argv[i]);
		if(file.isDirectory()) {
		    System.err.println(PROGNAME + ": " + argv[i] + ": Is a directory");
		} else if(!file.canRead()) {
		    System.err.println(PROGNAME + ": " + argv[i] + ": Permission denied");
		} else {
		    if (processStream(pattern,
				      new FileInputStream(argv[i]),
				      encoding, options, filename, out))
			notFound = false;
		}
	    } catch (FileNotFoundException e) {
		if (!options[SILENT])
		    System.err.println(PROGNAME+": "+e);
	    }
	}
      }
    } else {
	if (processStream(pattern,System.in,encoding,options,null,out))
	    notFound = false;
    }
    return notFound ? 1 : 0;
  }

  private static boolean processStream(RE pattern, InputStream is, 
				       String encoding, boolean[] options, 
				       String filename, PrintStream out) {
    try {
      final InputStreamReader isr = encoding == null?
	new InputStreamReader(is) : new InputStreamReader(is,encoding);
      final BufferedReader r = new BufferedReader(isr);
      return processReader(pattern, r, options, filename, out);
    } catch (UnsupportedEncodingException uee) {
      /* since grep() should have checked that the 'encoding' parameter
	 is valid, it should be impossible that this exception would
	 happen.  Of, sso, it is a logic error.
      */
      throw new Error(PROGNAME + ": programming logic error");
    }
  }

  private static boolean processReader(RE pattern,
				       BufferedReader br,
				       boolean[] options, String filename,
				       PrintStream out) {

    int newlineLen = System.getProperty("line.separator").length();
    int count = 0;
    long atByte = 0;
    int atLine = 1;
    String line;
    REMatch match;
    
    try {
      while ((line = br.readLine()) != null) {
	match = pattern.getMatch(line);
	if (((options[LINE_REGEXP] && pattern.isMatch(line))
	     || (!options[LINE_REGEXP] && (match != null))) 
	    ^ options[REVERT_MATCH]) {
	  count++;
	  if (!options[COUNT]) {
	    if (options[QUIET]) {
	      return true;
	    }
	    if (options[FILES_WITH_MATCHES]) {
	      if (filename != null)
		out.println(filename);
	      return true;
	    }
	    if (options[FILES_WITHOUT_MATCH]) {
	      return false;
	    }
	    if (filename != null) {
	      out.print(filename);
	      out.print(':');
	    }
	    if (options[LINE_NUMBER]) {
	      out.print(atLine);
	      out.print(':');
	    }
	    if (options[BYTE_OFFSET]) {
	      out.print(atByte + match.getStartIndex() );
	      out.print(':');
	    }
	    out.println(line);
	  }
	} // a match
	atByte += line.length() + newlineLen; // could be troublesome...
	atLine++;
      } // a valid line
      br.close();

      if (options[COUNT]) {
	if (filename != null)
	  out.println(filename+':');
	out.println(count);
      }
      if (options[FILES_WITHOUT_MATCH] && count==0) {
	if (filename != null)
	  out.println(filename);
      }
    } catch (IOException e) {
      System.err.println(PROGNAME+": "+e);
    }
    return ((count > 0) ^ options[REVERT_MATCH]);
  }
}
