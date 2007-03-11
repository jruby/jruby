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

/**
 * A Pattern subclass that accepts a simplified pattern syntax:
 * <li><code>?<code> - matches any single character;
 * <li><code>*<code> - matches any number of any characters;
 * <li>all the rest      - matches itself.
 * Each wildcard takes a capturing group withing a pattern.
 * 
 * @see        Pattern
 */

public class WildcardPattern extends Pattern{
   //a wildcard class, see WildcardPattern(String,String,int)
   public static final String WORD_CHAR="\\w";
   
   //a wildcard class, see WildcardPattern(String,String,int)
   public static final String ANY_CHAR=".";
   
   private static final String defaultSpecials="[]().{}+|^$\\";
   private static final String defaultWcClass=ANY_CHAR;
   protected static String convertSpecials(String s,String wcClass,String specials){
      int len=s.length();
      StringBuffer sb=new StringBuffer();
      for(int i=0;i<len;i++){
         char c=s.charAt(i);
         switch(c){
            case '*':
               sb.append("(");
               sb.append(wcClass);
               sb.append("*)");
               break;
            case '?':
               sb.append("(");
               sb.append(wcClass);
               sb.append(")");
               break;
            default:
               if(specials.indexOf(c)>=0) sb.append('\\');
               sb.append(c);
         }
      }
      return sb.toString();
   }
   
   private String str;
   
  /**
   * @param  wc    The pattern
   */
   public WildcardPattern(String wc){
      this(wc,true);
   }
   
  /**
   * @param  wc    The pattern
   * @param  icase If true, the pattern is case-insensitive.
   */
   public WildcardPattern(String wc,boolean icase){
      this(wc,icase? DEFAULT|IGNORE_CASE: DEFAULT);
   }
   
  /**
   * @param  wc    The pattern
   * @param  flags The bitwise OR of any of REFlags.* . The only meaningful
   * flags are REFlags.IGNORE_CASE and REFlags.DOTALL (the latter allows 
   * the wildcards to match the EOL characters).
   */
   public WildcardPattern(String wc,int flags){
      compile(wc,defaultWcClass,defaultSpecials,flags);
   }
   
  /**
   * @param  wc       The pattern
   * @param  wcClass  The wildcard class, could be any of WORD_CHAR or ANY_CHAR
   * @param  flags    The bitwise OR of any of REFlags.* . The only meaningful
   * flags are REFlags.IGNORE_CASE and REFlags.DOTALL (the latter allows 
   * the wildcards to match the EOL characters).
   */
   public WildcardPattern(String wc,String wcClass,int flags){
      compile(wc,wcClass,defaultSpecials,flags);
   }
   
   protected WildcardPattern(){}
   
   protected void compile(String wc,String wcClass,String specials,int flags){
      String converted=convertSpecials(wc,wcClass,specials);
      try{
         compile(converted,flags);
      }
      catch(PatternSyntaxException e){
         //something unexpected
         throw new Error(e.getMessage()+"; original expr: "+wc+", converted: "+converted);
      }
      str=wc;
   }
   
   public String toString(){
      return str;
   }
   
   /*
   public static void main(String[] args){
      Pattern p=new WildcardPattern("*.???");
      Matcher m=p.matcher("abc.def");
      //System.out.println(p.toString_d());
      while(m.proceed()){
         System.out.println(m);
         System.out.println("groups: "+m.groupv());
      }
   }
   */
}