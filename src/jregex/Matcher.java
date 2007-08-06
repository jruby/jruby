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
import java.io.*;

/**
 * Matcher instance is an automaton that actually performs matching. It provides the following methods:
 * <li> searching for a matching substrings : matcher.find() or matcher.findAll();
 * <li> testing whether a text matches a whole pattern : matcher.matches();
 * <li> testing whether the text matches the beginning of a pattern : matcher.matchesPrefix();
 * <li> searching with custom options : matcher.find(int options)
 * <p>
 * <b>Obtaining results</b><br>
 * After the search succeded, i.e. if one of above methods returned <code>true</code>
 * one may obtain an information on the match:
 * <li> may check whether some group is captured : matcher.isCaptured(int);
 * <li> may obtain start and end positions of the match and its length : matcher.start(int),matcher.end(int),matcher.length(int);
 * <li> may obtain match contents as String : matcher.group(int).<br>
 * The same way can be obtained the match prefix and suffix information.
 * The appropriate methods are grouped in MatchResult interface, which the Matcher class implements.<br>
 * Matcher objects are not thread-safe, so only one thread may use a matcher instance at a time.
 * Note, that Pattern objects are thread-safe(the same instanse may be shared between
 * multiple threads), and the typical tactics in multithreaded applications is to have one Pattern instance per expression(a singleton),
 * and one Matcher object per thread.
 */

public class Matcher implements MatchResult{
  /* Matching options*/
  /**
   * The same effect as "^" without REFlags.MULTILINE.
   * @see Matcher#find(int)
   */
   public static final int ANCHOR_START=1;
   
  /**
   * The same effect as "\\G".
   * @see Matcher#find(int)
   */
   public static final int ANCHOR_LASTMATCH=2;
   
  /**
   * The same effect as "$" without REFlags.MULTILINE.
   * @see Matcher#find(int)
   */
   public static final int ANCHOR_END=4;
   
  /**
   * Experimental option; if a text ends up before the end of a pattern,report a match.
   * @see Matcher#find(int)
   */
   public static final int ACCEPT_INCOMPLETE=8;
   
   //see search(ANCHOR_START|...)
   private static Term startAnchor=new Term(Term.START);
   
   //see search(ANCHOR_LASTMATCH|...)
   private static Term lastMatchAnchor=new Term(Term.LAST_MATCH_END);
   
   private Pattern re;
   private int[] counters;
   private MemReg[] memregs;
   private LAEntry[] lookaheads;
   private int counterCount;
   private int memregCount;
   private int lookaheadCount;
   
   private char[] data;
   private int offset,end,wOffset,wEnd;
   private boolean shared;
   
   private SearchEntry top;           //stack entry
   private SearchEntry first;         //object pool entry
   private SearchEntry defaultEntry;  //called when moving the window
   
   private boolean called;
   
   private int minQueueLength;
   
   private String cache;
   
   //cache may be longer than the actual data
   //and contrariwise; so cacheOffset may have both signs.
   //cacheOffset is actually -(data offset).
   private int cacheOffset,cacheLength;   
   
   private MemReg prefixBounds,suffixBounds,targetBounds;
   
   Matcher(Pattern regex){
      this.re=regex;
      //int memregCount=(memregs=new MemReg[regex.memregs]).length;
      //for(int i=0;i<memregCount;i++){
      //   this.memregs[i]=new MemReg(-1); //unlikely to SearchEntry, in this case we know memreg indicies by definition
      //}
      //counters=new int[regex.counters];
      //int lookaheadCount=(lookaheads=new LAEntry[regex.lookaheads]).length;
      //for(int i=0;i<lookaheadCount;i++){
      //   this.lookaheads[i]=new LAEntry();
      //}
      
      int memregCount,counterCount,lookaheadCount;
      if((memregCount=regex.memregs)>0){
         MemReg[] memregs=new MemReg[memregCount];
         for(int i=0;i<memregCount;i++){
            memregs[i]=new MemReg(-1); //unlikely to SearchEntry, in this case we know memreg indicies by definition
         }
         this.memregs=memregs;
      }
      
      if((counterCount=regex.counters)>0) counters=new int[counterCount];
      
      if((lookaheadCount=regex.lookaheads)>0){
         LAEntry[] lookaheads=new LAEntry[lookaheadCount];
         for(int i=0;i<lookaheadCount;i++){
            lookaheads[i]=new LAEntry();
         }
         this.lookaheads=lookaheads;
      }
      
      this.memregCount=memregCount;
      this.counterCount=counterCount;
      this.lookaheadCount=lookaheadCount;
      
      first=new SearchEntry();
      defaultEntry=new SearchEntry();
      minQueueLength=regex.stringRepr.length()/2;  // just evaluation!!!
   }
   
  /**
   * This method allows to efficiently pass data between matchers.
   * Note that a matcher may pass data to itself:<pre>
   *   Matcher m=new Pattern("\\w+").matcher(myString);
   *   if(m.find())m.setTarget(m,m.SUFFIX); //forget all that is not a suffix
   * </pre>
   * Resets current search position to zero.
   * @param m  - a matcher that is a source of data
   * @param groupId - which group to take data from
   * @see Matcher#setTarget(java.lang.String)
   * @see Matcher#setTarget(java.lang.String,int,int)
   * @see Matcher#setTarget(char[],int,int)
   * @see Matcher#setTarget(java.io.Reader,int)
   */
   public final void setTarget(Matcher m, int groupId){
      MemReg mr=m.bounds(groupId);
//System.out.println("setTarget("+m+","+groupId+")");
//System.out.println("   in="+mr.in);
//System.out.println("   out="+mr.out);
      if(mr==null) throw new IllegalArgumentException("group #"+groupId+" is not assigned");
      data=m.data;
      offset=mr.in;
      end=mr.out;
      cache=m.cache;
      cacheLength=m.cacheLength;
      cacheOffset=m.cacheOffset;
      if(m!=this){
         shared=true;
         m.shared=true;
      }
      init();
   }
   
   
  /**
   * Supplies a text to search in/match with.
   * Resets current search position to zero.
   * @param text - a data
   * @see Matcher#setTarget(jregex.Matcher,int)
   * @see Matcher#setTarget(java.lang.String,int,int)
   * @see Matcher#setTarget(char[],int,int)
   * @see Matcher#setTarget(java.io.Reader,int)
   */
   public void setTarget(String text){
      setTarget(text,0,text.length());
   }
   
  /**
   * Supplies a text to search in/match with, as a part of String.
   * Resets current search position to zero.
   * @param text - a data source
   * @param start - where the target starts
   * @param len - how long is the target
   * @see Matcher#setTarget(jregex.Matcher,int)
   * @see Matcher#setTarget(java.lang.String)
   * @see Matcher#setTarget(char[],int,int)
   * @see Matcher#setTarget(java.io.Reader,int)
   */
   public void setTarget(String text,int start,int len){
      char[] mychars=data;
      if(mychars==null || shared || mychars.length<len){
         data=mychars=new char[(int)(1.7f*len)];
         shared=false;
      }
      text.getChars(start,len,mychars,0); //(srcBegin,srcEnd,dst[],dstBegin)
      offset=0;
      end=len;
      
      cache=text;
      cacheOffset=-start;
      cacheLength=text.length();
      
      init();
   }
   
  /**
   * Supplies a text to search in/match with, as a part of char array.
   * Resets current search position to zero.
   * @param text - a data source
   * @param start - where the target starts
   * @param len - how long is the target
   * @see Matcher#setTarget(jregex.Matcher,int)
   * @see Matcher#setTarget(java.lang.String)
   * @see Matcher#setTarget(java.lang.String,int,int)
   * @see Matcher#setTarget(java.io.Reader,int)
   */
   public void setTarget(char[] text,int start,int len){
      setTarget(text,start,len,true);
   }
   
  /**
   * To be used with much care.
   * Supplies a text to search in/match with, as a part of a char array, as above, but also allows to permit
   * to use the array as internal buffer for subsequent inputs. That is, if we call it with <code>shared=false</code>:<pre>
   *   myMatcher.setTarget(myCharArray,x,y,<b>false</b>); //we declare that array contents is NEITHER shared NOR will be used later, so may modifications on it are permitted
   * </pre>
   * then we should expect the array contents to be changed on subsequent setTarget(..) operations.
   * Such method may yield some increase in perfomanse in the case of multiple setTarget() calls.
   * Resets current search position to zero.
   * @param text - a data source
   * @param start - where the target starts
   * @param len - how long is the target
   * @param shared - if <code>true<code>: data are shared or used later, <b>don't</b> modify it; if <code>false<code>: possible modifications of the text on subsequent <code>setTarget()</code> calls are perceived and allowed.
   * @see Matcher#setTarget(jregex.Matcher,int)
   * @see Matcher#setTarget(java.lang.String)
   * @see Matcher#setTarget(java.lang.String,int,int)
   * @see Matcher#setTarget(char[],int,int)
   * @see Matcher#setTarget(java.io.Reader,int)
   */
   public final void setTarget(char[] text,int start,int len,boolean shared){
      cache=null;
      data=text;
      offset=start;
      end=start+len;
      this.shared=shared;
      init();
   }
   
   
  /**
   * Supplies a text to search in/match with through a stream.
   * Resets current search position to zero.
   * @param in - a data stream;
   * @param len - how much characters should be read; if len is -1, read the entire stream.
   * @see Matcher#setTarget(jregex.Matcher,int)
   * @see Matcher#setTarget(java.lang.String)
   * @see Matcher#setTarget(java.lang.String,int,int)
   * @see Matcher#setTarget(char[],int,int)
   */
   public void setTarget(Reader in,int len)throws IOException{
      if(len<0){
         setAll(in);
         return;
      }
      char[] mychars=data;
      boolean shared=this.shared;
      if(mychars==null || shared || mychars.length<len){
         mychars=new char[len];
         shared=false;
      }
      int count=0;
      int c;
      while((c=in.read(mychars,count,len))>=0){
         len-=c;
         count+=c;
         if(len==0) break;
      }
      setTarget(mychars,0,count,shared);
   }
   
   private void setAll(Reader in)throws IOException{
      char[] mychars=data;
      int free;
      boolean shared=this.shared;
      if(mychars==null || shared){
         mychars=new char[free=1024];
         shared=false;
      }
      else free=mychars.length;
      int count=0;
      int c;
      while((c=in.read(mychars,count,free))>=0){
         free-=c;
         count+=c;
         if(free==0){
            int newsize=count*3;
            char[] newchars=new char[newsize];
            System.arraycopy(mychars,0,newchars,0,count);
            mychars=newchars;
            free=newsize-count;
            shared=false;
         }
      }
      setTarget(mychars,0,count,shared);
   }
   
   private final String getString(int start,int end){
      String src=cache;
      if(src!=null){
         int co=cacheOffset;
         return src.substring(start-co,end-co);
      }
      int tOffset,tEnd,tLen=(tEnd=this.end)-(tOffset=this.offset);
      char[] data=this.data;
      if((end-start)>=(tLen/3)){
         //it makes sence to make a cache
         cache=src=new String(data,tOffset,tLen);
         cacheOffset=tOffset;
         cacheLength=tLen;
         return src.substring(start-tOffset,end-tOffset);
      }
      return new String(data,start,end-start);
   }
   
  /* Matching */
   
  /**
   * Tells whether the entire target matches the beginning of the pattern.
   * The whole pattern is also regarded as its beginning.<br>
   * This feature allows to find a mismatch by examining only a beginning part of 
   * the target (as if the beginning of the target doesn't match the beginning of the pattern, then the entire target 
   * also couldn't match).<br>
   * For example the following assertions yield <code>true<code>:<pre>
   *   Pattern p=new Pattern("abcd"); 
   *   p.matcher("").matchesPrefix();
   *   p.matcher("a").matchesPrefix();
   *   p.matcher("ab").matchesPrefix();
   *   p.matcher("abc").matchesPrefix();
   *   p.matcher("abcd").matchesPrefix();
   * </pre>
   * and the following yield <code>false<code>:<pre>
   *   p.matcher("b").isPrefix();
   *   p.matcher("abcdef").isPrefix();
   *   p.matcher("x").isPrefix();
   * </pre>
   * @return true if the entire target matches the beginning of the pattern
   */
   public final boolean matchesPrefix(){
      setPosition(0);
      return search(ANCHOR_START|ACCEPT_INCOMPLETE|ANCHOR_END);
   }
   
  /**
   * Just an old name for isPrefix().<br>
   * Retained for backwards compatibility.
   * @deprecated Replaced by isPrefix()
   */
   public final boolean isStart(){
      return matchesPrefix();
   }
   
  /**
   * Tells whether a current target matches the whole pattern.
   * For example the following yields the <code>true<code>:<pre>
   *   Pattern p=new Pattern("\\w+"); 
   *   p.matcher("a").matches();
   *   p.matcher("ab").matches();
   *   p.matcher("abc").matches();
   * </pre>
   * and the following yields the <code>false<code>:<pre>
   *   p.matcher("abc def").matches();
   *   p.matcher("bcd ").matches();
   *   p.matcher(" bcd").matches();
   *   p.matcher("#xyz#").matches();
   * </pre>
   * @return whether a current target matches the whole pattern.
   */
   public final boolean matches(){
if(called) setPosition(0);
      return search(ANCHOR_START|ANCHOR_END);
   }
   
  /**
   * Just a combination of setTarget(String) and matches().
   * @param s the target string;
   * @return whether the specified string matches the whole pattern.
   */
   public final boolean matches(String s){
      setTarget(s);
      return search(ANCHOR_START|ANCHOR_END);
   }
   
  /**
   * Allows to set a position the subsequent find()/find(int) will start from.
   * @param pos the position to start from;
   * @see Matcher#find()
   * @see Matcher#find(int)
   */
   public void setPosition(int pos){
      wOffset=offset+pos;
      wEnd=-1;
      called=false;
      flush();
   }

   public void setOffset(int offset){
       this.offset = offset;
       wOffset=offset;
       wEnd=-1;
       called=false;
       flush();
   }
   
  /**
   * Searches through a target for a matching substring, starting from just after the end of last match.
   * If there wasn't any search performed, starts from zero.
   * @return <code>true</code> if a match found.
   */
   public final boolean find(){
      if(called) skip();
      return search(0);
   }
   
  /**
   * Searches through a target for a matching substring, starting from just after the end of last match.
   * If there wasn't any search performed, starts from zero.
   * @param anchors a zero or a combination(bitwise OR) of ANCHOR_START,ANCHOR_END,ANCHOR_LASTMATCH,ACCEPT_INCOMPLETE
   * @return <code>true</code> if a match found.
   */
   public final boolean find(int anchors){
      if(called) skip();
      return search(anchors);
   }
   
   
  /**
   * The same as  findAll(int), but with default behaviour;
   */
   public MatchIterator findAll(){
      return findAll(0);
   }
   
  /**
   * Returns an iterator over the matches found by subsequently calling find(options), the search starts from the zero position.
   */
   public MatchIterator findAll(final int options){
      //setPosition(0);
      return new MatchIterator(){
         private boolean checked=false;
         private boolean hasMore=false;
         public boolean hasMore(){
            if(!checked) check();
            return hasMore;
         }
         public MatchResult nextMatch(){
            if(!checked) check();
            if(!hasMore) throw new NoSuchElementException();
            checked=false;
            return Matcher.this;
         }
         private final void check(){
            hasMore=find(options);
            checked=true;
         }
         public int count(){
            if(!checked) check();
            if(!hasMore) return 0;
            int c=1;
            while(find(options))c++;
            checked=false;
            return c;
         }
      };
   }
   
  /**
   * Continues to search from where the last search left off.
   * The same as proceed(0).
   * @see Matcher#proceed(int)
   */
   public final boolean proceed(){
      return proceed(0);
   }
   
  /**
   * Continues to search from where the last search left off using specified options:<pre>
   * Matcher m=new Pattern("\\w+").matcher("abc");
   * while(m.proceed(0)){
   *    System.out.println(m.group(0));
   * }
   * </pre>
   * Output:<pre>
   * abc
   * ab
   * a
   * bc
   * b
   * c
   * </pre>
   * For example, let's find all odd nubmers occuring in a text:<pre>
   *    Matcher m=new Pattern("\\d+").matcher("123");
   *    while(m.proceed(0)){
   *       String match=m.group(0);
   *       if(isOdd(Integer.parseInt(match))) System.out.println(match);
   *    }
   *    
   *    static boolean isOdd(int i){
   *       return (i&1)>0;
   *    }
   * </pre>
   * This outputs:<pre>
   * 123
   * 1
   * 23
   * 3
   * </pre>
   * Note that using <code>find()</code> method we would find '123' only.
   * @param options search options, some of ANCHOR_START|ANCHOR_END|ANCHOR_LASTMATCH|ACCEPT_INCOMPLETE; zero value(default) stands for usual search for substring.
   */
   public final boolean proceed(int options){
//System.out.println("next() : top="+top);
      if(called){
         if(top==null){
            wOffset++;
         }
      }
      return search(0);
   }
   
  /**
   * Sets the current search position just after the end of last match.
   */
   public final void skip(){
      int we=wEnd;
      if(wOffset==we){ //requires special handling
         //if no variants at 'wOutside',advance pointer and clear
         if(top==null){ 
            wOffset++;
            flush();
         }
         //otherwise, if there exist a variant, 
         //don't clear(), i.e. allow it to match
         return;
      }
      else{
         if(we<0) wOffset=0;
         else wOffset=we;
      }
      //rflush(); //rflush() works faster on simple regexes (with a small group/branch number)
      flush();
   }
   
   private final void init(){
      //wOffset=-1;
//System.out.println("init(): offset="+offset+", end="+end);
      wOffset=offset;
      wEnd=-1;
      called=false;
      flush();
   }
   
  /**
   * Resets the internal state.
   */
   private final void flush(){
      top=null;
      defaultEntry.reset(0);
      
/*
int c=0;
SearchEntry se=first;
while(se!=null){
   c++;
   se=se.on;
}
System.out.println("queue: allocated="+c+", truncating to "+minQueueLength);
new Exception().printStackTrace();
*/
      
      first.reset(minQueueLength);
      //first.reset(0);
      for(int i=memregs.length-1;i>0;i--){
         MemReg mr=memregs[i];
         mr.in=mr.out=-1;
      }
      for(int i=memregs.length-1;i>0;i--){
         MemReg mr=memregs[i];
         mr.in=mr.out=-1;
      }
      called=false;
   }
   
   //reverse flush
   //may work significantly faster,
   //need testing
   private final void rflush(){
      SearchEntry entry=top;
      top=null;
      MemReg[] memregs=this.memregs;
      int[] counters=this.counters;
      while(entry!=null){
         SearchEntry next=entry.sub;
         SearchEntry.popState(entry,memregs,counters);
         entry=next;
      }
      SearchEntry.popState(defaultEntry,memregs,counters);
   }
   
  /**
   */
   public String toString(){
      return getString(wOffset,wEnd);
   }
   
   public Pattern pattern(){
      return re;
   }
   
   public String target(){
      return getString(offset,end);
   }
   
  /**
   */
   public char[] targetChars(){
      shared=true;
      return data;
   }
   
  /**
   */
   public int targetStart(){
      return offset;
   }
   
  /**
   */
   public int targetEnd(){
      return end;
   }
   
   public char charAt(int i){
      int in=this.wOffset;
      int out=this.wEnd;
      if(in<0 || out<in) throw new IllegalStateException("unassigned");
      return data[in+i];
   }
   
   public char charAt(int i,int groupId){
      MemReg mr=bounds(groupId);
      if(mr==null) throw new IllegalStateException("group #"+groupId+" is not assigned");
      int in=mr.in;
      if(i<0 || i>(mr.out-in)) throw new StringIndexOutOfBoundsException(""+i);
      return data[in+i];
   }
   
   public final int length(){
      return wEnd-wOffset;
   }
   
  /**
   */
   public final int start(){
      return wOffset-offset;
   }
   
  /**
   */
   public final int end(){
      return wEnd-offset;
   }
   
  /**
   */
   public String prefix(){
      return getString(offset,wOffset);
   }
   
  /**
   */
   public String suffix(){
      return getString(wEnd,end);
   }
   
  /**
   */
   public int groupCount(){
      return memregs.length;
   }
   
  /**
   */
   public String group(int n){
      MemReg mr=bounds(n);
      if(mr==null) return null;
      return getString(mr.in,mr.out);
   }
   
  /**
   */
   public String group(String name){
      Integer id=re.groupId(name);
      if(id==null) throw new IllegalArgumentException("<"+name+"> isn't defined");
      return group(id.intValue());
   }
   
  /**
   */
   public boolean getGroup(int n,TextBuffer tb){
      MemReg mr=bounds(n);
      if(mr==null) return false;
      int in;
      tb.append(data,in=mr.in,mr.out-in);
      return true;
   }
   
  /**
   */
   public boolean getGroup(String name,TextBuffer tb){
      Integer id=re.groupId(name);
      if(id==null) throw new IllegalArgumentException("unknown group: \""+name+"\"");
      return getGroup(id.intValue(),tb);
   }
   
  /**
   */
   public boolean getGroup(int n,StringBuffer sb){
      MemReg mr=bounds(n);
      if(mr==null) return false;
      int in;
      sb.append(data,in=mr.in,mr.out-in);
      return true;
   }
   
  /**
   */
   public boolean getGroup(String name,StringBuffer sb){
      Integer id=re.groupId(name);
      if(id==null) throw new IllegalArgumentException("unknown group: \""+name+"\"");
      return getGroup(id.intValue(),sb);
   }
   
  /**
   */
   public String[] groups(){
      MemReg[] memregs=this.memregs;
      String[] groups=new String[memregs.length];
      int in,out;
      MemReg mr;
      for(int i=0;i<memregs.length;i++){
         in=(mr=memregs[i]).in;
         out=mr.out;
         if((in=mr.in)<0 || mr.out<in) continue;
         groups[i]=getString(in,out);
      }
      return groups;
   }
   
  /**
   */
   public Vector groupv(){
      MemReg[] memregs=this.memregs;
      Vector v=new Vector();
      int in,out;
      MemReg mr;
      for(int i=0;i<memregs.length;i++){
         mr=bounds(i);
         if(mr==null){
            v.addElement("empty");
            continue;
         }
         String s=getString(mr.in,mr.out);
         v.addElement(s);
      }
      return v;
   }
   
   private final MemReg bounds(int id){
//System.out.println("Matcher.bounds("+id+"):");
      MemReg mr;
      if(id>=0){
         mr=memregs[id];
      }
      else switch(id){
         case PREFIX:
            mr=prefixBounds;
            if(mr==null) prefixBounds=mr=new MemReg(PREFIX);
            mr.in=offset;
            mr.out=wOffset;
            break;
         case SUFFIX:
            mr=suffixBounds;
            if(mr==null) suffixBounds=mr=new MemReg(SUFFIX);
            mr.in=wEnd;
            mr.out=end;
            break;
         case TARGET:
            mr=targetBounds;
            if(mr==null) targetBounds=mr=new MemReg(TARGET);
            mr.in=offset;
            mr.out=end;
            break;
         default:
            throw new IllegalArgumentException("illegal group id: "+id+"; must either nonnegative int, or MatchResult.PREFIX, or MatchResult.SUFFIX");
      }
//System.out.println("  mr=["+mr.in+","+mr.out+"]");
      int in;
      if((in=mr.in)<0 || mr.out<in) return null;
      return mr;
   }
   
  /**
   */
   public final boolean isCaptured(){
      return wOffset>=0 && wEnd>=wOffset;
   }
   
  /**
   */
   public final boolean isCaptured(int id){
      return bounds(id)!=null;
   }
   
  /**
   */
   public final boolean isCaptured(String groupName){
      Integer id=re.groupId(groupName);
      if(id==null) throw new IllegalArgumentException("unknown group: \""+groupName+"\"");
      return isCaptured(id.intValue());
   }
   
  /**
   */
   public final int length(int id){
      MemReg mr=bounds(id);
      return mr.out-mr.in;
   }
   
  /**
   */
   public final int start(int id){
      return bounds(id).in-offset;
   }
   
  /**
   */
   public final int end(int id){
      return bounds(id).out-offset;
   }
   
   private final boolean search(int anchors){
      called=true;
      final int end=this.end;
      int offset=this.offset;
      char[] data=this.data;
      int wOffset=this.wOffset;
      int wEnd=this.wEnd;
      
      MemReg[] memregs=this.memregs;
      int[] counters=this.counters;
      LAEntry[] lookaheads=this.lookaheads;
      
      //int memregCount=memregs.length;
      //int cntCount=counters.length;
      int memregCount=this.memregCount;
      int cntCount=this.counterCount;
      
      SearchEntry defaultEntry=this.defaultEntry;
      SearchEntry first=this.first;
      SearchEntry top=this.top;
      SearchEntry actual=null;
      int cnt,regLen;
      int i;
      
      final boolean matchEnd=(anchors&ANCHOR_END)>0;
      final boolean allowIncomplete=(anchors&ACCEPT_INCOMPLETE)>0;
      
      Pattern re=this.re;
      Term root=re.root;
      Term term;
      if(top==null){
         if((anchors&ANCHOR_START)>0){
            term=re.root0;  //raw root
            root=startAnchor;
         }
         else if((anchors&ANCHOR_LASTMATCH)>0){
            term=re.root0;  //raw root
            root=lastMatchAnchor;
         }
         else{
            term=root;  //optimized root
         }
         i=wOffset;
         actual=first;
         SearchEntry.popState(defaultEntry,memregs,counters);
      }
      else{
         top=(actual=top).sub;
         term=actual.term;
         i=actual.index;
         SearchEntry.popState(actual,memregs,counters);
      }
      cnt=actual.cnt;
      regLen=actual.regLen;
      
      main:
      while(wOffset<=end){
         matchHere:
         for(;;){
     /*
     System.out.print("char: "+i+", term: ");
     System.out.print(term.toString());

     System.out.print(" // mrs:{");
     for(int dbi=0;dbi<memregs.length;dbi++){
        System.out.print('[');
        System.out.print(memregs[dbi].in);
        System.out.print(',');
        System.out.print(memregs[dbi].out);
        System.out.print(']');
        System.out.print(' ');
     }
     System.out.print("}, crs:{");
     for(int dbi=0;dbi<counters.length;dbi++){
        System.out.print(counters[dbi]);
        if(dbi<counters.length-1)System.out.print(',');
     }
     System.out.println("}");
     */
            int memreg,cntreg;
            char c;
            switch(term.type){
               case Term.FIND:{
                  int jump=find(data,i+term.distance,end,term.target); //don't eat the last match
                  if(jump<0) break main; //return false
                  i+=jump;
                  wOffset=i; //force window to move
                  if(term.eat){
                     if(i==end) break;
                     i++;
                  }
                  term=term.next;
                  continue matchHere;
               }
               case Term.FINDREG:{
                  MemReg mr=memregs[term.target.memreg];
                  int sampleOff=mr.in;
                  int sampleLen=mr.out-sampleOff;
                  //if(sampleOff<0 || sampleLen<0) throw new Error("backreference used before definition: \\"+term.memreg);
                  /*@since 1.2*/
                  if(sampleOff<0 || sampleLen<0){
                     break;
                  }
                  else if(sampleLen==0){
                     term=term.next;
                     continue matchHere;
                  }
                  int jump=findReg(data,i+term.distance,sampleOff,sampleLen,term.target,end); //don't eat the last match
                  if(jump<0) break main; //return false
                  i+=jump;
                  wOffset=i; //force window to move
                  if(term.eat){
                     i+=sampleLen;
                     if(i>end) break;
                  }
                  term=term.next;
                  continue matchHere;
               }
               case Term.VOID:
                  term=term.next;
                  continue matchHere;
               
               case Term.CHAR:
                  //can only be 1-char-wide
                  //  \/
                  if(i>=end || data[i]!=term.c) break;
//System.out.println("CHAR: "+data[i]+", i="+i);
                  i++;
                  term=term.next;
                  continue matchHere;
               
               case Term.ANY_CHAR:
                  //can only be 1-char-wide
                  //  \/
                  if(i>=end) break;
                  i++;
                  term=term.next;
                  continue matchHere;
               
               case Term.ANY_CHAR_NE:
                  //can only be 1-char-wide
                  //  \/
                  if(i>=end || data[i]=='\n') break;
                  i++;
                  term=term.next;
                  continue matchHere;
               
               case Term.END:
                  if(i>=end){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  break; 
                  
               case Term.END_EOL:  //perl's $
                  if(i>=end){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  else{
                     boolean matches=
                        i>=end |
                        ((i+1)==end && data[i]=='\n');
                        
                     if(matches){
                        term=term.next;
                        continue matchHere;
                     }
                     else break; 
                  }
                  
               case Term.LINE_END:
                  if(i>=end){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  else{
                     /*
                     if(((c=data[i])=='\r' || c=='\n') &&
                           (c=data[i-1])!='\r' && c!='\n'){
                        term=term.next;
                        continue matchHere;
                     }
                     */
                     //5 aug 2001
                     if(data[i]=='\n'){
                        term=term.next;
                        continue matchHere;
                     }
                  }
                  break; 
                  
               case Term.START: //Perl's "^"
                  if(i==offset){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  //break; 
                  
                  //changed on 27-04-2002
                  //due to a side effect: if ALLOW_INCOMPLETE is enabled,
                  //the anchorStart moves up to the end and succeeds 
                  //(see comments at the last lines of matchHere, ~line 1830)
                  //Solution: if there are some entries on the stack ("^a|b$"),
                  //try them; otherwise it's a final 'no'
                  //if(top!=null) break;
                  //else break main;
                  
                  //changed on 25-05-2002
                  //rationale: if the term is startAnchor, 
                  //it's the root term by definition, 
                  //so if it doesn't match, the entire pattern 
                  //couldn't match too;
                  //otherwise we could have the following problem: 
                  //"c|^a" against "abc" finds only "a"
                  if(top!=null) break;
                  if(term!=startAnchor) break;
                  else break main;
                  
               case Term.LAST_MATCH_END:
                  if(i==wEnd || wEnd == -1){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  break main; //return false
                  
               case Term.LINE_START:
                  if(i==offset){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  else if(i<end){
                     /*
                     if(((c=data[i-1])=='\r' || c=='\n') &&
                           (c=data[i])!='\r' && c!='\n'){
                        term=term.next;
                        continue matchHere;
                     }
                     */
                     //5 aug 2001
                     //if((c=data[i-1])=='\r' || c=='\n'){ ??
                     if((c=data[i-1])=='\n'){
                        term=term.next;
                        continue matchHere;
                     }
                  }
                  break; 
                  
               case Term.BITSET:{
                  //can only be 1-char-wide
                  //  \/
                  if(i>=end) break;
                  c=data[i];
                  if(!(c<=255 && term.bitset[c])^term.inverse) break;
                  i++;
                  term=term.next;
                  continue matchHere;
               }
               case Term.BITSET2:{
                  //can only be 1-char-wide
                  //  \/
                  if(i>=end) break;
                  c=data[i];
                  boolean[] arr=term.bitset2[c>>8];
                  if(arr==null || !arr[c&255]^term.inverse) break;
                  i++;
                  term=term.next;
                  continue matchHere;
               }
               case Term.BOUNDARY:{
                  boolean ch1Meets=false,ch2Meets=false;
                  boolean[] bitset=term.bitset;
                  test1:{
                     int j=i-1;
                     //if(j<offset || j>=end) break test1;
                     if(j<offset) break test1;
                     c= data[j];
                     ch1Meets= (c<256 && bitset[c]);
                  }
                  test2:{
                     //if(i<offset || i>=end) break test2;
                     if(i>=end) break test2;
                     c= data[i];
                     ch2Meets= (c<256 && bitset[c]);
                  }
                  if(ch1Meets^ch2Meets^term.inverse){  //meets
                     term=term.next;
                     continue matchHere;
                  }
                  else break;
               }
               case Term.UBOUNDARY:{
                  boolean ch1Meets=false,ch2Meets=false;
                  boolean[][] bitset2=term.bitset2;
                  test1:{
                     int j=i-1;
                     //if(j<offset || j>=end) break test1;
                     if(j<offset) break test1;
                     c= data[j];
                     boolean[] bits=bitset2[c>>8];
                     ch1Meets= bits!=null && bits[c&0xff];
                  }
                  test2:{
                     //if(i<offset || i>=end) break test2;
                     if(i>=end) break test2;
                     c= data[i];
                     boolean[] bits=bitset2[c>>8];
                     ch2Meets= bits!=null && bits[c&0xff];
                  }
                  if(ch1Meets^ch2Meets^term.inverse){  //is boundary ^ inv
                     term=term.next;
                     continue matchHere;
                  }
                  else break;
               }
               case Term.DIRECTION:{
                  boolean ch1Meets=false,ch2Meets=false;
                  boolean[] bitset=term.bitset;
                  boolean inv=term.inverse;
//System.out.println("i="+i+", inv="+inv+", bitset="+CharacterClass.stringValue0(bitset));
                  int j=i-1;
                  //if(j>=offset && j<end){
                  if(j>=offset){
                     c= data[j];
                     ch1Meets= c<256 && bitset[c];
//System.out.println("    ch1Meets="+ch1Meets);
                  }
                  if(ch1Meets^inv) break;
                  
                  //if(i>=offset && i<end){
                  if(i<end){
                     c= data[i];
                     ch2Meets= c<256 && bitset[c];
//System.out.println("    ch2Meets="+ch2Meets);
                  }
                  if(!ch2Meets^inv) break;

//System.out.println("    Ok");
                  
                  term=term.next;
                  continue matchHere;
               }
               case Term.UDIRECTION:{
                  boolean ch1Meets=false,ch2Meets=false;
                  boolean[][] bitset2=term.bitset2;
                  boolean inv=term.inverse;
                  int j=i-1;
                  
                  //if(j>=offset && j<end){
                  if(j>=offset){
                     c= data[j];
                     boolean[] bits=bitset2[c>>8];
                     ch1Meets= bits!=null && bits[c&0xff];
                  }
                  if(ch1Meets^inv) break;
                  
                  //if(i>=offset && i<end){
                  if(i<end){
                     c= data[i];
                     boolean[] bits=bitset2[c>>8];
                     ch2Meets= bits!=null && bits[c&0xff];
                  }
                  if(!ch2Meets^inv) break;
                  
                  term=term.next;
                  continue matchHere;
               }
               case Term.REG:{
                  MemReg mr=memregs[term.memreg];
                  int sampleOffset=mr.in;
                  int sampleOutside=mr.out;
                  int rLen;
                  if(sampleOffset<0 || (rLen=sampleOutside-sampleOffset)<0){
                     break;
                  }
                  else if(rLen==0){
                     term=term.next;
                     continue matchHere;
                  }
                  
                  // don't prevent us from reaching the 'end'
                  if((i+rLen)>end) break;
                  
                  if(compareRegions(data,sampleOffset,i,rLen,end)){
                     i+=rLen;
                     term=term.next;
                     continue matchHere;
                  }
                  break;
               }
               case Term.REG_I:{
                  MemReg mr=memregs[term.memreg];
                  int sampleOffset=mr.in;
                  int sampleOutside=mr.out;
                  int rLen;
                  if(sampleOffset<0 || (rLen=sampleOutside-sampleOffset)<0){
                     break;
                  }
                  else if(rLen==0){
                     term=term.next;
                     continue matchHere;
                  }
                  
                  // don't prevent us from reaching the 'end'
                  if((i+rLen)>end) break;
                  
                  if(compareRegionsI(data,sampleOffset,i,rLen,end)){
                     i+=rLen;
                     term=term.next;
                     continue matchHere;
                  }
                  break;
               }
               case Term.REPEAT_0_INF:{
//System.out.println("REPEAT, i="+i+", term.minCount="+term.minCount+", term.maxCount="+term.maxCount);
                  //i+=(cnt=repeat(data,i,end,term.target));
                  if((cnt=repeat(data,i,end,term.target))<=0){
                     term=term.next;
                     continue;
                  }
                  i+=cnt;
                  
                  //branch out the backtracker (that is term.failNext, see Term.make*())
                  actual.cnt=cnt;
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                  }
                  term=term.next;
                  continue;
               }
               case Term.REPEAT_MIN_INF:{
//System.out.println("REPEAT, i="+i+", term.minCount="+term.minCount+", term.maxCount="+term.maxCount);
                  cnt=repeat(data,i,end,term.target);
                  if(cnt<term.minCount) break;
                  i+=cnt;
                  
                  //branch out the backtracker (that is term.failNext, see Term.make*())
                  actual.cnt=cnt;
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                  }
                  term=term.next;
                  continue;
               }
               case Term.REPEAT_MIN_MAX:{
//System.out.println("REPEAT, i="+i+", term.minCount="+term.minCount+", term.maxCount="+term.maxCount);
                  int out1=end;
                  int out2=i+term.maxCount;
                  cnt=repeat(data,i,out1<out2? out1: out2,term.target);
                  if(cnt<term.minCount) break;
                  i+=cnt;
                  
                  //branch out the backtracker (that is term.failNext, see Term.make*())
                  actual.cnt=cnt;
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                  }
                  term=term.next;
                  continue;
               }
               case Term.REPEAT_REG_MIN_INF:{
               	 MemReg mr=memregs[term.memreg];
                  int sampleOffset=mr.in;
                  int sampleOutside=mr.out;
                  //if(sampleOffset<0) throw new Error("register is referred before definition: "+term.memreg);
                  //if(sampleOutside<0 || sampleOutside<sampleOffset) throw new Error("register is referred within definition: "+term.memreg);
                  /*@since 1.2*/
                  int bitset;
                  if(sampleOffset<0 || (bitset=sampleOutside-sampleOffset)<0){
                     break;
                  }
                  else if(bitset==0){
                     term=term.next;
                     continue matchHere;
                  }
                  
                  cnt=0;
                  
                  while(compareRegions(data,i,sampleOffset,bitset,end)){
                     cnt++;
                     i+=bitset;
                  }
                  
                  if(cnt<term.minCount) break;
                  
                  actual.cnt=cnt;
                  actual.term=term.failNext;
                  actual.index=i;
                  actual.regLen=bitset;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  term=term.next;
                  continue;
               }
               case Term.REPEAT_REG_MIN_MAX:{
               	   MemReg mr=memregs[term.memreg];
                  int sampleOffset=mr.in;
                  int sampleOutside=mr.out;
                  //if(sampleOffset<0) throw new Error("register is referred before definition: "+term.memreg);
                  //if(sampleOutside<0 || sampleOutside<sampleOffset) throw new Error("register is referred within definition: "+term.memreg);
                  /*@since 1.2*/
                  int bitset;
                  if(sampleOffset<0 || (bitset=sampleOutside-sampleOffset)<0){
                     break;
                  }
                  else if(bitset==0){
                     term=term.next;
                     continue matchHere;
                  }
                  
                  cnt=0;
                  int countBack=term.maxCount;
                  while(countBack>0 && compareRegions(data,i,sampleOffset,bitset,end)){
                     cnt++;
                     i+=bitset;
                     countBack--;
                  }
                  
                  if(cnt<term.minCount) break;
                  
                  actual.cnt=cnt;
                  actual.term=term.failNext;
                  actual.index=i;
                  actual.regLen=bitset;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  term=term.next;
                  continue;
               }
               case Term.BACKTRACK_0:
//System.out.println("<<");
                  cnt=actual.cnt;
                  if(cnt>0){
                     cnt--;
                     i--;
                     actual.cnt=cnt;
                     actual.index=i;
                     actual.term=term;
                     actual=(top=actual).on;
                     if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                     }
                     term=term.next;
                     continue;
                  }
                  else break;
               
               case Term.BACKTRACK_MIN:
//System.out.println("<<");
                  cnt=actual.cnt;
                  if(cnt>term.minCount){
                     cnt--;
                     i--;
                     actual.cnt=cnt;
                     actual.index=i;
                     actual.term=term;
                     actual=(top=actual).on;
                     if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                     }
                     term=term.next;
                     continue;
                  }
                  else break;
               
               case Term.BACKTRACK_FIND_MIN:{
//System.out.print("<<<[cnt=");
                  cnt=actual.cnt;
//System.out.print(cnt+", minCnt=");
//System.out.print(term.minCount+", target=");
//System.out.print(term.target+"]");
                  int minCnt;
                  if(cnt>(minCnt=term.minCount)){
                     int start=i+term.distance;
                     if(start>end){
                        int exceed=start-end;
                        cnt-=exceed;
                        if(cnt<=minCnt) break;
                        i-=exceed;
                        start=end;
                     }
                     int back=findBack(data,i+term.distance,cnt-minCnt,term.target);
//System.out.print("[back="+back+"]");
                     if(back<0) break;
                     
                     //cnt-=back;
                     //i-=back;
                     if((cnt-=back)<=minCnt){
                        i-=back;
                        if(term.eat)i++;
                        term=term.next;
                        continue;
                     }
                     i-=back;
                     
                     actual.cnt=cnt;
                     actual.index=i;
                     
                     if(term.eat)i++;
                     
                     actual.term=term;
                     actual=(top=actual).on;
                     if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                     }
                     term=term.next;
                     continue;
                  }
                  else break;
               }
               
               case Term.BACKTRACK_FINDREG_MIN:{
//System.out.print("<<<[cnt=");
                  cnt=actual.cnt;
//System.out.print(cnt+", minCnt=");
//System.out.print(term.minCount+", target=");
//System.out.print(term.target);
//System.out.print("reg=<"+memregs[term.target.memreg].in+","+memregs[term.target.memreg].out+">]");
                  int minCnt;
                  if(cnt>(minCnt=term.minCount)){
                     int start=i+term.distance;
                     if(start>end){
                        int exceed=start-end;
                        cnt-=exceed;
                        if(cnt<=minCnt) break;
                        i-=exceed;
                        start=end;
                     }
                     MemReg mr=memregs[term.target.memreg];
                     int sampleOff=mr.in;
                     int sampleLen=mr.out-sampleOff;
                     //if(sampleOff<0 || sampleLen<0) throw new Error("backreference used before definition: \\"+term.memreg);
                     //int back=findBackReg(data,i+term.distance,sampleOff,sampleLen,cnt-minCnt,term.target,end);
                     //if(back<0) break;
                     /*@since 1.2*/
                     int back;
                     if(sampleOff<0 || sampleLen<0){ 
                     //the group is not def., as in the case of '(\w+)\1'
                     //treat as usual BACKTRACK_MIN
                        cnt--;
                        i--;
                        actual.cnt=cnt;
                        actual.index=i;
                        actual.term=term;
                        actual=(top=actual).on;
                        if(actual==null){
                           actual=new SearchEntry();
                           top.on=actual;
                           actual.sub=top;
                        }
                        term=term.next;
                        continue;
                     }
                     else if(sampleLen==0){
                        back=1;
                     }
                     else{
                        back=findBackReg(data,i+term.distance,sampleOff,sampleLen,cnt-minCnt,term.target,end);
//System.out.print("[back="+back+"]");
                        if(back<0) break;
                     }
                     cnt-=back;
                     i-=back;
                     actual.cnt=cnt;
                     actual.index=i;
                     
                     if(term.eat)i+=sampleLen;
                     
                     actual.term=term;
                     actual=(top=actual).on;
                     if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                     }
                     term=term.next;
                     continue;
                  }
                  else break;
               }
               
               case Term.BACKTRACK_REG_MIN:
//System.out.println("<<");
                  cnt=actual.cnt;
                  if(cnt>term.minCount){
                     regLen=actual.regLen;
                     cnt--;
                     i-=regLen;
                     actual.cnt=cnt;
                     actual.index=i;
                     actual.term=term;
                     //actual.regLen=regLen;
                     actual=(top=actual).on;
                     if(actual==null){
                        actual=new SearchEntry();
                        top.on=actual;
                        actual.sub=top;
                     }
                     term=term.next;
                     continue;
                  }
                  else break;
               
               case Term.GROUP_IN:{
                  memreg=term.memreg;
                  //memreg=0 is a regex itself; we don't need to handle it
                  //because regex bounds already are in wOffset and wEnd
                  if(memreg>0){
                     //MemReg mr=memregs[memreg];
                     //saveMemregState((top!=null)? top: defaultEntry,memreg,mr);
                     //mr.in=i;
                     
                     memregs[memreg].tmp=i; //assume
                  }
                  term=term.next;
                  continue;
               }
               case Term.GROUP_OUT:
                  memreg=term.memreg;
                  //see above
                  if(memreg>0){
                     //if(term.saveState)saveMemregState((top!=null)? top: defaultEntry,memreg,memregs);
                     
                     MemReg mr=memregs[memreg];
                     SearchEntry.saveMemregState((top!=null)? top: defaultEntry,memreg,mr);
                     mr.in=mr.tmp; //commit
                     mr.out=i;
                  }
                  term=term.next;
                  continue;
               
               case Term.PLOOKBEHIND_IN:{
                  int tmp=i-term.distance;
                  if(tmp<offset) break;
//System.out.println("term="+term+", next="+term.next);
               	 LAEntry le=lookaheads[term.lookaheadId];
               	 le.index=i;
                  i=tmp;
                  le.actual=actual;
                  le.top=top;
                  term=term.next;
                  continue;
               }
               case Term.INDEPENDENT_IN:
               case Term.PLOOKAHEAD_IN:{
               	 LAEntry le=lookaheads[term.lookaheadId];
               	 le.index=i;
                  le.actual=actual;
                  le.top=top;
                  term=term.next;
                  continue;
               }
               case Term.LOOKBEHIND_CONDITION_OUT:
               case Term.LOOKAHEAD_CONDITION_OUT:
               case Term.PLOOKAHEAD_OUT:
               case Term.PLOOKBEHIND_OUT:{
                  LAEntry le=lookaheads[term.lookaheadId];
               	   i=le.index;
                  actual=le.actual;
                  top=le.top;
                  term=term.next;
                  continue;
               }
               case Term.INDEPENDENT_OUT:{
                  LAEntry le=lookaheads[term.lookaheadId];
               	   actual=le.actual;
                  top=le.top;
                  term=term.next;
                  continue;
               }
               case Term.NLOOKBEHIND_IN:{
                  int tmp=i-term.distance;
                  if(tmp<offset){
                     term=term.failNext;
                     continue;
                  }
                  LAEntry le=lookaheads[term.lookaheadId];
                  le.actual=actual;
                  le.top=top;
                  
                  actual.term=term.failNext;
                  actual.index=i;
                  i=tmp;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  term=term.next;
                  continue;
               }
               case Term.NLOOKAHEAD_IN:{
                  LAEntry le=lookaheads[term.lookaheadId];
                  le.actual=actual;
                  le.top=top;
                  
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  
                  term=term.next;
                  continue;
               }
               case Term.NLOOKBEHIND_OUT:
               case Term.NLOOKAHEAD_OUT:{
                  LAEntry le=lookaheads[term.lookaheadId];
               	 actual=le.actual;
                  top=le.top;
                  break;
               }
               case Term.LOOKBEHIND_CONDITION_IN:{
                  int tmp=i-term.distance;
                  if(tmp<offset){
                     term=term.failNext;
                     continue;
                  }
                  LAEntry le=lookaheads[term.lookaheadId];
                  le.index=i;
                  le.actual=actual;
                  le.top=top;
                  
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  
                  i=tmp;
                  
                  term=term.next;
                  continue;
               }
               case Term.LOOKAHEAD_CONDITION_IN:{
                  LAEntry le=lookaheads[term.lookaheadId];
                  le.index=i;
                  le.actual=actual;
                  le.top=top;
                  
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  
                  term=term.next;
                  continue;
               }
               case Term.MEMREG_CONDITION:{
                  MemReg mr=memregs[term.memreg];
                  int sampleOffset=mr.in;
                  int sampleOutside=mr.out;
                  if(sampleOffset>=0 && sampleOutside>=0 && sampleOutside>=sampleOffset){
                     term=term.next;
                  }
                  else{
                     term=term.failNext;
                  }
                  continue;
               }
               case Term.BRANCH_STORE_CNT_AUX1:
                  actual.regLen=regLen;
               case Term.BRANCH_STORE_CNT:
                  actual.cnt=cnt;
               case Term.BRANCH:
                  actual.term=term.failNext;
                  actual.index=i;
                  actual=(top=actual).on;
                  if(actual==null){
                     actual=new SearchEntry();
                     top.on=actual;
                     actual.sub=top;
                  }
                  term=term.next;
                  continue;

               case Term.SUCCESS:
//System.out.println("success, matchEnd="+matchEnd+", i="+i+", end="+end);
                  if(!matchEnd || i==end){
                     this.wOffset=memregs[0].in=wOffset;
                     this.wEnd=memregs[0].out=i;
                     this.top=top;
                     return true;
                  }
                  else break;

               case Term.CNT_SET_0:
                  cnt=0;
                  term=term.next;
                  continue;

               case Term.CNT_INC:
                  cnt++;
                  term=term.next;
                  continue;
               
               case Term.CNT_GT_EQ:
                  if(cnt>=term.maxCount){
                     term=term.next;
                     continue;
                  }
                  else break;
               
               case Term.READ_CNT_LT:
                  cnt=actual.cnt;
                  if(cnt<term.maxCount){
                     term=term.next;
                     continue;
                  }
                  else break;
               
               case Term.CRSTORE_CRINC:{
               	 int cntvalue=counters[cntreg=term.cntreg];
                  SearchEntry.saveCntState((top!=null)? top: defaultEntry,cntreg,cntvalue);
                  counters[cntreg]=++cntvalue;
                  term=term.next;
                  continue;
               }
               case Term.CR_SET_0:
                  counters[term.cntreg]=0;

                  term=term.next;
                  continue;

               case Term.CR_LT:
                  if(counters[term.cntreg]<term.maxCount){
                     term=term.next;
                     continue;
                  }
                  else break;

               case Term.CR_GT_EQ:
                  if(counters[term.cntreg]>=term.maxCount){
                     term=term.next;
                     continue;
                  }
                  else break;
                               
               default:
                  throw new Error("unknown term type: "+term.type);
            }
            
            //if(top==null) break matchHere;
            if(allowIncomplete && i==end){
               //an attempt to implement matchesPrefix()
               //not sure it's a good way
               //27-04-2002: just as expencted, 
               //the side effect was found (and POSSIBLY fixed); 
               //see the case Term.START
               return true;
            }
            if(top==null){
               break matchHere;
            }
            
            //pop the stack
            top=(actual=top).sub;
            term=actual.term;
            i=actual.index;
//System.out.println("***POP*** :  branch to #"+term.instanceNum+" at "+i);
            if(actual.isState){
               SearchEntry.popState(actual,memregs,counters);
            }
         }
         
         if(defaultEntry.isState)SearchEntry.popState(defaultEntry,memregs,counters);
         
         term=root;
         //wOffset++;
         //i=wOffset;
         i=++wOffset;
      }
      this.wOffset=wOffset;
      this.top=top;
      
      return false;
   }
   
   private static final boolean compareRegions(char[] arr, int off1, int off2, int len,int out){
//System.out.print("out="+out+", off1="+off1+", off2="+off2+", len="+len+", reg1="+new String(arr,off1,len)+", reg2="+new String(arr,off2,len));
      int p1=off1+len-1;
      int p2=off2+len-1;
      if(p1>=out || p2>=out){
//System.out.println(" : out");
         return false;
      }
      for(int c=len;c>0;c--,p1--,p2--){
         if(arr[p1]!=arr[p2]){
//System.out.println(" : no");
         	  return false;
         }
      }
//System.out.println(" : yes");
      return true;
   }
   
   private static final boolean compareRegionsI(char[] arr, int off1, int off2, int len,int out){
      int p1=off1+len-1;
      int p2=off2+len-1;
      if(p1>=out || p2>=out){
         return false;
      }
      char c1,c2;
      for(int c=len;c>0;c--,p1--,p2--){
         if((c1=arr[p1])!=Character.toLowerCase(c2=arr[p2]) &&
            c1!=Character.toUpperCase(c2) &&
            c1!=Character.toTitleCase(c2)) return false;
      }
      return true;
   }
   
   //repeat while matches
   private static final int repeat(char[] data,int off,int out,Term term){
//System.out.print("off="+off+", out="+out+", term="+term);
      switch(term.type){
         case Term.CHAR:{
            char c=term.c;
            int i=off;
            while(i<out){
               if(data[i]!=c) break;
               i++;
            }
//System.out.println(", returning "+(i-off));
            return i-off;
         }
         case Term.ANY_CHAR:{
            return out-off;
         }
         case Term.ANY_CHAR_NE:{
            int i=off;
            while(i<out){
               if(data[i]=='\n') break;
               i++;
            }
            return i-off;
         }
         case Term.BITSET:{
            boolean[] arr=term.bitset;
            int i=off;
            char c;
            if(term.inverse) while(i<out){
               if((c=data[i])<=255 && arr[c]) break;
               else i++;
            }
            else while(i<out){
               if((c=data[i])<=255 && arr[c]) i++;
               else break;
            }
            return i-off;
         }
         case Term.BITSET2:{
            int i=off;
            boolean[][] bitset2=term.bitset2;
            char c;
            if(term.inverse) while(i<out){
               boolean[] arr=bitset2[(c=data[i])>>8];
               if(arr!=null && arr[c&0xff]) break;
               else i++;
            }
            else while(i<out){
               boolean[] arr=bitset2[(c=data[i])>>8];
               if(arr!=null && arr[c&0xff]) i++;
               else break;
            }
            return i-off;
         }
      }
      throw new Error("this kind of term can't be quantified:"+term.type);
   }
   
   //repeat while doesn't match
   private static final int find(char[] data,int off,int out,Term term){
//System.out.print("off="+off+", out="+out+", term="+term);
      if(off>=out) return -1;
      switch(term.type){
         case Term.CHAR:{
            char c=term.c;
            int i=off;
            while(i<out){
               if(data[i]==c) break;
               i++;
            }
//System.out.println(", returning "+(i-off));
            return i-off;
         }
         case Term.BITSET:{
            boolean[] arr=term.bitset;
            int i=off;
            char c;
            if(!term.inverse) while(i<out){
               if((c=data[i])<=255 && arr[c]) break;
               else i++;
            }
            else while(i<out){
               if((c=data[i])<=255 && arr[c]) i++;
               else break;
            }
            return i-off;
         }
         case Term.BITSET2:{
            int i=off;
            boolean[][] bitset2=term.bitset2;
            char c;
            if(!term.inverse) while(i<out){
               boolean[] arr=bitset2[(c=data[i])>>8];
               if(arr!=null && arr[c&0xff]) break;
               else i++;
            }
            else while(i<out){
               boolean[] arr=bitset2[(c=data[i])>>8];
               if(arr!=null && arr[c&0xff]) i++;
               else break;
            }
            return i-off;
         }
      }
      throw new IllegalArgumentException("can't seek this kind of term:"+term.type);
   }
   
   
   private static final int findReg(char[] data,int off,int regOff,int regLen,Term term,int out){
//System.out.print("off="+off+", out="+out+", term="+term);
      if(off>=out) return -1;
      int i=off;
      if(term.type==Term.REG){
         while(i<out){
            if(compareRegions(data,i,regOff,regLen,out)) break;
            i++;
         }
      }
      else if(term.type==Term.REG_I){
         while(i<out){
            if(compareRegionsI(data,i,regOff,regLen,out)) break;
            i++;
         }
      }
      else throw new IllegalArgumentException("wrong findReg() target:"+term.type);
      return off-i;
   }
   
   private static final int findBack(char[] data,int off,int maxCount,Term term){
//System.out.print("off="+off+", maxCount="+maxCount+", term="+term);
      switch(term.type){
         case Term.CHAR:{
            char c=term.c;
            int i=off;
            int iMin=off-maxCount;
            for(;;){
               if(data[--i]==c) break;
               if(i<=iMin) return -1; 
            }
//System.out.println(", returning "+(off-i));
            return off-i;
         }
         case Term.BITSET:{
            boolean[] arr=term.bitset;
            int i=off;
            char c;
            int iMin=off-maxCount;
            if(!term.inverse) for(;;){
               if((c=data[--i])<=255 && arr[c]) break;
               if(i<=iMin) return -1;
            }
            else for(;;){
               if((c=data[--i])>255 || !arr[c]) break;
               if(i<=iMin) return -1;
            }
            return off-i;
         }
         case Term.BITSET2:{
            boolean[][] bitset2=term.bitset2;
            int i=off;
            char c;
            int iMin=off-maxCount;
            if(!term.inverse) for(;;){
               boolean[] arr=bitset2[(c=data[--i])>>8];
               if(arr!=null && arr[c&0xff]) break;
               if(i<=iMin) return -1;
            }
            else for(;;){
               boolean[] arr=bitset2[(c=data[--i])>>8];
               if(arr==null || arr[c&0xff]) break;
               if(i<=iMin) return -1;
            }
            return off-i;
         }
      }
      throw new IllegalArgumentException("can't find this kind of term:"+term.type);
   }
   
   private static final int findBackReg(char[] data,int off,int regOff,int regLen,int maxCount,Term term,int out){
      //assume that the cases when regLen==0 or maxCount==0 are handled by caller
      int i=off;
      int iMin=off-maxCount;
      if(term.type==Term.REG){
         /*@since 1.2*/
         char first=data[regOff];
         regOff++;
         regLen--;
         for(;;){
            i--;
            if(data[i]==first && compareRegions(data,i+1,regOff,regLen,out)) break;
            if(i<=iMin) return -1;
         }
      }
      else if(term.type==Term.REG_I){
         /*@since 1.2*/
         char c=data[regOff];
         char firstLower=Character.toLowerCase(c);
         char firstUpper=Character.toUpperCase(c);
         char firstTitle=Character.toTitleCase(c);
         regOff++;
         regLen--;
         for(;;){
            i--;
            if(((c=data[i])==firstLower || c==firstUpper || c==firstTitle) && compareRegionsI(data,i+1,regOff,regLen,out)) break;
            if(i<=iMin) return -1;
         }
         return off-i;
      }
      else throw new IllegalArgumentException("wrong findBackReg() target type :"+term.type);
      return off-i;
   }
   
   public String toString_d(){
      StringBuffer s=new StringBuffer();
      s.append("counters: ");
      s.append(counters==null? 0: counters.length);

      s.append("\r\nmemregs: ");
      s.append(memregs.length);
      for(int i=0;i<memregs.length;i++) s.append("\r\n #"+i+": ["+memregs[i].in+","+memregs[i].out+"](\""+getString(memregs[i].in,memregs[i].out)+"\")");
   
      s.append("\r\ndata: ");
      if(data!=null)s.append(data.length);
      else s.append("[none]");
      
      s.append("\r\noffset: ");
      s.append(offset);
   
      s.append("\r\nend: ");
      s.append(end);
   
      s.append("\r\nwOffset: ");
      s.append(wOffset);
   
      s.append("\r\nwEnd: ");
      s.append(wEnd);
   
      s.append("\r\nregex: ");
      s.append(re);
      return s.toString();
   }
}

class SearchEntry{
   Term term;
   int index;
   int cnt;
   int regLen;
   
   boolean isState;
   
   SearchEntry sub,on;
   
   private static class MState{
      int index,in,out;
      MState next,prev;
   }
   
   private static class CState{
      int index,value;
      CState next,prev;
   }
   
   private MState mHead,mCurrent;
   private CState cHead,cCurrent;
   
   final static void saveMemregState(SearchEntry entry,int memreg, MemReg mr){
//System.out.println("saveMemregState("+entry+","+memreg+"):");
      entry.isState=true;
      MState current=entry.mCurrent;
      if(current==null){
         MState head=entry.mHead;
         if(head==null) entry.mHead=entry.mCurrent=current=new MState();
         else current=head;
      }
      else{
         MState next=current.next;
         if(next==null){
            current.next=next=new MState();
            next.prev=current;
         }
         current=next;
      }
      current.index=memreg;
      current.in=mr.in;
      current.out=mr.out;
      entry.mCurrent=current;
   }
   
   final static void saveCntState(SearchEntry entry,int cntreg,int value){
      entry.isState=true;
      CState current=entry.cCurrent;
      if(current==null){
         CState head=entry.cHead;
         if(head==null) entry.cHead=entry.cCurrent=current=new CState();
         else current=head;
      }
      else{
         CState next=current.next;
         if(next==null){
            current.next=next=new CState();
            next.prev=current;
         }
         current=next;
      }
      current.index=cntreg;
      current.value=value;
      entry.cCurrent=current;
   }
   
   final static void popState(SearchEntry entry, MemReg[] memregs, int[] counters){
//System.out.println("popState("+entry+"):");
      MState ms=entry.mCurrent;
      while(ms!=null){
         MemReg mr=memregs[ms.index];
         mr.in=ms.in;
         mr.out=ms.out;
         ms=ms.prev;
      }
      CState cs=entry.cCurrent;
      while(cs!=null){
         counters[cs.index]=cs.value;
         cs=cs.prev;
      }
      entry.mCurrent=null;
      entry.cCurrent=null;
      entry.isState=false;
   }
   
   final void reset(int restQueue){
      term=null;
      index=cnt=regLen=0;
      
      mCurrent=null;
      cCurrent=null;
      isState=false;
      
      SearchEntry on=this.on;
      if(on!=null){
         if(restQueue>0) on.reset(restQueue-1);
         else{
            this.on=null;
            on.sub=null;
         }
      }
      //sub=on=null;      
   }
}

class MemReg{
   int index;
   
   int in=-1,out=-1;
   int tmp=-1;  //for assuming at GROUP_IN
   
   MemReg(int index){
      this.index=index;
   }
   
   void reset(){
      in=out=-1;
   }
}

class LAEntry{
   int index;
   SearchEntry top,actual;
}
