/*
 * gnu/regexp/util/Tests.java -- Simple testsuite for gnu.regexp package
 * Copyright (C) 1998-2001 Wes Biggs
 *
 * This file is in the public domain.  However, the gnu.regexp library
 * proper is licensed under the terms of the GNU Lesser General Public
 * License (see the file COPYING.LIB for details).
 */

package gnu.regexp.util;
import gnu.regexp.*;
import java.io.StringBufferInputStream;
import java.io.StringReader;

/**
 * This is a very basic testsuite application for gnu.regexp.
 *
 * @author <A HREF="mailto:wes@cacas.org">Wes Biggs</A>
 * @version 1.1.1
 */
public class Tests {
  private Tests() { }

    private static void check(RE expr, String input, String expect, int id) {
	// Test it using all possible input types
	check(expr.getMatch(input),expect,id, "String");
	check(expr.getMatch(new StringBuffer(input)),expect,id, "StringBuffer");
	check(expr.getMatch(input.toCharArray()),expect,id, "char[]");
	check(expr.getMatch(new StringReader(input)),expect,id, "Reader");
	check(expr.getMatch(new StringBufferInputStream(input)),expect,id, "InputStream");
    }
  private static void check(REMatch m, String expect, int x, String type) {
    if ((m == null) || !m.toString().equals(expect)) System.out.print("*** Failed");
    else System.out.print("Passed");
    System.out.println(" test #"+x + " (" + type + ")");
  }

  /**
   * Runs the testsuite.  No command line arguments are necessary. 
   *
   * @exception REException An error occurred compiling a regular expression.
   */
  public static void main(String[] argv) throws REException {
    RE e;

    
    e = new RE("(.*)z");
    check(e,("xxz"),"xxz",1);

    e = new RE(".*z");
    check(e,("xxz"),"xxz",2);
    
    e = new RE("(x|xy)z");
    check(e,("xz"),"xz",3);
    check(e,("xyz"),"xyz",4);

    e = new RE("(x)+z");
    check(e,("xxz"),"xxz",5);

    e = new RE("abc");
    check(e,("xyzabcdef"),"abc",6);

    e = new RE("^start.*end$");
    check(e,("start here and go to the end"),"start here and go to the end",7);

    e = new RE("(x|xy)+z");
    check(e,("xxyz"),"xxyz",8);

    e = new RE("type=([^ \t]+)[ \t]+exts=([^ \t\n\r]+)");
    check(e,("type=text/html	exts=htm,html"),"type=text/html	exts=htm,html",9);

    e = new RE("(x)\\1");
    check(e,("zxxz"),"xx", 10);

    e = new RE("(x*)(y)\\2\\1");
    check(e,("xxxyyxx"),"xxyyxx",11);

    e = new RE("[-go]+");
    check(e,("go-go"),"go-go",12);

    e = new RE("[\\w-]+");
    check(e,("go-go"),"go-go",13);

    e = new RE("^start.*?end");
    check(e,("start here and end in the middle, not the very end"),"start here and end",14);
    
    e = new RE("\\d\\s\\w\\n\\r");
    check(e,("  9\tX\n\r  "),"9\tX\n\r",15);

    e = new RE("zow",RE.REG_ICASE);
    check(e,("ZoW"),"ZoW",16);

    e = new RE("(\\d+)\\D*(\\d+)\\D*(\\d)+");
    check(e,("size--10 by 20 by 30 feet"),"10 by 20 by 30",17);

    e = new RE("(ab)(.*?)(d)");
    REMatch m = e.getMatch("abcd");
    check(m,"abcd",18, "String");
    System.out.println(((m.toString(2).equals("c")) ? "Pass" : "*** Fail") 
		       + "ed test #19");

    e = new RE("^$");
    check(e,(""),"",20);

    e = new RE("a*");
    check(e,(""),"",21);
    check(e,("a"),"a",22);
    check(e,("aa"),"aa",23);

    e = new RE("(([12]))?");
    check(e,("12"),"1",24);

    e = new RE("(.*)?b");
    check(e,("ab"),"ab",25);

    e = new RE("(.*)?-(.*)"); 
    check(e,("a-b"), "a-b", 26);

    e = new RE("(a)b");
    check(e,("aab"), "ab", 27);

    e = new RE("[M]iss");
    check(e,("one Mississippi"), "Miss", 28);

    e = new RE("\\S Miss");
    check(e,("one Mississippi"), "e Miss", 29);

    e = new RE("a*");
    check(e,("b"),"",30);
    check(e,("ab"),"a",31);
    check(e,("aab"),"aa",32);

    // Single character should match anywhere in String
    e = new RE("a");
    check(e,("a"),"a", 33);
    check(e,("ab"),"a", 34);
    check(e,("ba"),"a", 35);
    check(e,("bab"),"a", 36);
    
  }
}      
