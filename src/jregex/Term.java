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

class Term implements REFlags{
   //runtime Term types
   static final int CHAR        = 0;
   static final int BITSET      = 1;
   static final int BITSET2     = 2;
   static final int ANY_CHAR    = 4;
   static final int ANY_CHAR_NE = 5;
   
   static final int REG         = 6;
   static final int REG_I       = 7;
   static final int FIND        = 8;
   static final int FINDREG     = 9;
   static final int SUCCESS     = 10;
   
   /*optimization-transparent types*/
   static final int BOUNDARY    = 11;
   static final int DIRECTION   = 12;
   static final int UBOUNDARY    = 13;
   static final int UDIRECTION   = 14;
   
   static final int GROUP_IN          = 15;
   static final int GROUP_OUT         = 16;
   static final int VOID              = 17;
   
   static final int START             = 18;
   static final int END               = 19;
   static final int END_EOL           = 20;
   static final int LINE_START        = 21;
   static final int LINE_END          = 22;
   static final int LAST_MATCH_END    = 23;
   
   static final int CNT_SET_0      = 24;
   static final int CNT_INC        = 25;
   static final int CNT_GT_EQ      = 26;
   static final int READ_CNT_LT    = 27;

   static final int CRSTORE_CRINC = 28; //store on 'actual' search entry
   static final int CR_SET_0      = 29;
   static final int CR_LT         = 30;
   static final int CR_GT_EQ      = 31;
   
   /*optimization-nontransparent types*/
   static final int BRANCH                 = 32;
   static final int BRANCH_STORE_CNT       = 33;
   static final int BRANCH_STORE_CNT_AUX1  = 34;
   
   static final int PLOOKAHEAD_IN           = 35;
   static final int PLOOKAHEAD_OUT          = 36;
   static final int NLOOKAHEAD_IN           = 37;
   static final int NLOOKAHEAD_OUT          = 38;
   static final int PLOOKBEHIND_IN           = 39;
   static final int PLOOKBEHIND_OUT          = 40;
   static final int NLOOKBEHIND_IN           = 41;
   static final int NLOOKBEHIND_OUT          = 42;
   static final int INDEPENDENT_IN          = 43; //functionally the same as NLOOKAHEAD_IN
   static final int INDEPENDENT_OUT         = 44;
   
   static final int REPEAT_0_INF       = 45;
   static final int REPEAT_MIN_INF     = 46;
   static final int REPEAT_MIN_MAX     = 47;
   static final int REPEAT_REG_MIN_INF = 48;
   static final int REPEAT_REG_MIN_MAX = 49;
   
   static final int BACKTRACK_0           = 50;
   static final int BACKTRACK_MIN         = 51;
   static final int BACKTRACK_FIND_MIN    = 52;
   static final int BACKTRACK_FINDREG_MIN = 53;
   static final int BACKTRACK_REG_MIN     = 54;
   
   static final int MEMREG_CONDITION        = 55;
   static final int LOOKAHEAD_CONDITION_IN  = 56;
   static final int LOOKAHEAD_CONDITION_OUT = 57;
   static final int LOOKBEHIND_CONDITION_IN  = 58;
   static final int LOOKBEHIND_CONDITION_OUT = 59;
   
   //optimization
   static final int FIRST_TRANSPARENT = BOUNDARY;
   static final int LAST_TRANSPARENT  = CR_GT_EQ;
   
   // compiletime: length of vars[] (see makeTree())
   static final int VARS_LENGTH=4;

   // compiletime variable indicies:
   private static final int MEMREG_COUNT=0;    //refers current memreg index
   private static final int CNTREG_COUNT=1;   //refers current counters number
   private static final int DEPTH=2;      //refers current depth: (((depth=3)))
   private static final int LOOKAHEAD_COUNT=3;    //refers current memreg index
   
   private static final int LIMITS_LENGTH=3;
   private static final int LIMITS_PARSE_RESULT_INDEX=2;
   private static final int LIMITS_OK=1;
   private static final int LIMITS_FAILURE=2;
   
   //static CustomParser[] customParsers=new CustomParser[256];
   
   // **** CONTROL FLOW **** 

   // next-to-execute and next-if-failed commands;
   Term next,failNext;
   
   // **** TYPES ****
   
   int type=VOID;
   boolean inverse;
   
   // used with type=CHAR
   char c;
   
   // used with type=FIND
   int distance;
   boolean eat;
   
   // used with type=BITSET(2);
   boolean[] bitset;
   boolean[][] bitset2;
   boolean[] categoryBitset;  //types(unicode categories)
   
   // used with type=BALANCE;
   char[] brackets;
   
   // used for optimization with type=BITSET,BITSET2
   int weight;

   // **** MEMORISATION ****

   // memory slot, used with type=REG,GROUP_IN,GROUP_OUT
   int memreg=-1;


   // **** COUNTERS ****

   // max|min number of iterations
   // used with CNT_GT_EQ ,REPEAT_* etc.;
   int minCount,maxCount;
   
   // used with REPEAT_*,REPEAT_REG_*;
   Term target;
   
   // a counter slot to increment & compare with maxCount (CNT_INC etc.);
   int cntreg=0;
   
   // lookahead group id;
   int lookaheadId;
   
   // **** COMPILE HELPERS ****

   protected Term prev,in,out,out1,first,current;
   
   //new!!
   protected Term branchOut;
   
   //protected  boolean newBranch=false,closed=false;
   //protected  boolean newBranch=false;

   //for debugging
   static int instances;
   int instanceNum;
   
   Term(){
      //for debugging
      instanceNum=instances;
      instances++;
      in=out=this;
   }
   
   Term(int type){
      this();
      this.type=type;
   }
   
   static void makeTree(String s, int flags,Pattern re) throws PatternSyntaxException{
      char[] data=s.toCharArray();
      makeTree(data,0,data.length,flags,re);
   }
   
   static void makeTree(char[] data,int offset,int end,
         int flags,Pattern re) throws PatternSyntaxException{
      // memreg,counter,depth,lookahead
      int[] vars={1,0,0,0}; //don't use counters[0]
      
      //collect iterators for subsequent optimization
      Vector iterators=new Vector();
      Hashtable groupNames=new Hashtable();
      
      Pretokenizer t=new Pretokenizer(data,offset,end);
      Term term=makeTree(t,data,vars,flags,new Group(),iterators,groupNames);
      // term=(0-...-0)

      // convert closing outer bracket into success term
      term.out.type=SUCCESS;
      // term=(0-...-!!!
      
      //throw out opening bracket
      Term first=term.next;
      // term=...-!!!
      
      // Optimisation: 
      Term optimized=first;
      Optimizer opt=Optimizer.find(first);
      if(opt!=null) optimized=opt.makeFirst(first);
      
      Enumeration en=iterators.elements();
      while(en.hasMoreElements()){
         Iterator i=(Iterator)en.nextElement();
         i.optimize();
      }
      // ===
      
      re.root=optimized;
      re.root0=first;
      re.memregs=vars[MEMREG_COUNT];
      re.counters=vars[CNTREG_COUNT];
      re.lookaheads=vars[LOOKAHEAD_COUNT];
      re.namedGroupMap=groupNames;
   }

   private static Term makeTree(Pretokenizer t,char[] data,int[] vars,
         int flags,Term term,Vector iterators,Hashtable groupNames) throws PatternSyntaxException{
//System.out.println("Term.makeTree(): flags="+flags);
      if(vars.length!=VARS_LENGTH) throw new IllegalArgumentException("vars.length should be "+VARS_LENGTH+", not "+vars.length);
      //Term term=new Term(isMemReg? vars[MEMREG_COUNT]: -1);
      // use memreg 0 as unsignificant
      //Term term=new Group(isMemReg? vars[MEMREG_COUNT]: 0);
      while(true){
         t.next();
         term.append(t.tOffset,t.tOutside,data,vars,flags,iterators,groupNames);
         switch(t.ttype){
            case Pretokenizer.FLAGS:
               flags=t.flags(flags);
               continue;
            case Pretokenizer.CLASS_GROUP:
               t.next();
               Term clg=new Term();
               CharacterClass.parseGroup(data,t.tOffset,t.tOutside,clg,
                               (flags&IGNORE_CASE)>0, (flags&IGNORE_SPACES)>0,
                               (flags&UNICODE)>0, (flags&XML_SCHEMA)>0);
               term.append(clg);
               continue;
            case Pretokenizer.PLAIN_GROUP:
               vars[DEPTH]++;
//System.out.println("PLAIN_GROUP, t.tOffset="+t.tOffset+", t.tOutside="+t.tOutside+", t.flags("+flags+")="+t.flags(flags));
               term.append(makeTree(t,data,vars,t.flags(flags),new Group(),iterators,groupNames));
               break;
            case Pretokenizer.NAMED_GROUP:
               String gname=t.groupName;
               int id;
               if(Character.isDigit(gname.charAt(0))){
                  try{
                     id=Integer.parseInt(gname);
                  }
                  catch(NumberFormatException e){
                     throw new PatternSyntaxException("group name starts with digit but is not a number");
                  }
                  if(groupNames.contains(new Integer(id))){
                     if(t.groupDeclared) throw new PatternSyntaxException("group redeclaration: "+gname+"; use ({=id}...) for multiple group assignments");
                  }
                  if(vars[MEMREG_COUNT]<=id)vars[MEMREG_COUNT]=id+1;
               }
               else{
                  Integer no=(Integer)groupNames.get(gname);
                  if(no==null){
                     id=vars[MEMREG_COUNT]++;
                     groupNames.put(t.groupName,new Integer(id));
                  }
                  else{
                     if(t.groupDeclared) throw new PatternSyntaxException("group redeclaration "+gname+"; use ({=name}...) for group reassignments");
                     id=no.intValue();
                  }
               }
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new Group(id),iterators,groupNames));
               break;
            case '(':
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new Group(vars[MEMREG_COUNT]++),iterators,groupNames));
               break;
            case Pretokenizer.POS_LOOKAHEAD:
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new Lookahead(vars[LOOKAHEAD_COUNT]++,true),iterators,groupNames));
               break;
            case Pretokenizer.NEG_LOOKAHEAD:
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new Lookahead(vars[LOOKAHEAD_COUNT]++,false),iterators,groupNames));
               break;
            case Pretokenizer.POS_LOOKBEHIND:
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new Lookbehind(vars[LOOKAHEAD_COUNT]++,true),iterators,groupNames));
               break;
            case Pretokenizer.NEG_LOOKBEHIND:
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new Lookbehind(vars[LOOKAHEAD_COUNT]++,false),iterators,groupNames));
               break;
            case Pretokenizer.INDEPENDENT_REGEX:
               vars[DEPTH]++;
               term.append(makeTree(t,data,vars,flags,new IndependentGroup(vars[LOOKAHEAD_COUNT]++),iterators,groupNames));
               break;
            case Pretokenizer.CONDITIONAL_GROUP:
               vars[DEPTH]++;
               t.next();
               Term fork=null;
               boolean positive=true;
               switch(t.ttype){
                  case Pretokenizer.NEG_LOOKAHEAD:
                     positive=false;
                  case Pretokenizer.POS_LOOKAHEAD:
                     vars[DEPTH]++;
                     Lookahead la=new Lookahead(vars[LOOKAHEAD_COUNT]++,positive);
                     makeTree(t,data,vars,flags,la,iterators,groupNames);
                     fork=new ConditionalExpr(la);
                     break;
                  case Pretokenizer.NEG_LOOKBEHIND:
                     positive=false;
                  case Pretokenizer.POS_LOOKBEHIND:
                     vars[DEPTH]++;
                     Lookbehind lb=new Lookbehind(vars[LOOKAHEAD_COUNT]++,positive);
                     makeTree(t,data,vars,flags,lb,iterators,groupNames);
                     fork=new ConditionalExpr(lb);
                     break;
                  case '(':
                     t.next();
                     if(t.ttype!=')') throw new PatternSyntaxException("malformed condition");
                     int memregNo;
                     if(Character.isDigit(data[t.tOffset])) memregNo=makeNumber(t.tOffset,t.tOutside,data);
                     else{
                        String gn=new String(data,t.tOffset,t.tOutside-t.tOffset);
                        Integer gno=(Integer)groupNames.get(gn);
                        if(gno==null) throw new PatternSyntaxException("unknown group name in conditional expr.: "+gn);
                        memregNo=gno.intValue();
                     }
                     fork=new ConditionalExpr(memregNo);
                     break;
                  default:
                     throw new PatternSyntaxException("malformed conditional expression: "+t.ttype+" '"+(char)t.ttype+"'");
               }
               term.append(makeTree(t,data,vars,flags,fork,iterators,groupNames));
               break;
            case '|':
               term.newBranch();
               break;
            case Pretokenizer.END:
               if(vars[DEPTH]>0) throw new PatternSyntaxException("unbalanced parenthesis");
               term.close();
               return term;
            case ')':
               if(vars[DEPTH]<=0) throw new PatternSyntaxException("unbalanced parenthesis");
               term.close();
               vars[DEPTH]--;
               return term;
            case Pretokenizer.COMMENT:
               while(t.ttype!=')') t.next();
               continue;
            default:
               throw new PatternSyntaxException("unknown token type: "+t.ttype);
         }
      }
   }
   
   static int makeNumber(int off, int out, char[] data){
      int n=0;
      for(int i=off;i<out;i++){
         int d=data[i]-'0';
         if(d<0 || d>9) return -1;
         n*=10;
         n+=d;
      }
      return n;
   }
   
   protected void append(int offset,int end,char[] data,
         int[] vars,int flags,Vector iterators,Hashtable gmap) throws PatternSyntaxException{
//System.out.println("append("+new String(data,offset,end-offset)+")");
//System.out.println("current="+this.current);
      int[] limits=new int[3];
      int i=offset;
      Term tmp,current=this.current;
      while(i<end){
         char c=data[i];
         boolean greedy=true;
         switch(c){
            //operations
            case '*':
               if(current==null) throw new PatternSyntaxException("missing term before *");
               i++;
               if(i<end){
                   switch(data[i]) {
                   case '?':
                       greedy^=true;
                       i++;
                       break;
                   case '*':
                   case '+':
                       throw new PatternSyntaxException("nested *?+ in regexp");
                   }
               }
               tmp=greedy? makeGreedyStar(vars,current,iterators):
                             makeLazyStar(vars,current);
               current=replaceCurrent(tmp);
               break;
               
            case '+':
               if(current==null) throw new PatternSyntaxException("missing term before +");
               i++;
               if(i<end){
                   switch(data[i]) {
                   case '?':
                       greedy^=true;
                       i++;
                       break;
                   case '*':
                   case '+':
                       throw new PatternSyntaxException("nested *?+ in regexp");
                   }
               }
               tmp=greedy? makeGreedyPlus(vars,current,iterators):
                               makeLazyPlus(vars,current);
               current=replaceCurrent(tmp);
               break;
               
            case '?':
               if(current==null) throw new PatternSyntaxException("missing term before ?");
               i++;
               if(i<end){
                   switch(data[i]) {
                   case '?':
                       greedy^=true;
                       i++;
                       break;
                   case '*':
                   case '+':
                       throw new PatternSyntaxException("nested *?+ in regexp");
                   }
               }
               
               tmp=greedy? makeGreedyQMark(vars,current):
                               makeLazyQMark(vars,current);
               current=replaceCurrent(tmp);
               break;
               
            case '{':
               limits[0]=0;
               limits[1]=-1;
               int le=parseLimits(i+1,end,data,limits);
               if(limits[LIMITS_PARSE_RESULT_INDEX]==LIMITS_OK){ //parse ok
                  if(current==null) throw new PatternSyntaxException("missing term before {}");
                  i=le;
                  if(i<end && data[i]=='?'){
                     greedy^=true;
                     i++;
                  }
                  tmp=greedy? makeGreedyLimits(vars,current,limits,iterators):
                              makeLazyLimits(vars,current,limits);
                  current=replaceCurrent(tmp);
                  break;
               }
               else{ //unicode class or named backreference
                  if(data[i+1]=='\\'){ //'{\name}' - backreference
                     int p=i+2;
                     if(p==end) throw new PatternSyntaxException("'group_id' expected");
                     while(Character.isWhitespace(data[p])){
                        p++;
                        if(p==end) throw new PatternSyntaxException("'group_id' expected");
                     }
                     BackReference br=new BackReference(-1,(flags&IGNORE_CASE)>0);
                     i=parseGroupId(data,p,end,br,gmap);
                     current=append(br);
                     continue;
                  }
                  else{
                     Term t=new Term();
                     i=CharacterClass.parseName(data,i,end,t,false,(flags&IGNORE_SPACES)>0);
                     current=append(t);
                     continue;
                  }
               }
               
            case ' ':
            case '\t':
            case '\r':
            case '\n':
               if((flags&IGNORE_SPACES)>0){
                  i++;
                  continue;
               }
               //else go on as default
               
            //symbolic items
            default:
               tmp=new Term();
               i=parseTerm(data,i,end,tmp,flags);
               
               if(tmp.type==END && i<end){
                   if((flags&IGNORE_SPACES)>0) {
                       i++;
                       while(i<end) {
                           c=data[i];
                           switch(c){
                           case ' ':
                           case '\t':
                           case '\r':
                           case '\n':
                               i++;
                               continue;
                           default:
                               throw new PatternSyntaxException("'$' is not a last term in the group: <"+new String(data,offset,end-offset)+">");
                           }
                       }
                   } else {
                       throw new PatternSyntaxException("'$' is not a last term in the group: <"+new String(data,offset,end-offset)+">");
                   }
               }
               //"\A" 
               //if(tmp.type==START && i>(offset+1)){
               //   throw new PatternSyntaxException("'^' is not a first term in the group: <"+new String(data,offset,end-offset)+">");
               //}
               current=append(tmp);
               break;
         }
//System.out.println("next term: "+next);
//System.out.println("  next.out="+next.out);
//System.out.println("  next.out1="+next.out1);
//System.out.println("  next.branchOut="+next.branchOut);
      }
//System.out.println(in.toStringAll());
//System.out.println("current="+current);
//System.out.println();
   }
   
   
   private static int parseGroupId(char[] data, int i, int end, Term term, Hashtable gmap) throws PatternSyntaxException{
      int id;
      int nstart=i;
      if(Character.isDigit(data[i])){
         while(Character.isDigit(data[i])){
            i++;
            if(i==end) throw new PatternSyntaxException("group_id expected");
         }
         id=makeNumber(nstart,i,data);
      }
      else{
         while(Character.isJavaIdentifierPart(data[i])){
            i++;
            if(i==end) throw new PatternSyntaxException("group_id expected");
         }
         String s=new String(data,nstart,i-nstart);
         Integer no=(Integer)gmap.get(s);
         if(no==null)throw new PatternSyntaxException("backreference to unknown group: "+s);
         id=no.intValue();
      }
      while(Character.isWhitespace(data[i])){
         i++;
         if(i==end) throw new PatternSyntaxException("'}' expected");
      }
      
      int c=data[i++];
      
      if(c!='}') throw new PatternSyntaxException("'}' expected");
      
      term.memreg=id;
      return i;
   }
   
   protected Term append(Term term) throws PatternSyntaxException{
//System.out.println("append("+term.toStringAll()+"), this="+toStringAll());
      //Term prev=this.prev;
      Term current=this.current;
      if(current==null){
//System.out.println("2");
//System.out.println("  term="+term);
//System.out.println("  term.in="+term.in);
         in.next=term;
         term.prev=in;
         this.current=term;
//System.out.println("  result: "+in.toStringAll()+"\r\n");
         return term;
      }
//System.out.println("3");
      link(current,term);
      //this.prev=current;
      this.current=term;
//System.out.println(in.toStringAll());
//System.out.println("current="+this.current);
//System.out.println();
      return term;
   }
   
   protected Term replaceCurrent(Term term) throws PatternSyntaxException{
//System.out.println("replaceCurrent("+term+"), current="+current+", current.prev="+current.prev);
      //Term prev=this.prev;
      Term prev=current.prev;
      if(prev!=null){
         Term in=this.in;
         if(prev==in){
            //in.next=term;
            //term.prev=in;
            in.next=term.in;
            term.in.prev=in;
         }
         else link(prev,term);
      }
      this.current=term;
//System.out.println("   new current="+this.current);
      return term;
   }


   protected void newBranch() throws PatternSyntaxException{
//System.out.println("newBranch()");
      close();
      startNewBranch();
//System.out.println(in.toStringAll());
//System.out.println("current="+current);
//System.out.println();
   }


   protected void close() throws PatternSyntaxException{
//System.out.println("close(), current="+current+", this="+toStringAll());
//System.out.println();
//System.out.println("close()");
//System.out.println("current="+this.current);
//System.out.println("prev="+this.prev);
//System.out.println();
      /*
      Term prev=this.prev;
      if(prev!=null){
         Term current=this.current;
         if(current!=null){
            link(prev,current);
            prev=current;
            this.current=null;
         }
         link(prev,out);
         this.prev=null;
      }
      */
      Term current=this.current;
      if(current!=null) linkd(current,out);
      else in.next=out;
//System.out.println(in.toStringAll());
//System.out.println("current="+this.current);
//System.out.println("prev="+this.prev);
//System.out.println();
   }
   
   private final static void link(Term term,Term next){
      linkd(term,next.in);
      next.prev=term;
   }
   
   private final static void linkd(Term term,Term next){
//System.out.println("linkDirectly(\""+term+"\" -> \""+next+"\")");
      Term prev_out=term.out;
      if(prev_out!=null){
//System.out.println("   prev_out="+prev_out);
         prev_out.next=next;
      }
      Term prev_out1=term.out1;
      if(prev_out1!=null){
//System.out.println("   prev_out1="+prev_out1);
         prev_out1.next=next;
      }
      Term prev_branch=term.branchOut;
      if(prev_branch!=null){
//System.out.println("   prev_branch="+prev_branch);
         prev_branch.failNext=next;
      }
   }
   
   protected void startNewBranch() throws PatternSyntaxException{
//System.out.println("newBranch()");
//System.out.println("before startNewBranch(), this="+toStringAll());
//System.out.println();
      Term tmp=in.next;
      Term b=new Branch();
      in.next=b;
      b.next=tmp;
      b.in=null;
      b.out=null;
      b.out1=null;
      b.branchOut=b;
      current=b;
//System.out.println("startNewBranch(), this="+toStringAll());
//System.out.println();
   }

   private final static Term makeGreedyStar(int[] vars,Term term,Vector iterators) throws PatternSyntaxException{
      //vars[STACK_SIZE]++;
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case INDEPENDENT_IN:
         case GROUP_IN:{
            Term b=new Branch();
            b.next=term.in;
            term.out.next=b;
            
            b.in=b;
            b.out=null;
            b.out1=null;
            b.branchOut=b;
            
            return b;
         }
         default:{
            Iterator i=new Iterator(term,0,-1,iterators);
            return i;
         }
      }
   }

   private final static Term makeLazyStar(int[] vars,Term term){
      //vars[STACK_SIZE]++;
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case GROUP_IN:{
            Term b=new Branch();
            b.failNext=term.in;
            term.out.next=b;
            
            b.in=b;
            b.out=b;
            b.out1=null;
            b.branchOut=null;
            
            return b;
         }
         default:{
            Term b=new Branch();
            b.failNext=term;
            term.next=b;
            
            b.in=b;
            b.out=b;
            b.out1=null;
            b.branchOut=null;
            
            return b;
         }
      }
   }

   private final static Term makeGreedyPlus(int[] vars,Term term,Vector iterators) throws PatternSyntaxException{
      //vars[STACK_SIZE]++;
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case INDEPENDENT_IN://?
         case GROUP_IN:{
//System.out.println("makeGreedyPlus():");
//System.out.println("   in="+term.in);
//System.out.println("   out="+term.out);
            Term b=new Branch();
            b.next=term.in;
            term.out.next=b;
            
            b.in=term.in;
            b.out=null;
            b.out1=null;
            b.branchOut=b;

//System.out.println("   returning "+b.in);
            
            return b;
         }
         default:{
            return new Iterator(term,1,-1,iterators);
         }
      }
   }
   
   private final static Term makeLazyPlus(int[] vars,Term term){
      //vars[STACK_SIZE]++;
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case GROUP_IN:{
            Term b=new Branch();
            term.out.next=b;
            b.failNext=term.in;
            
            b.in=term.in;
            b.out=b;
            b.out1=null;
            b.branchOut=null;
            
            return b;
         }
         case REG:
         default:{
            Term b=new Branch();
            term.next=b;
            b.failNext=term;
            
            b.in=term;
            b.out=b;
            b.out1=null;
            b.branchOut=null;
            
            return b;
         }
      }
   }

   private final static Term makeGreedyQMark(int[] vars,Term term){
      //vars[STACK_SIZE]++;
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case GROUP_IN:{
            Term b=new Branch();
            b.next=term.in;
            
            b.in=b;
            b.out=term.out;
            b.out1=null;
            b.branchOut=b;
            
            return b;
         }
         case REG:
         default:{
            Term b=new Branch();
            b.next=term;
            
            b.in=b;
            b.out=term;
            b.out1=null;
            b.branchOut=b;
            
            return b;
         }
      }
   }
   
   private final static Term makeLazyQMark(int[] vars,Term term){
      //vars[STACK_SIZE]++;
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case GROUP_IN:{
            Term b=new Branch();
            b.failNext=term.in;
            
            b.in=b;
            b.out=b;
            b.out1=term.out;
            b.branchOut=null;
            
            return b;
         }
         case REG:
         default:{
            Term b=new Branch();
            b.failNext=term;
            
            b.in=b;
            b.out=b;
            b.out1=term;
            b.branchOut=null;
            
            return b;
         }
      }
   }

   private final static Term makeGreedyLimits(int[] vars,Term term,int[] limits,Vector iterators) throws PatternSyntaxException{
      //vars[STACK_SIZE]++;
      int m=limits[0];
      int n=limits[1];
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case GROUP_IN:{
            int cntreg=vars[CNTREG_COUNT]++;
            Term reset=new Term(CR_SET_0);
               reset.cntreg=cntreg;
            Term b=new Term(BRANCH);
            
            Term inc=new Term(CRSTORE_CRINC);
               inc.cntreg=cntreg;
            
            reset.next=b;
            
            if(n>=0){
               Term lt=new Term(CR_LT);
                  lt.cntreg=cntreg;
                  lt.maxCount=n;
               b.next=lt;
               lt.next=term.in;
            }
            else{
               b.next=term.in;
            }
            term.out.next=inc;
            inc.next=b;
            
            if(m>=0){
               Term gt=new Term(CR_GT_EQ);
                  gt.cntreg=cntreg;
                  gt.maxCount=m;
               b.failNext=gt;
               
               reset.in=reset;
               reset.out=gt;
               reset.out1=null;
               reset.branchOut=null;
            }
            else{
               reset.in=reset;
               reset.out=null;
               reset.out1=null;
               reset.branchOut=b;
            }
            return reset;
         }
         default:{
            return new Iterator(term,limits[0],limits[1],iterators);
         }
      }
   }

   private final static Term makeLazyLimits(int[] vars,Term term,int[] limits){
      //vars[STACK_SIZE]++;
      int m=limits[0];
      int n=limits[1];
      switch(term.type){
         case REPEAT_0_INF:
         case REPEAT_MIN_INF:
         case REPEAT_MIN_MAX:
         case REPEAT_REG_MIN_INF:
         case REPEAT_REG_MIN_MAX:
         case GROUP_IN:{
            int cntreg=vars[CNTREG_COUNT]++;
            Term reset=new Term(CR_SET_0);
               reset.cntreg=cntreg;
            Term b=new Term(BRANCH);
            Term inc=new Term(CRSTORE_CRINC);
               inc.cntreg=cntreg;
               
            reset.next=b;
            
            if(n>=0){
               Term lt=new Term(CR_LT);
                  lt.cntreg=cntreg;
                  lt.maxCount=n;
               b.failNext=lt;
               lt.next=term.in;
            }
            else{
               b.failNext=term.in;
            }
            term.out.next=inc;
            inc.next=b;
            
            if(m>=0){
               Term gt=new Term(CR_GT_EQ);
                  gt.cntreg=cntreg;
                  gt.maxCount=m;
               b.next=gt;
               
               reset.in=reset;
               reset.out=gt;
               reset.out1=null;
               reset.branchOut=null;
               
               return reset;
            }
            else{
            	  reset.in=reset;
               reset.out=b;
               reset.out1=null;
               reset.branchOut=null;
               
               return reset;
            }
         }
         case REG:
         default:{
            Term reset=new Term(CNT_SET_0);
            Term b=new Branch(BRANCH_STORE_CNT);
            Term inc=new Term(CNT_INC);
            
            reset.next=b;
            
            if(n>=0){
               Term lt=new Term(READ_CNT_LT);
                  lt.maxCount=n;
               b.failNext=lt;
               lt.next=term;
               term.next=inc;
               inc.next=b;
            }
            else{
               b.next=term;
               term.next=inc;
               inc.next=term;
            }
            
            if(m>=0){
               Term gt=new Term(CNT_GT_EQ);
                  gt.maxCount=m;
               b.next=gt;
               
               reset.in=reset;
               reset.out=gt;
               reset.out1=null;
               reset.branchOut=null;
               
               return reset;
            }
            else{
               reset.in=reset;
               reset.out=b;
               reset.out1=null;
               reset.branchOut=null;
               
               return reset;
            }
         }
      }
   }
   
   
   private final int parseTerm(char[] data, int i, int out, Term term,
              int flags) throws PatternSyntaxException{
      char c=data[i++];
      boolean inv=false;
      switch(c){
         case '[':
            return CharacterClass.parseClass(data,i,out,term,(flags&IGNORE_CASE)>0,(flags&IGNORE_SPACES)>0,(flags&UNICODE)>0,(flags&XML_SCHEMA)>0);
            
         case '.':
            term.type=(flags&DOTALL)>0? ANY_CHAR: ANY_CHAR_NE;
            break;
            
         case '$':
            //term.type=mods[MULTILINE_IND]? LINE_END: END; //??
            term.type=(flags&MULTILINE)>0? LINE_END: END_EOL;
            break;
            
         case '^':
            term.type=(flags&MULTILINE)>0? LINE_START: START;
            break;
            
         case '\\':
            if(i>=out) throw new PatternSyntaxException("Escape without a character");
            c=data[i++];
            esc: switch(c){
               case 'f':
                  c='\f'; // form feed
                  break;

               case 'n':
                  c='\n'; // new line
                  break;

               case 'r':
                  c='\r'; // carriage return
                  break;

               case 't':
                  c='\t'; // tab
                  break;
               
               case 'u':
                   if(i+4 >= out) throw new PatternSyntaxException("To few characters for u-escape");

                  c=(char)((CharacterClass.toHexDigit(data[i++])<<12)+
                          (CharacterClass.toHexDigit(data[i++])<<8)+
                          (CharacterClass.toHexDigit(data[i++])<<4)+
                           CharacterClass.toHexDigit(data[i++]));
                  break;
                  
               case 'v':
                   if(i+6 >= out) throw new PatternSyntaxException("To few characters for u-escape");
                  c=(char)((CharacterClass.toHexDigit(data[i++])<<24)+
                          (CharacterClass.toHexDigit(data[i++])<<16)+
                          (CharacterClass.toHexDigit(data[i++])<<12)+
                          (CharacterClass.toHexDigit(data[i++])<<8)+
                          (CharacterClass.toHexDigit(data[i++])<<4)+
                           CharacterClass.toHexDigit(data[i++]));
                  break;
                  
               case 'x':{   // hex 2-digit number -> char
                   if(i >= out) throw new PatternSyntaxException("To few characters for x-escape");
                  int hex=0;
                  char d;
	               if((d=data[i++])=='{'){
	                  while(i<out && (d=data[i++])!='}'){
	                     hex=(hex<<4)+CharacterClass.toHexDigit(d);
	                     if(hex>0xffff) throw new PatternSyntaxException("\\x{<out of range>}");
	                  }
	               }
	               else{
                       if(i >= out) throw new PatternSyntaxException("To few characters for x-escape");
                     hex=(CharacterClass.toHexDigit(d)<<4)+
                          CharacterClass.toHexDigit(data[i++]);
	               }
                  c=(char)hex;
                  break;
               }
               case '0':
               case 'o':   // oct 2- or 3-digit number -> char
                  int oct=0;
                  for(;;){
                     char d=data[i];
                     if(d>='0' && d<='7'){
                         i++;
                        oct*=8;
                        oct+=d-'0';
                        if(oct>0xffff) break;
                        if(i>=out) break;
                     }
                     else break;
                  }
                  c=(char)oct;
                  break;
                  
               case 'm':   // decimal number -> char
                  int dec=0;
                  for(;;){
                     char d=data[i++];
                     if(d>='0' && d<='9'){
                        dec*=10;
                        dec+=d-'0';
                        if(dec>0xffff) break;
                        if(i>=out) break;
                     }
                     else break;
                  }
                  i--;
                  c=(char)dec;
                  break;
                  
               case 'c':   // ctrl-char
                  c=(char)(data[i++]&0x1f);
                  break;

               case 'D':   // non-digit
                  inv=true;
                  // go on
               case 'd':   // digit
                  CharacterClass.makeDigit(term,inv,(flags&UNICODE)>0);
                  return i;

               case 'S':   // non-space
                  inv=true;
                  // go on
               case 's':   // space
                  CharacterClass.makeSpace(term,inv,(flags&UNICODE)>0);
                  return i;

               case 'W':   // non-letter
                  inv=true;
                  // go on
               case 'w':   // letter
                  CharacterClass.makeWordChar(term,inv,(flags&UNICODE)>0);
                  return i;
                  
               case 'B':   // non-(word boundary)
                  inv=true;
                  // go on
               case 'b':   // word boundary
                  CharacterClass.makeWordBoundary(term,inv,(flags&UNICODE)>0);
                  return i;
                  /* NOT SUPPORTED IN RUBY                  
               case '<':   // non-(word boundary)
                  CharacterClass.makeWordStart(term,(flags&UNICODE)>0);
                  return i;
                  
               case '>':   // word boundary
                  CharacterClass.makeWordEnd(term,(flags&UNICODE)>0);
                  return i;
                  */
               case 'A':   // text beginning
                  term.type=START;
                  return i;
                  
               case 'Z':   // text end
                  term.type=END_EOL;
                  return i;
                  
               case 'z':   // text end
                  term.type=END;
                  return i;
                  
               case 'G':   // end of last match
                  term.type=LAST_MATCH_END;
                  return i;
                  
               case 'P':   // \\P{..}
                  inv=true;
               case 'p':   // \\p{..}
                  i=CharacterClass.parseName(data,i,out,term,inv,(flags&IGNORE_SPACES)>0);
                  return i;
                  
               default:
                  if(c>='1' && c<='9'){
                     int n=c-'0';
                     while((i<out) && (c=data[i])>='0' && c<='9'){
                        n=(n*10)+c-'0';
                        i++;
                     }
                     term.type=(flags&IGNORE_CASE)>0? REG_I: REG;
                     term.memreg=n;
                     return i;
                  }
                  /*
                  if(c<256){
                     CustomParser termp=customParsers[c];
                     if(termp!=null){
                        i=termp.parse(i,data,term);
                        return i;
                     }
                  }
                  */
            }
            term.type=CHAR;
            term.c=c;
            break;
            
         default:
            if((flags&IGNORE_CASE)==0){
               term.type=CHAR;
               term.c=c;
            }
            else{
               CharacterClass.makeICase(term,c);
            }
            break;
      }
      return i;
   }


   // one of {n},{n,},{,n},{n1,n2}
   protected static final int parseLimits(int i,int end,char[] data,int[] limits) throws PatternSyntaxException{
      if(limits.length!=LIMITS_LENGTH) throw new IllegalArgumentException("maxTimess.length="+limits.length+", should be 2");
      limits[LIMITS_PARSE_RESULT_INDEX]=LIMITS_OK;
      int ind=0;
      int v=0;
      char c;
      while(i<end){
         c=data[i++];
         switch(c){
            case ' ':
               continue;

            case ',':
               if(ind>0) throw new PatternSyntaxException("illegal construction: {.. , , ..}");
               limits[ind++]=v;
               v=-1;
               continue;

            case '}':
               limits[ind]=v;
               if(ind==0) limits[1]=v;
               return i;

            default:
               if(c>'9' || c<'0'){
                  //throw new PatternSyntaxException("illegal symbol in iterator: '{"+c+"}'");
                  limits[LIMITS_PARSE_RESULT_INDEX]=LIMITS_FAILURE;
                  return i;
               }
               if(v<0) v=0;
               v= v*10 + (c-'0');
         }
      }
      throw new PatternSyntaxException("malformed quantifier");
   }
   
   public String toString(){
      StringBuffer b=new StringBuffer(100);
      b.append(instanceNum);
      b.append(": ");
      if(inverse) b.append('^');
      switch(type){
         case VOID:
            b.append("[]");
            b.append(" , ");
            break;
         case CHAR:
            b.append(CharacterClass.stringValue(c));
            b.append(" , ");
            break;
         case ANY_CHAR:
            b.append("dotall, ");
            break;
         case ANY_CHAR_NE:
            b.append("dot-eols, ");
            break;
         case BITSET:
            b.append('[');
            b.append(CharacterClass.stringValue0(bitset));
            b.append(']');
            b.append(" , weight=");
            b.append(weight);
            b.append(" , ");
            break;
         case BITSET2:
            b.append('[');
            b.append(CharacterClass.stringValue2(bitset2));
            b.append(']');
            b.append(" , weight=");
            b.append(weight);
            b.append(" , ");
            break;
         case START:
            b.append("abs.start");
            break;            
         case END:
            b.append("abs.end");
            break;            
         case END_EOL:
            b.append("abs.end-eol");
            break;            
         case LINE_START:
            b.append("line start");
            break;            
         case LINE_END:
            b.append("line end");
            break;            
         case LAST_MATCH_END:
            if(inverse)b.append("non-");
            b.append("BOUNDARY");
            break;            
         case BOUNDARY:
            if(inverse)b.append("non-");
            b.append("BOUNDARY");
            break;            
         case UBOUNDARY:
            if(inverse)b.append("non-");
            b.append("UBOUNDARY");
            break;            
         case DIRECTION:
            b.append("DIRECTION");
            break;            
         case UDIRECTION:
            b.append("UDIRECTION");
            break;            
         case FIND:
            b.append(">>>{");
            b.append(target);
            b.append("}, <<");
            b.append(distance);
            if(eat){
               b.append(",eat");
            }
            b.append(", ");
            break;            
         case REPEAT_0_INF:
            b.append("rpt{");
            b.append(target);
            b.append(",0,inf}");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;            
         case REPEAT_MIN_INF:
            b.append("rpt{");
            b.append(target);
            b.append(",");
            b.append(minCount);
            b.append(",inf}");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;            
         case REPEAT_MIN_MAX:
            b.append("rpt{");
            b.append(target);
            b.append(",");
            b.append(minCount);
            b.append(",");
            b.append(maxCount);
            b.append("}");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;            
         case REPEAT_REG_MIN_INF:
            b.append("rpt{$");
            b.append(memreg);
            b.append(',');
            b.append(minCount);
            b.append(",inf}");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;            
         case REPEAT_REG_MIN_MAX:
            b.append("rpt{$");
            b.append(memreg);
            b.append(',');
            b.append(minCount);
            b.append(',');
            b.append(maxCount);
            b.append("}");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;            
         case BACKTRACK_0:
            b.append("back(0)");
            break;            
         case BACKTRACK_MIN:
            b.append("back(");
            b.append(minCount);
            b.append(")");
            break;            
         case BACKTRACK_REG_MIN:
            b.append("back");
            b.append("_$");
            b.append(memreg);
            b.append("(");
            b.append(minCount);
            b.append(")");
            break;            
         case GROUP_IN:
            b.append('(');
            if(memreg>0)b.append(memreg);
            b.append('-');
            b.append(" , ");
            break;
         case GROUP_OUT:
            b.append('-');
            if(memreg>0)b.append(memreg);
            b.append(')');
            b.append(" , ");
            break;
         case PLOOKAHEAD_IN:
            b.append('(');
            b.append("=");
            b.append(lookaheadId);
            b.append(" , ");
            break;
         case PLOOKAHEAD_OUT:
            b.append('=');
            b.append(lookaheadId);
            b.append(')');
            b.append(" , ");
            break;
         case NLOOKAHEAD_IN:
            b.append("(!");
            b.append(lookaheadId);
            b.append(" , ");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;
         case NLOOKAHEAD_OUT:
            b.append('!');
            b.append(lookaheadId);
            b.append(')');
            b.append(" , ");
            break;
         case PLOOKBEHIND_IN:
            b.append('(');
            b.append("<=");
            b.append(lookaheadId);
            b.append(" , dist=");
            b.append(distance);
            b.append(" , ");
            break;
         case PLOOKBEHIND_OUT:
            b.append("<=");
            b.append(lookaheadId);
            b.append(')');
            b.append(" , ");
            break;
         case NLOOKBEHIND_IN:
            b.append("(<!");
            b.append(lookaheadId);
            b.append(" , dist=");
            b.append(distance);
            b.append(" , ");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;
         case NLOOKBEHIND_OUT:
            b.append("<!");
            b.append(lookaheadId);
            b.append(')');
            b.append(" , ");
            break;
         case MEMREG_CONDITION:
            b.append("(reg");
            b.append(memreg);
            b.append("?)");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;
         case LOOKAHEAD_CONDITION_IN:
            b.append("(cond");
            b.append(lookaheadId);
            b.append(((Lookahead)this).isPositive? '=': '!');
            b.append(" , ");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;
         case LOOKAHEAD_CONDITION_OUT:
            b.append("cond");
            b.append(lookaheadId);
            b.append(")");
            if(failNext!=null){
               b.append(", =>");
               b.append(failNext.instanceNum);
               b.append(", ");
            }
            break;
         case REG:
            b.append("$");
            b.append(memreg);
            b.append(", ");
            break;
         case SUCCESS:
            b.append("END");
            break;
         case BRANCH_STORE_CNT_AUX1:
            b.append("(aux1)");
         case BRANCH_STORE_CNT:
            b.append("(cnt)");
         case BRANCH:
            b.append("=>");
            if(failNext!=null) b.append(failNext.instanceNum);
            else b.append("null");
            b.append(" , ");
            break;
         default:
            b.append('[');
            switch(type){
               case CNT_SET_0:
                  b.append("cnt=0");
                  break;
               case CNT_INC:
                  b.append("cnt++");
                  break;
               case CNT_GT_EQ:
                  b.append("cnt>="+maxCount);
                  break;
               case READ_CNT_LT:
                  b.append("->cnt<"+maxCount);
                  break;
               case CRSTORE_CRINC:
                  b.append("M("+memreg+")->,Cr("+cntreg+")->,Cr("+cntreg+")++");
                  break;
               case CR_SET_0:
                  b.append("Cr("+cntreg+")=0");
                  break;
               case CR_LT:
                  b.append("Cr("+cntreg+")<"+maxCount);
                  break;
               case CR_GT_EQ:
                  b.append("Cr("+cntreg+")>="+maxCount);
                  break;
               default:
                  b.append("unknown type: "+type);
            }
            b.append("] , ");
      }
      if(next!=null){
         b.append("->");
         b.append(next.instanceNum);
         b.append(", ");
      }
      //b.append("\r\n");
      return b.toString();
   }
   
   public String toStringAll(){
      return toStringAll(new Vector());
   }
   
   public String toStringAll(Vector v){
      v.addElement(new Integer(instanceNum));
      String s=toString();
      if(next!=null){
         if(!v.contains(new Integer(next.instanceNum))){
            s+="\r\n";
            s+=next.toStringAll(v);
         }
      }
      if(failNext!=null){
         if(!v.contains(new Integer(failNext.instanceNum))){
            s+="\r\n";
            s+=failNext.toStringAll(v);
         }
      }
      return s;
   }
}

class Pretokenizer{
   private static final int START=1;
   static final int END=2;
   static final int PLAIN_GROUP=3;
   static final int POS_LOOKAHEAD=4;
   static final int NEG_LOOKAHEAD=5;
   static final int POS_LOOKBEHIND=6;
   static final int NEG_LOOKBEHIND=7;
   static final int INDEPENDENT_REGEX=8;
   static final int COMMENT=9;
   static final int CONDITIONAL_GROUP=10;
   static final int FLAGS=11;
   static final int CLASS_GROUP=12;
   static final int NAMED_GROUP=13;
   
   int tOffset,tOutside,skip;
   int offset,end;
   int c;
   
   int ttype=START;
   
   char[] data;
   
   //results
   private int flags;
   private boolean flagsChanged;
   
   char[] brackets;
   String groupName;
   boolean groupDeclared;
   
   Pretokenizer(char[] data,int offset,int end){
      if(offset<0 || end>data.length) throw new IndexOutOfBoundsException("offset="+offset+", end="+end+", length="+data.length);
      this.offset=offset;
      this.end=end;

      this.tOffset=offset;
      this.tOutside=offset;

      this.data=data;
   }
   
   int flags(int def){
      return flagsChanged? flags: def;
   }
   
   void next() throws PatternSyntaxException{
      int tOffset=this.tOutside;
      int skip=this.skip;
      
      tOffset+=skip;
      flagsChanged=false;
      
      int end=this.end; 
      char[] data=this.data; 
      boolean esc=false;
      for(int i=tOffset;i<end;i++){
         if(esc){
            esc=false;
            continue;
         }
         char c=data[i];
         switch(c){
            case '\\':
              esc=true;
              continue;
            case '|':
            case ')':
              ttype=c;
              this.tOffset=tOffset;
              this.tOutside=i;
              this.skip=1;
              return;
            case '(':
              if(((i+2)<end) && (data[i+1]=='?')){
              	  char c1=data[i+2];
              	  switch(c1){
              	     case ':':
              	     	  ttype=PLAIN_GROUP;
              	     	  skip=3; // "(?:" - skip 3 chars
              	     	  break;
              	     case '=':
              	     	  ttype=POS_LOOKAHEAD;
              	     	  skip=3;  // "(?="
              	     	  break;
              	     case '!':
              	     	  ttype=NEG_LOOKAHEAD;
              	     	  skip=3;  // "(?!"
              	     	  break;
              	     case '<':
              	        switch(c1=data[i+3]){
              	           case '=':
               	           ttype=POS_LOOKBEHIND;
               	           skip=4; // "(?<="
               	           break;
              	           case '!':
               	           ttype=NEG_LOOKBEHIND;
               	           skip=4; // "(?<!"
               	           break;
               	        default:
               	           throw new PatternSyntaxException("invalid character after '(?<' : "+c1);
              	        }
              	     	  break;
              	     case '>':
              	     	  ttype=INDEPENDENT_REGEX;
              	     	  skip=3;  // "(?>"
              	     	  break;
              	     case '#':
              	     	  ttype=COMMENT;
              	     	  skip=3; // ="(?#".length, the makeTree() skips the rest by itself
              	     	  break;
              	     case '(':
              	     	  ttype=CONDITIONAL_GROUP;
              	     	  skip=2; //"(?"+"(..." - skip "(?" (2 chars) and parse condition as a group
              	     	  break;
              	     case '[':
              	     	  ttype=CLASS_GROUP;
              	     	  skip=2; // "(?"+"[..]+...-...&...)" - skip 2 chars and parse a class group
              	     	  break;
              	     default:
              	        int mOff,mLen;
              	        mLoop:
              	        for(int p=i+2;p<end;p++){
              	           char c2=data[p];
              	           switch(c2){
              	              case '-':
              	              case 'i':
              	              case 'm':
              	              case 's':
              	              case 'x':
              	              case 'u':
              	              case 'X':
//System.out.println("case '+-imsxuX' ("+c2+")");
              	                 continue mLoop;
              	                 
              	              case ':':
              	                 mOff=i+2;
              	                 mLen=p-mOff;
                               if(mLen>0){
                                  flags=Pattern.parseFlags(data,mOff,mLen);
                                  flagsChanged=true;
              	                 }
              	                 ttype=PLAIN_GROUP;
              	                 skip=mLen+3; // "(?imsx:" mLen=4; skip= "(?".len + ":".len + mLen = 2+1+4=7
              	                 break mLoop;
              	              case ')':
              	                 flags=Pattern.parseFlags(data,mOff=(i+2),mLen=(p-mOff));
              	                 flagsChanged=true;
              	                 ttype=FLAGS;
              	                 skip=mLen+3; // "(?imsx)" mLen=4, skip="(?".len+")".len+mLen=2+1+4=7
              	                 break mLoop;
              	              default:
              	                 throw new PatternSyntaxException("wrong char after \"(?\": "+c2);
              	           }
              	        }
              	        break;
              	  }
              }
              else if(((i+2)<end) && (data[i+1]=='{')){ //parse named group: ({name}....),({=name}....)
                 int p=i+2;
                 skip=3; //'({' + '}'
                 int nstart,nend;
                 boolean isDecl;
                 c=data[p];
//System.out.println("NG: p="+p+", c="+c);
                 while(Character.isWhitespace(c)){
                    c=data[++p];
                    skip++;
                    if(p==end)throw new PatternSyntaxException("malformed named group");
                 }
                 
                 if(c=='='){
                    isDecl=false;
                    c=data[++p];
                    skip++;
                    if(p==end)throw new PatternSyntaxException("malformed named group");
                 }
                 else isDecl=true;
                 
                 nstart=p;
                 while(Character.isJavaIdentifierPart(c)){
                    c=data[++p];
                    skip++;
                    if(p==end)throw new PatternSyntaxException("malformed named group");
                 }
                 nend=p;
                 while(Character.isWhitespace(c)){
                    c=data[++p];
                    skip++;
                    if(p==end)throw new PatternSyntaxException("malformed named group");
                 }
                 if(c!='}') throw new PatternSyntaxException("'}' expected at "+(p-i)+" in "+new String(data,i,end-i));
                 
                 this.groupName=new String(data,nstart,nend-nstart);
                 this.groupDeclared=isDecl;
                 ttype=NAMED_GROUP;
              }
              else{
                 ttype='(';
                 skip=1;
              }
              this.tOffset=tOffset;
              this.tOutside=i;
              this.skip=skip;
              return;
            case '[':
              loop:
              for(;;i++){
                 if(i==end) throw new PatternSyntaxException("malformed character class");
                 char c1=data[i];
                 switch(c1){
                    case '\\':
                       i++;
                       continue;
                    case ']':
                       break loop;
                 }
              }
         }
      }
      ttype=END;
      this.tOffset=tOffset;
      this.tOutside=end;
   }

}

class Branch extends Term{
   Branch(){
      type=BRANCH;
   }

   Branch(int type){
      switch(type){
         case BRANCH:
         case BRANCH_STORE_CNT:
         case BRANCH_STORE_CNT_AUX1:
            this.type=type;
            break;
         default:
            throw new IllegalArgumentException("not a branch type: "+type);
      }
   }
}

class BackReference extends Term{
   BackReference(int no,boolean icase){
      super(icase? REG_I: REG);
      memreg=no;
   }
}

class Group extends Term{
   Group(){
      this(0);
   }
   
   Group(int memreg){
      type=GROUP_IN;
      this.memreg=memreg;
      
      //used in append()
      current=null;
      in=this;
      prev=null;
      
      out=new Term();
      out.type=GROUP_OUT;
      out.memreg=memreg;
   }
}

class ConditionalExpr extends Group{
   protected Term node;
   protected boolean newBranchStarted=false;
   protected boolean linkAsBranch=true;
   
   ConditionalExpr(Lookahead la){
      super(0);
//System.out.println("ConditionalExpr("+la+")");
      /*
      * This all is rather tricky.
      * See how this types are handled in Matcher.
      * The shortcoming is that we strongly rely upon 
      * the internal structure of Lookahead.
      */
      la.in.type=LOOKAHEAD_CONDITION_IN;
      la.out.type=LOOKAHEAD_CONDITION_OUT;
      if(la.isPositive){
         node=la.in;
         linkAsBranch=true;
         
         //empty 2'nd branch
         node.failNext=out;
      }
      else{
         node=la.out;
         linkAsBranch=false;
         
         //empty 2'nd branch
         node.next=out;
      }
      
      //node.prev=in;
      //in.next=node;
      
      la.prev=in;
      in.next=la;
      
      current=la;
      //current=node;
   }
   
   ConditionalExpr(Lookbehind lb){
      super(0);
//System.out.println("ConditionalExpr("+la+")");
      /*
      * This all is rather tricky.
      * See how this types are handled in Matcher.
      * The shortcoming is that we strongly rely upon 
      * the internal structure of Lookahead.
      */
      lb.in.type=LOOKBEHIND_CONDITION_IN;
      lb.out.type=LOOKBEHIND_CONDITION_OUT;
      if(lb.isPositive){
         node=lb.in;
         linkAsBranch=true;
         
         //empty 2'nd branch
         node.failNext=out;
      }
      else{
         node=lb.out;
         linkAsBranch=false;
         
         //empty 2'nd branch
         node.next=out;
      }
      
      lb.prev=in;
      in.next=lb;
      
      current=lb;
      //current=node;
   }
   
   ConditionalExpr(int memreg){
      super(0);
//System.out.println("ConditionalExpr("+memreg+")");
      Term condition=new Term(MEMREG_CONDITION);
      condition.memreg=memreg;
      condition.out=condition;
      condition.out1=null;
      condition.branchOut=null;
      
      //default branch
      condition.failNext=out;
      
      node=current=condition;
      linkAsBranch=true;
      
      condition.prev=in;
      in.next=condition;
      
      current=condition;
   }
   
   protected void startNewBranch() throws PatternSyntaxException{
      if(newBranchStarted) throw new PatternSyntaxException("attempt to set a 3'd choice in a conditional expr.");
      Term node=this.node;
      node.out1=null;
      if(linkAsBranch){
         node.out=null;
         node.branchOut=node;
      }
      else{
         node.out=node;
         node.branchOut=null;
      }
      newBranchStarted=true;
//System.out.println("CondGrp.startNewBranch(): current="+current+", this="+this.toStringAll());
      current=node;
   }
}

class IndependentGroup extends Term{
   IndependentGroup(int id){
      super(0);
      in=this;
      out=new Term();
      type=INDEPENDENT_IN;
      out.type=INDEPENDENT_OUT;
      lookaheadId=out.lookaheadId=id;
   }
}

class Lookahead extends Term{
   final boolean isPositive;
   
   Lookahead(int id,boolean isPositive){
      this.isPositive=isPositive;
      in=this;
      out=new Term();
      if(isPositive){
         type=PLOOKAHEAD_IN;
         out.type=PLOOKAHEAD_OUT;
      }
      else{
         type=NLOOKAHEAD_IN;
         out.type=NLOOKAHEAD_OUT;
         branchOut=this; 
      }
      lookaheadId=id;
      out.lookaheadId=id;
   }
}

class Lookbehind extends Term{
   final boolean isPositive;
   private int prevDistance=-1;
   
   Lookbehind(int id,boolean isPositive){
      distance=0;
      this.isPositive=isPositive;
      in=this;
      out=new Term();
      if(isPositive){
         type=PLOOKBEHIND_IN;
         out.type=PLOOKBEHIND_OUT;
      }
      else{
         type=NLOOKBEHIND_IN;
         out.type=NLOOKBEHIND_OUT;
         branchOut=this; 
      }
      lookaheadId=id;
      out.lookaheadId=id;
   }
   
   protected Term append(Term t) throws PatternSyntaxException{
      distance+=length(t);
      return super.append(t);
   }
   
   protected Term replaceCurrent(Term t) throws PatternSyntaxException{
      distance+=length(t)-length(current);
      return super.replaceCurrent(t);
   }
   
   private static int length(Term t) throws PatternSyntaxException{
      int type=t.type;
      switch(type){
         case CHAR:
         case BITSET:
         case BITSET2:
         case ANY_CHAR:
         case ANY_CHAR_NE:
            return 1;
         case BOUNDARY:
         case DIRECTION:
         case UBOUNDARY:
         case UDIRECTION:
            return 0;
         default:
            if(type>=FIRST_TRANSPARENT && type<=LAST_TRANSPARENT) return 0;
            throw new PatternSyntaxException("variable length element within a lookbehind assertion");
      }
   }
   
   protected void startNewBranch() throws PatternSyntaxException{
      prevDistance=distance;
      distance=0;
      super.startNewBranch();
   }
   
   protected void close() throws PatternSyntaxException{
      int pd=prevDistance;
      if(pd>=0){
         if(distance!=pd) throw new PatternSyntaxException("non-equal branch lengths within a lookbehind assertion");
      }
      super.close();
   }
}

class Iterator extends Term{
   
   Iterator(Term term,int min,int max,Vector collection) throws PatternSyntaxException{
      collection.addElement(this);
      switch(term.type){
         case CHAR:
         case ANY_CHAR:
         case ANY_CHAR_NE:
         case BITSET:
         case BITSET2:{
            target=term;
            Term back=new Term();
            if(min<=0 && max<0){
               type=REPEAT_0_INF;
               back.type=BACKTRACK_0;
            }
            else if(min>0 && max<0){
               type=REPEAT_MIN_INF;
               back.type=BACKTRACK_MIN;
               minCount=back.minCount=min;
            }
            else{
               type=REPEAT_MIN_MAX;
               back.type=BACKTRACK_MIN;
               minCount=back.minCount=min;
               maxCount=max;
            }
            
            failNext=back;
            
            in=this;
            out=this;
            out1=back;
            branchOut=null;   
            return;
         }
         case REG:{
            target=term;
            memreg=term.memreg;
            Term back=new Term();
            if(max<0){
               type=REPEAT_REG_MIN_INF;
               back.type=BACKTRACK_REG_MIN;
               minCount=back.minCount=min;
            }
            else{
               type=REPEAT_REG_MIN_MAX;
               back.type=BACKTRACK_REG_MIN;
               minCount=back.minCount=min;
               maxCount=max;
            }
            
            failNext=back;
            
            in=this;
            out=this;
            out1=back;
            branchOut=null;   
            return; 
         }
         default:
            throw new PatternSyntaxException("can't iterate this type: "+term.type);
      }
   }
   
   void optimize(){
//System.out.println("optimizing myself: "+this);
//BACKTRACK_MIN_REG_FIND
      Term back=failNext;
      Optimizer opt=Optimizer.find(back.next);
      if(opt==null) return;
      failNext=opt.makeBacktrack(back);
   }
}
