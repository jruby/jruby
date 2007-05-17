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
import java.util.Vector;

/**
 * An implementation of the Substitution interface. Performs substitutions in accordance with Perl-like substitution scripts.<br>
 * The latter is a string, containing a mix of memory register references and plain text blocks.<br>
 * It may look like "some_chars $1 some_chars$2some_chars" or "123${1}45${2}67".<br>
 * A tag consisting of '$',not preceeded by the escape character'\' and  followed by some digits (possibly enclosed in the curled brackets) is interpreted as a memory register reference, the digits forming a register ID.
 * All the rest is considered as a plain text.<br>
 * Upon the Replacer has found a text block that matches the pattern, a references in a replacement string are replaced by the contents of 
 * corresponding memory registers, and the resulting text replaces the matched block.<br>
 * For example, the following code:
 * <pre>
 * System.out.println("\""+
 *    new Replacer(new Pattern("\\b(\\d+)\\b"),new PerlSubstitution("'$1'")).replace("abc 123 def")
 *    +"\"");
 * </pre>
 * will print <code>"abc '123' def"</code>.<br>
 * @see        Substitution
 * @see        Replacer
 * @see        Pattern
 */


public class PerlSubstitution implements Substitution{
   //private static Pattern refPtn,argsPtn;
   private static Pattern refPtn;
   private static int NAME_ID;
   private static int ESC_ID;
   //private static int FN_NAME_ID;
   //private static int FN_ARGS_ID;
   //private static int ARG_NAME_ID;
   
   private static final String groupRef="\\$(?:\\{({=name}\\w+)\\}|({=name}\\d+|&))|\\\\({esc}.)";
   //private static final String fnRef="\\&({fn_name}\\w+)\\(({fn_args}"+groupRef+"(?:,"+groupRef+")*)*\\)";
   
   static{
      try{
         //refPtn=new Pattern("(?<!\\\\)"+fnRef+"|"+groupRef);
         //argsPtn=new Pattern(groupRef);
         //refPtn=new Pattern("(?<!\\\\)"+groupRef);
         refPtn=new Pattern(groupRef);
         NAME_ID=refPtn.groupId("name").intValue();
         ESC_ID=refPtn.groupId("esc").intValue();
         //ARG_NAME_ID=argsPtn.groupId("name").intValue();
         //FN_NAME_ID=refPtn.groupId("fn_name").intValue();
         //FN_ARGS_ID=refPtn.groupId("fn_args").intValue();
      }
      catch(PatternSyntaxException e){
         e.printStackTrace();
      }
   }
   
   private Element queueEntry;
   
   //It seems we should somehow throw an IllegalArgumentException if an expression 
   //holds a reference to a non-existing group. Such checking will require a Pattern instance.
   public PerlSubstitution(String s){
      Matcher refMatcher=new Matcher(refPtn);
      refMatcher.setTarget(s);
      queueEntry=makeQueue(refMatcher);
   }
   
   public String value(MatchResult mr){
      TextBuffer dest=Replacer.wrap(new StringBuffer(mr.length()));
      appendSubstitution(mr,dest);
      return dest.toString();
   }
   
   private static Element makeQueue(Matcher refMatcher){
      if(refMatcher.find()){
         Element element;
         if(refMatcher.isCaptured(NAME_ID)){
            char c=refMatcher.charAt(0,NAME_ID);
            if(c=='&'){
               element=new IntRefHandler(refMatcher.prefix(),new Integer(0));
            }
            else if(Character.isDigit(c)){
               element=new IntRefHandler(refMatcher.prefix(),new Integer(refMatcher.group(NAME_ID)));
            }
            else 
               element=new StringRefHandler(refMatcher.prefix(),refMatcher.group(NAME_ID));
         }
         else{
            //escaped char
            element=new PlainElement(refMatcher.prefix(),refMatcher.group(ESC_ID));
         }
         refMatcher.setTarget(refMatcher,MatchResult.SUFFIX);
         element.next=makeQueue(refMatcher);
         return element;
      }
      else return new PlainElement(refMatcher.target());
   }
   
   public void appendSubstitution(MatchResult match,TextBuffer dest){
      for(Element element=this.queueEntry; element!=null; element=element.next){
         element.append(match,dest);
      }
   }
   
   public String toString(){
      StringBuffer sb=new StringBuffer();
      for(Element element=this.queueEntry;element!=null;element=element.next){
         sb.append(element.toString());
      }
      return sb.toString();
   }
   
   private static abstract class Element{
      protected String prefix;
      Element next;
      abstract void append(MatchResult match,TextBuffer dest);
   }
   
   private static class PlainElement extends Element{
      private String str;
      PlainElement(String s){
         str=s;
      }
      PlainElement(String pref,String s){
         prefix=pref;
         str=s;
      }
      void append(MatchResult match,TextBuffer dest){
         if(prefix!=null)dest.append(prefix);
         if(str!=null)dest.append(str);
      }
   }
   
   private static class IntRefHandler extends Element{
      private Integer index;
      IntRefHandler(String s,Integer ind){
         prefix=s;
         index=ind;
      }
      void append(MatchResult match,TextBuffer dest){
         if(prefix!=null) dest.append(prefix);
         if(index==null) return;
         int i=index.intValue();
         if(i>=match.pattern().groupCount()) return;
         if(match.isCaptured(i))match.getGroup(i,dest);
      }
   }
   
   private static class StringRefHandler extends Element{
      private String index;
      StringRefHandler(String s,String ind){
         prefix=s;
         index=ind;
      }
      void append(MatchResult match,TextBuffer dest){
         if(prefix!=null) dest.append(prefix);
         if(index==null) return;
         Integer id=match.pattern().groupId(index);
         //if(id==null) return; //???
         int i=id.intValue();
         if(match.isCaptured(i))match.getGroup(i,dest);
      }
   }
}

abstract class GReference{
   public abstract String stringValue(MatchResult match);
   
   public static GReference createInstance(MatchResult match,int grp){
      if(match.length(grp)==0) throw new IllegalArgumentException("arg name cannot be an empty string");
      if(Character.isDigit(match.charAt(0,grp))){
         try{
            return new IntReference(Integer.parseInt(match.group(grp)));
         }
         catch(NumberFormatException e){
            throw new IllegalArgumentException("illegal arg name, starts with digit but is not a number");
         }
      }
      return new StringReference((match.group(grp)));
   }
}

class IntReference extends GReference{
   protected int id;
   
   IntReference(int id){
      this.id=id;
   }
   
   public String stringValue(MatchResult match){
      return match.group(id);
   }
}

class StringReference extends GReference{
   protected String name;
   
   StringReference(String name){
      this.name=name;
   }
   
   public String stringValue(MatchResult match){
      return match.group(name);
   }
}