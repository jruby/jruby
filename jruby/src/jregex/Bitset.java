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

class Bitset implements UnicodeConstants{
   private static final Block[][] categoryBits=new Block[CATEGORY_COUNT][BLOCK_COUNT];
   static{
      for(int i=Character.MIN_VALUE;i<=Character.MAX_VALUE;i++){
         int cat=Character.getType((char)i);
         int blockNo=(i>>8)&0xff;
         Block b=categoryBits[cat][blockNo];
         if(b==null) categoryBits[cat][blockNo]=b=new Block();
//if(i>32 && i<127)System.out.println((char)i+" -> ["+cat+"]["+blockNo+"].("+i+")");
         b.set(i&0xff);
      }
   }
   
   private boolean positive=true;
   private boolean isLarge=false;
   
   boolean[] block0;  //1-byte bit set
   private static final boolean[] emptyBlock0=new boolean[BLOCK_SIZE];
   
   Block[] blocks;  //2-byte bit set
   
   private int weight;
   
   final void reset(){
      positive=true;
      block0=null;
      blocks=null;
      isLarge=false;
      weight=0;
   }
   
   final static void unify(Bitset bs,Term term){
      if(bs.isLarge){
         term.type=Term.BITSET2;
         term.bitset2=Block.toBitset2(bs.blocks);
      }
      else{
         term.type=Term.BITSET;
         term.bitset=bs.block0==null? emptyBlock0: bs.block0;
      }
      term.inverse=!bs.positive;
      term.weight=bs.positive? bs.weight: MAX_WEIGHT-bs.weight;
   }

   final void setPositive(boolean b){
      positive=b;
   }
   
   final boolean isPositive(){
      return positive;
   }
   
   final boolean isLarge(){
      return isLarge;
   }
   
   private final void enableLargeMode(){
      if(isLarge) return;
      Block[] blocks=new Block[BLOCK_COUNT];
      this.blocks=blocks;
      if(block0!=null){
         blocks[0]=new Block(block0);
      }
      isLarge=true;
   }
   
   final int getWeight(){
      return positive? weight: MAX_WEIGHT-weight;
   }
   
   final void setWordChar(boolean unicode){
      if(unicode){
         setCategory(Lu);
         setCategory(Ll);
         setCategory(Lt);
         setCategory(Lo);
         setCategory(Nd);
         setChar('_');
      }
      else{
         setRange('a','z');
         setRange('A','Z');
         setRange('0','9');
         setChar('_');
      }
   }
   
   final void setDigit(boolean unicode){
      if(unicode){
         setCategory(Nd);
      }
      else{
         setRange('0','9');
      }
   }
   
   final void setSpace(boolean unicode){
      if(unicode){
         setCategory(Zs);
         setCategory(Zp);
         setCategory(Zl);
      }
      else{
         setChar(' ');
         setChar('\r');
         setChar('\n');
         setChar('\t');
         setChar('\f');
      }
   }
   
   final void setCategory(int c){
      if(!isLarge) enableLargeMode();
      Block[] catBits=categoryBits[c];
      weight+=Block.add(this.blocks,catBits,0,BLOCK_COUNT-1,false);
//System.out.println("["+this+"].setCategory("+c+"): weight="+weight);
   }
   
   final void setChars(String chars){
      for(int i=chars.length()-1;i>=0;i--) setChar(chars.charAt(i));
   }
   
   final void setChar(char c){
      setRange(c,c);
   }
   
   final void setRange(char c1,char c2){
//System.out.println("["+this+"].setRange("+c1+","+c2+"):");
//if(c1>31 && c1<=126 && c2>31 && c2<=126) System.out.println("setRange('"+c1+"','"+c2+"'):");
//else System.out.println("setRange(["+Integer.toHexString(c1)+"],["+Integer.toHexString(c2)+"]):");
      if(c2>=256 || isLarge){
         int s=0;
         if(!isLarge){
            enableLargeMode();
         }
         Block[] blocks=this.blocks;
         for(int c=c1;c<=c2;c++){
            int i2=(c>>8)&0xff;
            int i=c&0xff;
            Block block=blocks[i2];
            if(block==null){
               blocks[i2]=block=new Block();
            }
            if(block.set(i))s++;
         }
         weight+=s;
      }
      else{
         boolean[] block0=this.block0;
         if(block0==null){
            this.block0=block0=new boolean[BLOCK_SIZE];
         }
         weight+=set(block0,true,c1,c2);
      }
   }
   
   final void add(Bitset bs){
      add(bs,false);
   }
   
   final void add(Bitset bs,boolean inverse){
      weight+=addImpl(this,bs,!bs.positive^inverse);
   }
   
   private final static int addImpl(Bitset bs1, Bitset bs2, boolean inv){
      int s=0;
      if(!bs1.isLarge && !bs2.isLarge && !inv){
         if(bs2.block0!=null){
            boolean[] bits=bs1.block0;
            if(bits==null) bs1.block0=bits=new boolean[BLOCK_SIZE];
            s+=add(bits,bs2.block0,0,BLOCK_SIZE-1,false);
         }
      }
      else {
         if(!bs1.isLarge) bs1.enableLargeMode();
         if(!bs2.isLarge) bs2.enableLargeMode();
         s+=Block.add(bs1.blocks,bs2.blocks,0,BLOCK_COUNT-1,inv);
      }
      return s;
   }
   
   final void subtract(Bitset bs){
      subtract(bs,false);
   }
   
   final void subtract(Bitset bs,boolean inverse){
//System.out.println("["+this+"].subtract(["+bs+"],"+inverse+"):");
      weight+=subtractImpl(this,bs,!bs.positive^inverse);
   }
   
   private final static int subtractImpl(Bitset bs1,Bitset bs2,boolean inv){
      int s=0;
      if(!bs1.isLarge && !bs2.isLarge && !inv){
         boolean[] bits1,bits2;
         if((bits2=bs2.block0)!=null){
            bits1=bs1.block0;
            if(bits1==null) return 0;
            s+=subtract(bits1,bits2,0,BLOCK_SIZE-1,false);
         }
      }
      else {
         if(!bs1.isLarge) bs1.enableLargeMode();
         if(!bs2.isLarge) bs2.enableLargeMode();
         s+=Block.subtract(bs1.blocks,bs2.blocks,0,BLOCK_COUNT-1,inv);
      }
      return s;
   }
   
   final void intersect(Bitset bs){
      intersect(bs,false);
   }
   
   final void intersect(Bitset bs,boolean inverse){
//System.out.println("["+this+"].intersect(["+bs+"],"+inverse+"):");
      subtract(bs,!inverse);
   }
   
   static final int add(boolean[] bs1,boolean[] bs2,int from,int to,boolean inv){
//System.out.println("Bitset.add(boolean[],boolean[],"+inv+"):");
      int s=0;
      for(int i=from;i<=to;i++){
         if(bs1[i]) continue;
         if(!(bs2[i]^inv)) continue;
//System.out.println("        "+i+": value0="+value0+", value="+value);
         s++;
         bs1[i]=true;
//System.out.println("             s="+s+", bs1[i]->"+bs1[i]);
      }
      return s;
   }
   
   static final int subtract(boolean[] bs1,boolean[] bs2,int from,int to,boolean inv){
//System.out.println("Bitset.subtract(boolean[],boolean[],"+inv+"):");
      int s=0;
      for(int i=from;i<=to;i++){
         if(!bs1[i]) continue;
         if(!(bs2[i]^inv)) continue;
         s--;
         bs1[i]=false;
//if(i>32 && i<127) System.out.println("             s="+s+", bs1['"+(char)i+"']->"+bs1[i]);
//else System.out.println("             s="+s+", bs1["+i+"]->"+bs1[i]);
      }
      return s;
   }
   
   static final int set(boolean[] arr,boolean value,int from,int to){
      int s=0;
      for(int i=from;i<=to;i++){
         if(arr[i]==value) continue;
         if(value) s++; else s--;
         arr[i]=value;
      }
      return s;
   }
   
   public String toString(){
      StringBuffer sb=new StringBuffer();
      if(!positive) sb.append('^');
      
      if(isLarge) sb.append(CharacterClass.stringValue2(Block.toBitset2(blocks)));
      else if(block0!=null) sb.append(CharacterClass.stringValue0(block0));
      
      sb.append('(');
      sb.append(getWeight());
      sb.append(')');
      return sb.toString();
   }
   
   /*
   public static void main(String[] args){
      //System.out.print("blocks(Lu)=");
      //System.out.println(CharacterClass.stringValue2(Block.toBitset2(categoryBits[Lu])));
      //System.out.println("[1][0].get('a')="+categoryBits[1][0].get('a'));
      //System.out.println("[1][0].get('A')="+categoryBits[1][0].get('A'));
      //System.out.println("[1][0].get(65)="+categoryBits[1][0].get(65));
      //System.out.println(""+categoryBits[1][0].get('A'));
      Bitset b1=new Bitset();
      //b1.setCategory(Lu);
      //b1.enableLargeMode();
      b1.setRange('a','z');
      b1.setRange('à','ÿ');
      
      Bitset b2=new Bitset();
      //b2.setCategory(Ll);
      //b2.enableLargeMode();
      b2.setRange('A','Z');
      b2.setRange('À','ß');
      
      Bitset b=new Bitset();
      //bs.setRange('a','z');
      //bs.setRange('A','Z');
      b.add(b1);
      b.add(b2,true);
      
      System.out.println("b1="+b1);
      System.out.println("b2="+b2);
      System.out.println("b=b1+^b2="+b);
      
      b.subtract(b1,true);
      
      System.out.println("(b1+^b2)-^b1="+b);
      
   }
   */
}

class Block implements UnicodeConstants{
   private boolean isFull;
   //private boolean[] bits;
   boolean[] bits;
   private boolean shared=false;
   
   Block(){}
   
   Block(boolean[] bits){
      this.bits=bits;
      shared=true;
   }
   
   final boolean set(int c){
//System.out.println("Block.add("+CharacterClass.stringValue2(toBitset2(targets))+","+CharacterClass.stringValue2(toBitset2(addends))+","+from*BLOCK_SIZE+","+to*BLOCK_SIZE+","+inv+"):");
      if(isFull) return false;
      boolean[] bits=this.bits;
      if(bits==null){
         this.bits=bits=new boolean[BLOCK_SIZE];
         shared=false;
         bits[c]=true;
         return true;
      }
      
      if(bits[c]) return false;
      
      if(shared) bits=copyBits(this);
      
      bits[c]=true;
      return true;
   }
   
   final boolean get(int c){
      if(isFull) return true;
      boolean[] bits=this.bits;
      if(bits==null){
         return false;
      }
      return bits[c];
   }
   
   final static int add(Block[] targets,Block[] addends,int from,int to,boolean inv){
//System.out.println("Block.add("+CharacterClass.stringValue2(toBitset2(targets))+","+CharacterClass.stringValue2(toBitset2(addends))+","+from*BLOCK_SIZE+","+to*BLOCK_SIZE+","+inv+"):");
//System.out.println("Block.add():");
      int s=0;
      for(int i=from;i<=to;i++){
         Block addend=addends[i];
//System.out.println("   "+i+": ");
//System.out.println("     target="+(target==null? "null": i==0? CharacterClass.stringValue0(target.bits): "{"+count(target.bits,0,BLOCK_SIZE-1)+"}"));
//System.out.println("     addend="+(addend==null? "null": i==0? CharacterClass.stringValue0(addend.bits): "{"+count(addend.bits,0,BLOCK_SIZE-1)+"}"));
         if(addend==null){ 
            if(!inv) continue;
         }
         else if(addend.isFull && inv) continue;
         
         Block target=targets[i];
         if(target==null) targets[i]=target=new Block();
         else if(target.isFull) continue;
         
         s+=add(target,addend,inv);
//System.out.println("     result="+(target==null? "null": i==0? CharacterClass.stringValue0(target.bits): "{"+count(target.bits,0,BLOCK_SIZE-1)+"}"));
//System.out.println("     s="+s);
      }
//System.out.println("   s="+s);
      return s;
   }
   
   private final static int add(Block target,Block addend,boolean inv){
//System.out.println("Block.add(Block,Block):");
      //there is provided that !target.isFull
      boolean[] targetbits,addbits;
      if(addend==null){
         if(!inv) return 0;
         int s=BLOCK_SIZE;
         if((targetbits=target.bits)!=null){
            s-=count(targetbits,0,BLOCK_SIZE-1);
         }
         target.isFull=true;
         target.bits=null;
         target.shared=false;
         return s;
      }
      else if(addend.isFull){
         if(inv) return 0;
         int s=BLOCK_SIZE;
         if((targetbits=target.bits)!=null){
            s-=count(targetbits,0,BLOCK_SIZE-1);
         }
         target.isFull=true;
         target.bits=null;
         target.shared=false;
         return s;
      }
      else if((addbits=addend.bits)==null){
         if(!inv) return 0;
         int s=BLOCK_SIZE;
         if((targetbits=target.bits)!=null){
            s-=count(targetbits,0,BLOCK_SIZE-1);
         }
         target.isFull=true;
         target.bits=null;
         target.shared=false;
         return s;
      }
      else{
         if((targetbits=target.bits)==null){
            if(!inv){
               target.bits=addbits;
               target.shared=true;
               return count(addbits,0,BLOCK_SIZE-1);
            }
            else{
               target.bits=targetbits=emptyBits(null);
               target.shared=false;
               return Bitset.add(targetbits,addbits,0,BLOCK_SIZE-1,inv);
            }
         }
         else{
            if(target.shared) targetbits=copyBits(target);
            return Bitset.add(targetbits,addbits,0,BLOCK_SIZE-1,inv);
         }
      }
   }
   
   final static int subtract(Block[] targets,Block[] subtrahends,int from,int to,boolean inv){
//System.out.println("Block.subtract(Block[],Block[],"+inv+"):");
      int s=0;
      for(int i=from;i<=to;i++){
//System.out.println("   "+i+": ");
         
         Block target=targets[i];
         if(target==null || (!target.isFull && target.bits==null)) continue;
//System.out.println("     target="+(target==null? "null": i==0? CharacterClass.stringValue0(target.bits): "{"+ (target.isFull? BLOCK_SIZE: count(target.bits,0,BLOCK_SIZE-1))+"}"));
         
         Block subtrahend=subtrahends[i];
//System.out.println("     subtrahend="+(subtrahend==null? "null": i==0? CharacterClass.stringValue0(subtrahend.bits): "{"+(subtrahend.isFull? BLOCK_SIZE: count(subtrahend.bits,0,BLOCK_SIZE-1))+"}"));
         
         if(subtrahend==null){
            if(!inv) continue;
            else{
               if(target.isFull){
                  s-=BLOCK_SIZE;
               }
               else{
                  s-=count(target.bits,0,BLOCK_SIZE-1);
               }
               target.isFull=false;
               target.bits=null;
               target.shared=false;
            }
         }
         else{
            s+=subtract(target,subtrahend,inv);
         }
//System.out.println("     result="+(target==null? "null": i==0? CharacterClass.stringValue0(target.bits): "{"+ (target.isFull? BLOCK_SIZE: target.bits==null? 0: count(target.bits,0,BLOCK_SIZE-1))+"}"));
//System.out.println("     s="+s);
      }
//System.out.println("   s="+s);
      return s;
   }
   
   private final static int subtract(Block target,Block subtrahend,boolean inv){
      boolean[] targetbits,subbits;
//System.out.println("subtract(Block,Block,"+inv+")");
      //there is provided that target.isFull or target.bits!=null
      if(subtrahend.isFull){
         if(inv) return 0;
         int s=0;
         if(target.isFull){
            s=BLOCK_SIZE;
         }
         else{
            s=count(target.bits,0,BLOCK_SIZE-1);
         }
         target.isFull=false;
         target.bits=null;
         target.shared=false;
         return s;
      }
      else if((subbits=subtrahend.bits)==null){
         if(!inv) return 0;
         int s=0;
         if(target.isFull){
            s=BLOCK_SIZE;
         }
         else{
            s=count(target.bits,0,BLOCK_SIZE-1);
         }
         target.isFull=false;
         target.bits=null;
         target.shared=false;
         return s;
      }
      else{
         if(target.isFull){
            boolean[] bits=fullBits(target.bits);
            int s=Bitset.subtract(bits,subbits,0,BLOCK_SIZE-1,inv);
            target.isFull=false;
            target.shared=false;
            target.bits=bits;
            return s;
         }
         else{
            if(target.shared) targetbits=copyBits(target);
            else targetbits=target.bits;
            return Bitset.subtract(targetbits,subbits,0,BLOCK_SIZE-1,inv);
         }
      }
   }
   
   private static boolean[] copyBits(Block block){
      boolean[] bits=new boolean[BLOCK_SIZE];
      System.arraycopy(block.bits,0,bits,0,BLOCK_SIZE);
      block.bits=bits;
      block.shared=false;
      return bits;
   }
   
   private static boolean[] fullBits(boolean[] bits){
      if(bits==null) bits=new boolean[BLOCK_SIZE];
      System.arraycopy(FULL_BITS,0,bits,0,BLOCK_SIZE);
      return bits;
   }
   
   private static boolean[] emptyBits(boolean[] bits){
      if(bits==null) bits=new boolean[BLOCK_SIZE];
      else System.arraycopy(EMPTY_BITS,0,bits,0,BLOCK_SIZE);
      return bits;
   }
   
   final static int count(boolean[] arr, int from, int to){
      int s=0;
      for(int i=from;i<=to;i++){
         if(arr[i]) s++;
      }
      return s;
   }
   
   final static boolean[][] toBitset2(Block[] blocks){
      int len=blocks.length;
      boolean[][] result=new boolean[len][];
      for(int i=0;i<len;i++){
         Block block=blocks[i];
         if(block==null) continue;
         if(block.isFull){
            result[i]=FULL_BITS;
         }
         else result[i]=block.bits;
      }
      return result;
   }
   
   private final static boolean[] EMPTY_BITS=new boolean[BLOCK_SIZE];
   private final static boolean[] FULL_BITS=new boolean[BLOCK_SIZE];
   static{
      for(int i=0;i<BLOCK_SIZE;i++) FULL_BITS[i]=true;
   }
}