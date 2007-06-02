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
import java.util.NoSuchElementException;

public class ListEnumerator extends Enumerator{
   public interface Instantiator{
      public File instantiate(File dir,String name);
   }
   
   public static final Instantiator defaultInstantiator=new Instantiator(){
      public File instantiate(File dir,String name){
         File f= dir==null? new File(name): new File(dir,name);
         //return f.exists()? f: null;
         return f;
      }
   };
   
   private String[] list;
   private File dir;
   private Instantiator instantiator;
   private int index=0;
   
   public ListEnumerator(File dir, Instantiator i){
      this(dir,dir.list(),i);
   }
   
   public ListEnumerator(File dir, String[] names, Instantiator i){
      if(names==null) throw new IllegalArgumentException();
      if(i==null) throw new IllegalArgumentException();
      this.dir=dir;
      this.list=names;
      this.instantiator=i;
   }
   
   protected boolean find(){
//System.out.println(" PathElementMask.elements().find():");
      File dir=this.dir;
      Instantiator instantiator=this.instantiator;
      while(index<list.length){
         String name=list[index++];
         File f= instantiator.instantiate(dir,name);
         if(f==null) continue;
         currObj=f;
         return true;
      }
      return false;
   }
}