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

import java.util.*;

public class Optimizer{
   public static final int THRESHOLD=20;
   
   static Optimizer find(Term entry){
      return find(entry,0);
   }
   
   private static Optimizer find(Term term,int dist){
//System.out.println("term="+term+", dist="+dist);
      if(term==null) return null;
      Term next=term.next;
      int type=term.type;
      switch(type){
         case Term.CHAR:
         case Term.REG:
         case Term.REG_I:
            return new Optimizer(term,dist);
         case Term.BITSET:
         case Term.BITSET2:
            if(term.weight<=THRESHOLD) return new Optimizer(term,dist);
            else return find(term.next,dist+1);
         case Term.ANY_CHAR:
         case Term.ANY_CHAR_NE:
            return find(next,dist+1);
         case Term.REPEAT_MIN_INF:
         case Term.REPEAT_MIN_MAX:
            if(term.minCount>0){
               return find(term.target,dist);
            }
            else return null;
      }
      if(type>=Term.FIRST_TRANSPARENT && type<=Term.LAST_TRANSPARENT){
         return find(next,dist);
      }
      return null;
   }
   
   private Term atom;
   private int distance;
   
   private Optimizer(Term atom,int distance){
      this.atom=atom;
      this.distance=distance;
   }
   
   Term makeFirst(Term theFirst){
      return new Find(atom,distance,theFirst);
   }
   
   Term makeBacktrack(Term back){
      int min=back.minCount;
      switch(back.type){
         case Term.BACKTRACK_0:
            min=0;
         case Term.BACKTRACK_MIN:
            return new FindBack(atom,distance,min,back);
         
         case Term.BACKTRACK_REG_MIN:
            return back;
         
         default:
            throw new Error("unexpected iterator's backtracker:"+ back);
            //return back;
      }
   }
}

class Find extends Term{
   Find(Term target, int distance, Term theFirst){
      switch(target.type){
         case Term.CHAR:
         case Term.BITSET:
         case Term.BITSET2:
            type=Term.FIND;
            break;
         case Term.REG:
         case Term.REG_I:
            type=Term.FINDREG;
            break;
         default:
            throw new IllegalArgumentException("wrong target type: "+target.type);
      }
      this.target=target;
      this.distance=distance;
      if(target==theFirst){
         next=target.next;
         eat=true; //eat the next
      }
      else{
         next=theFirst;
         eat=false;
      }
   }
}

class FindBack extends Term{
   FindBack(Term target, int distance, int minCount, Term backtrack){
      this.minCount=minCount;
      switch(target.type){
         case Term.CHAR:
         case Term.BITSET:
         case Term.BITSET2:
            type=Term.BACKTRACK_FIND_MIN;
            break;
         case Term.REG:
         case Term.REG_I:
            type=Term.BACKTRACK_FINDREG_MIN;
            break;
         default:
            throw new IllegalArgumentException("wrong target type: "+target.type);
      }
      
      this.target=target;
      this.distance=distance;
      Term next=backtrack.next;
      if(target==next){
         this.next=next.next;
         this.eat=true;
      }
      else{
         this.next=next;
         this.eat=false;
      }
   }
}