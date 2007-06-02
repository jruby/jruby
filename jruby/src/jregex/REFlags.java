/**
 * Copyright (c) 2001, Sergey A. Samokhodkin
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer. 
 * - Redistributions in binary form 
 * must reproduce the above copyright notice, this list of conditions and the following 
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of jregex nor the names of its contributors may be used 
 * to endorse or promote products derived from this software without specific prior 
 * written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY 
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @version 1.2_01
 */

package jregex;

public interface REFlags{
  /**
   * All the foolowing options turned off
   */
   public int DEFAULT=0;
   
  /**
   * Pattern "a" matches both "a" and "A".
   * Corresponds to "i" in Perl notation.
   */
   public int IGNORE_CASE=1<<0;
   
  /**
   * Affects the behaviour of "^" and "$" tags. When switched off:
   * <li> the "^" matches the beginning of the whole text;
   * <li> the "$" matches the end of the whole text, or just before the '\n' or "\r\n" at the end of text.
   * When switched on:
   * <li> the "^" additionally matches the line beginnings (that is just after the '\n');
   * <li> the "$" additionally matches the line ends (that is just before "\r\n" or '\n');
   * Corresponds to "m" in Perl notation.
   */
   public int MULTILINE=1<<1;
   
  /**
   * Affects the behaviour of dot(".") tag. When switched off:
   * <li> the dot matches any character but EOLs('\r','\n');
   * When switched on:
   * <li> the dot matches any character, including EOLs.
   * This flag is sometimes referenced in regex tutorials as SINGLELINE, which confusingly seems opposite to MULTILINE, but in fact is orthogonal.
   * Corresponds to "s" in Perl notation.
   */
   public int DOTALL=1<<2;
   
  /**
   * Affects how the space characters are interpeted in the expression. When switched off:
   * <li> the spaces are interpreted literally;
   * When switched on:
   * <li> the spaces are ingnored, allowing an expression to be slightly more readable.
   * Corresponds to "x" in Perl notation.
   */
   public int IGNORE_SPACES=1<<3;
   
  /**
   * Affects whether the predefined classes("\d","\s","\w",etc) in the expression are interpreted as belonging to Unicode. When switched off:
   * <li> the predefined classes are interpreted as ASCII;
   * When switched on:
   * <li> the predefined classes are interpreted as Unicode categories;
   */
   public int UNICODE=1<<4;
   
  /**
   * Turns on the compatibility with XML Schema regular expressions.
   */
   public int XML_SCHEMA=1<<5;
}