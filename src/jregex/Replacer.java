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

import java.io.*;
import java.util.Hashtable;

/**
 * <b>The Replacer class</b> suggests some methods to replace occurences of a pattern 
 * either by a result of evaluation of a perl-like expression, or by a plain string,
 * or according to a custom substitution model, provided as a Substitution interface implementation.<br>
 * A Replacer instance may be obtained either using Pattern.replacer(...) method, or by constructor:<pre>
 * Pattern p=new Pattern("\\w+");
 * Replacer perlExpressionReplacer=p.replacer("[$&]");
 * //or another way to do the same
 * Substitution myOwnModel=new Substitution(){
 *    public void appendSubstitution(MatchResult match,TextBuffer tb){
 *       tb.append('[');
 *       match.getGroup(MatchResult.MATCH,tb);
 *       tb.append(']');
 *    }
 * }
 * Replacer myVeryOwnReplacer=new Replacer(p,myOwnModel);
 * </pre>
 * The second method is much more verbose, but gives more freedom.
 * To perform a replacement call replace(someInput):<pre>
 * System.out.print(perlExpressionReplacer.replace("All your base "));
 * System.out.println(myVeryOwnReplacer.replace("are belong to us"));
 * //result: "[All] [your] [base] [are] [belong] [to] [us]"
 * </pre>
 * @see        Substitution
 * @see        PerlSubstitution
 * @see        Replacer#Replacer(jregex.Pattern,jregex.Substitution)
 */

public class Replacer{
   private Pattern pattern;
   private Substitution substitution;
   
  /**
   */
   public Replacer(Pattern pattern,Substitution substitution){
      this.pattern=pattern;
      this.substitution=substitution;
   }
   
  /**
   */
   public Replacer(Pattern pattern, String substitution){
      this(pattern,substitution,true);
   }
   
  /**
   */
   public Replacer(Pattern pattern, String substitution, boolean isPerlExpr){
      this.pattern=pattern;
      this.substitution= isPerlExpr? (Substitution)new PerlSubstitution(substitution): 
                               new DummySubstitution(substitution);
   }
   
   public void setSubstitution(String s, boolean isPerlExpr){
      substitution= isPerlExpr? (Substitution)new PerlSubstitution(s): 
                               new DummySubstitution(s);
   }
   
  /**
   */
   public String replace(String text){
      TextBuffer tb=wrap(new StringBuffer(text.length()));
      replace(pattern.matcher(text),substitution,tb);
      return tb.toString();
   }
   
  /**
   */
   public String replace(char[] chars,int off,int len){
      TextBuffer tb=wrap(new StringBuffer(len));
      replace(pattern.matcher(chars,off,len),substitution,tb);
      return tb.toString();
   }
   
  /**
   */
   public String replace(MatchResult res,int group){
      TextBuffer tb=wrap(new StringBuffer());
      replace(pattern.matcher(res,group),substitution,tb);
      return tb.toString();
   }
   
  /**
   */
   public String replace(Reader text,int length)throws IOException{
      TextBuffer tb=wrap(new StringBuffer(length>=0? length: 0));
      replace(pattern.matcher(text,length),substitution,tb);
      return tb.toString();
   }
   
  /**
   */
   public int replace(String text,StringBuffer sb){
      return replace(pattern.matcher(text),substitution,wrap(sb));
   }
   
  /**
   */
   public int replace(char[] chars,int off,int len,StringBuffer sb){
      return replace(chars,off,len,wrap(sb));
   }
   
  /**
   */
   public int replace(MatchResult res,int group,StringBuffer sb){
      return replace(res,group,wrap(sb));
   }
   
  /**
   */
   public int replace(MatchResult res,String groupName,StringBuffer sb){
      return replace(res,groupName,wrap(sb));
   }
   
   public int replace(Reader text,int length,StringBuffer sb)throws IOException{
      return replace(text,length,wrap(sb));
   }
   
  /**
   */
   public int replace(String text,TextBuffer dest){
      return replace(pattern.matcher(text),substitution,dest);
   }
   
  /**
   */
   public int replace(char[] chars,int off,int len,TextBuffer dest){
      return replace(pattern.matcher(chars,off,len),substitution,dest);
   }
   
  /**
   */
   public int replace(MatchResult res,int group,TextBuffer dest){
      return replace(pattern.matcher(res,group),substitution,dest);
   }
   
  /**
   */
   public int replace(MatchResult res,String groupName,TextBuffer dest){
      return replace(pattern.matcher(res,groupName),substitution,dest);
   }
   
   public int replace(Reader text,int length,TextBuffer dest)throws IOException{
      return replace(pattern.matcher(text,length),substitution,dest);
   }
   
  /**
   * Replaces all occurences of a matcher's pattern in a matcher's target
   * by a given substitution appending the result to a buffer.<br>
   * The substitution starts from current matcher's position, current match
   * not included.
   */
   public static int replace(Matcher m,Substitution substitution,TextBuffer dest){
      boolean firstPass=true;
      int c=0;
      while(m.find()){
         if(m.end()==0 && !firstPass) continue;  //allow to replace at "^"
         if(m.start()>0) m.getGroup(MatchResult.PREFIX,dest);
         substitution.appendSubstitution(m,dest);
         c++;
         m.setTarget(m,MatchResult.SUFFIX);
         firstPass=false;
      }
      m.getGroup(MatchResult.TARGET,dest);
      return c;
   }
   
   public static int replace(Matcher m,Substitution substitution,Writer out) throws IOException{
      try{
         return replace(m,substitution,wrap(out));
      }
      catch(WriteException e){
         throw e.reason;
      }
   }
   
  /**
   */
   public void replace(String text,Writer out) throws IOException{
      replace(pattern.matcher(text),substitution,out);
   }
   
  /**
   */
   public void replace(char[] chars,int off,int len,Writer out) throws IOException{
      replace(pattern.matcher(chars,off,len),substitution,out);
   }
   
  /**
   */
   public void replace(MatchResult res,int group,Writer out) throws IOException{
      replace(pattern.matcher(res,group),substitution,out);
   }
   
  /**
   */
   public void replace(MatchResult res,String groupName,Writer out) throws IOException{
      replace(pattern.matcher(res,groupName),substitution,out);
   }
   
   public void replace(Reader in,int length,Writer out)throws IOException{
      replace(pattern.matcher(in,length),substitution,out);
   }
   
   private static class DummySubstitution implements Substitution{
      String str;
      DummySubstitution(String s){
         str=s;
      }
      public void appendSubstitution(MatchResult match,TextBuffer res){
         if(str!=null) res.append(str);
      }
   }
   
   public static TextBuffer wrap(final StringBuffer sb){
      return new TextBuffer(){
         public void append(char c){
            sb.append(c);
         }
         public void append(char[] chars,int start,int len){
            sb.append(chars,start,len);
         }
         public void append(String s){
            sb.append(s);
         }
         public String toString(){
            return sb.toString();
         }
      };
   }
   
   public static TextBuffer wrap(final Writer writer){
      return new TextBuffer(){
         public void append(char c){
            try{
               writer.write(c);
            }
            catch(IOException e){
               throw new WriteException(e);
            }
         }
         public void append(char[] chars,int off,int len){
            try{
               writer.write(chars,off,len);
            }
            catch(IOException e){
               throw new WriteException(e);
            }
         }
         public void append(String s){
            try{
               writer.write(s);
            }
            catch(IOException e){
               throw new WriteException(e);
            }
         }
      };
   }
   
   private static class WriteException extends RuntimeException{
      IOException reason;
      WriteException(IOException io){
         reason=io;
      }
   }
}