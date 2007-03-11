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

package jregex.util.io;

import jregex.*;
import java.io.FilenameFilter;
import java.io.File;
import java.util.Enumeration;
import java.util.Stack;
import java.util.NoSuchElementException;

abstract class PathElementMask{
   protected PathElementMask next;
   protected boolean dirsOnly;
   
   protected PathElementMask(boolean dirsOnly){
      this.dirsOnly=dirsOnly;
   }
   
   public abstract Enumeration elements(final File dir);
   
   PathElementEnumerator newEnumerator(){
      return new PathElementEnumerator(this);
   }
   
   static PathElementMask regularMask(String s,int flags,boolean dirsOnly){
      return new RegularMask(s,flags,dirsOnly);
   }
   
   static PathElementMask fixedMask(String s,boolean dirsOnly){
      return new FixedPathElement(s,dirsOnly);
   }
   
   static PathElementMask anyFile(boolean dirsOnly){
      return new AnyFile(dirsOnly);
   }
   
   static PathElementMask anyPath(boolean dirsOnly){
      return new AnyPath(dirsOnly);
   }
}

class RegularMask extends PathElementMask{
   Pattern pattern;
   
   RegularMask(String s,int flags,boolean dirsOnly){
      super(dirsOnly);
//System.out.println("PathElementMask("+s+","+dirsOnly+"):");
      pattern=new WildcardPattern(s,flags);
   }
   
   class MatchingElementEnumerator extends PathElementEnumerator{
      private RegularMask rmask;
      private Matcher matcher;
      MatchingElementEnumerator(RegularMask rm){
         super(rm);
         rmask=rm;
         matcher=rm.pattern.matcher();
      }
      
      protected void setDir(File f){
         entries=rmask.elements(f,matcher);
      }
   }
   
   PathElementEnumerator newEnumerator(){
      return new MatchingElementEnumerator(this);
   }
   
   public Enumeration elements(File dir){
      throw new Error();
   }
   
   public Enumeration elements(File dir,final Matcher matcher){
      if(dir==null) throw new IllegalArgumentException();
//System.out.println("PathElementMask.elements("+dir+"{"+dir.getName()+","+dir.getAbsolutePath()+"}, mask=\""+matcher.pattern()+")\"");
      return new ListEnumerator(dir, new ListEnumerator.Instantiator(){
         public File instantiate(File dir,String name){
//System.out.println("  next name:"+name);
            //if(matcher!=null && !matcher.matches(name)) return null;
            if(!matcher.matches(name)) return null;
//System.out.println("    mask ok");
            File f=new File(dir,name);
            if(dirsOnly && !f.isDirectory()) return null;
            return f;
         }
      });
   }
}

class FixedPathElement extends PathElementMask{
   private String[] list;
   
   FixedPathElement(String s,boolean dirsOnly){
      super(dirsOnly);
//System.out.println("FixedPathElement("+s+","+dirsOnly+"):");
      //windows: paths like "d:" don't work as expected, "d:/" is ok
      list=new String[]{dirsOnly? s+File.separator: s};
   }
   
   public Enumeration elements(File dir){
//System.out.println("FixedPathElement.elements("+dir+"), mask=\""+name+"\"");
      if(dir==null) throw new IllegalArgumentException();
      return new ListEnumerator(dir,list,new ListEnumerator.Instantiator(){
         public File instantiate(File dir,String name){
            File f= dir.getName().equals(".")? new File(name): new File(dir,name);
            if(!f.exists() || (dirsOnly && !f.isDirectory())) return null;
            return f;
         }
      });
   }
}

class AnyFile extends PathElementMask{
   AnyFile(boolean dirsOnly){
      super(dirsOnly);
//System.out.println("AnyFile("+dirsOnly+"):");
   }
   
   public Enumeration elements(File dir){
//System.out.println("AnyFile.elements("+dir+")");
      if(dir==null) throw new IllegalArgumentException();
      return new ListEnumerator(dir,new ListEnumerator.Instantiator(){
         public File instantiate(File dir,String name){
            File f=new File(dir,name);
            if(dirsOnly && !f.isDirectory()) return null;
            return f;
         }
      });
   }
}

class AnyPath extends PathElementMask{
   private ListEnumerator.Instantiator inst=new ListEnumerator.Instantiator(){
      public File instantiate(File dir,String name){
         if(dir==null || name==null) throw new IllegalArgumentException();
         File f=new File(dir,name);
         if(dirsOnly && !f.isDirectory()) return null;
         return f;
      }
   };
   
   AnyPath(boolean dirsOnly){
      super(dirsOnly);
//System.out.println("AnyFile("+dirsOnly+"):");
   }
   
   public Enumeration elements(final File dir){
//System.out.println("AnyFile.elements("+dir+")");
      if(dir==null) throw new IllegalArgumentException();
      final Stack stack=new Stack();
      stack.push(dir);
      return new Enumerator(){
         { 
            currObj=dir;
         }
         private Enumeration currEn;
         protected boolean find(){
            while(currEn==null || !currEn.hasMoreElements()){
               if(stack.size()==0){
                  return false;
               }
               currEn=new ListEnumerator((File)stack.pop(),inst);
            }
            currObj=currEn.nextElement();
            if(((File)currObj).isDirectory()) stack.push(currObj);
            return true;
         }
      };
   }
}