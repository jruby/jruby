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

class CharacterClass extends Term implements UnicodeConstants{
   static final Bitset DIGIT=new Bitset();
   static final Bitset WORDCHAR=new Bitset();
   static final Bitset SPACE=new Bitset();
   
   static final Bitset UDIGIT=new Bitset();
   static final Bitset UWORDCHAR=new Bitset();
   static final Bitset USPACE=new Bitset();
   
   static final Bitset NONDIGIT=new Bitset();
   static final Bitset NONWORDCHAR=new Bitset();
   static final Bitset NONSPACE=new Bitset();
   
   static final Bitset UNONDIGIT=new Bitset();
   static final Bitset UNONWORDCHAR=new Bitset();
   static final Bitset UNONSPACE=new Bitset();
   
   private static boolean namesInitialized=false;
   
   static final Hashtable namedClasses=new Hashtable();
   static final Vector unicodeBlocks=new Vector();
   static final Vector posixClasses=new Vector();
   static final Vector unicodeCategories=new Vector();
   
   //modes; used in parseGroup(()
   private final static int ADD=1;
   private final static int SUBTRACT=2;
   private final static int INTERSECT=3;
   
   private static final String blockData=
     "0000..007F:InBasicLatin;0080..00FF:InLatin-1Supplement;0100..017F:InLatinExtended-A;"
    +"0180..024F:InLatinExtended-B;0250..02AF:InIPAExtensions;02B0..02FF:InSpacingModifierLetters;"
    +"0300..036F:InCombiningDiacriticalMarks;0370..03FF:InGreek;0400..04FF:InCyrillic;0530..058F:InArmenian;"
    +"0590..05FF:InHebrew;0600..06FF:InArabic;0700..074F:InSyriac;0780..07BF:InThaana;0900..097F:InDevanagari;"
    +"0980..09FF:InBengali;0A00..0A7F:InGurmukhi;0A80..0AFF:InGujarati;0B00..0B7F:InOriya;0B80..0BFF:InTamil;"
    +"0C00..0C7F:InTelugu;0C80..0CFF:InKannada;0D00..0D7F:InMalayalam;0D80..0DFF:InSinhala;0E00..0E7F:InThai;"
    +"0E80..0EFF:InLao;0F00..0FFF:InTibetan;1000..109F:InMyanmar;10A0..10FF:InGeorgian;1100..11FF:InHangulJamo;"
    +"1200..137F:InEthiopic;13A0..13FF:InCherokee;1400..167F:InUnifiedCanadianAboriginalSyllabics;"
    +"1680..169F:InOgham;16A0..16FF:InRunic;1780..17FF:InKhmer;1800..18AF:InMongolian;"
    +"1E00..1EFF:InLatinExtendedAdditional;1F00..1FFF:InGreekExtended;2000..206F:InGeneralPunctuation;"
    +"2070..209F:InSuperscriptsAndSubscripts;20A0..20CF:InCurrencySymbols;"
    +"20D0..20FF:InCombiningMarksForSymbols;2100..214F:InLetterLikeSymbols;2150..218F:InNumberForms;"
    +"2190..21FF:InArrows;2200..22FF:InMathematicalOperators;2300..23FF:InMiscellaneousTechnical;"
    +"2400..243F:InControlPictures;2440..245F:InOpticalCharacterRecognition;"
    +"2460..24FF:InEnclosedAlphanumerics;2500..257F:InBoxDrawing;2580..259F:InBlockElements;"
    +"25A0..25FF:InGeometricShapes;2600..26FF:InMiscellaneousSymbols;2700..27BF:InDingbats;"
    +"2800..28FF:InBraillePatterns;2E80..2EFF:InCJKRadicalsSupplement;2F00..2FDF:InKangxiRadicals;"
    +"2FF0..2FFF:InIdeographicDescriptionCharacters;3000..303F:InCJKSymbolsAndPunctuation;"
    +"3040..309F:InHiragana;30A0..30FF:InKatakana;3100..312F:InBopomofo;3130..318F:InHangulCompatibilityJamo;"
    +"3190..319F:InKanbun;31A0..31BF:InBopomofoExtended;3200..32FF:InEnclosedCJKLettersAndMonths;"
    +"3300..33FF:InCJKCompatibility;3400..4DB5:InCJKUnifiedIdeographsExtensionA;"
    +"4E00..9FFF:InCJKUnifiedIdeographs;A000..A48F:InYiSyllables;A490..A4CF:InYiRadicals;"
    +"AC00..D7A3:InHangulSyllables;D800..DB7F:InHighSurrogates;DB80..DBFF:InHighPrivateUseSurrogates;"
    +"DC00..DFFF:InLowSurrogates;E000..F8FF:InPrivateUse;F900..FAFF:InCJKCompatibilityIdeographs;"
    +"FB00..FB4F:InAlphabeticPresentationForms;FB50..FDFF:InArabicPresentationForms-A;"
    +"FE20..FE2F:InCombiningHalfMarks;FE30..FE4F:InCJKCompatibilityForms;FE50..FE6F:InSmallFormVariants;"
    +"FE70..FEFE:InArabicPresentationForms-B;FEFF..FEFF:InSpecials;FF00..FFEF:InHalfWidthAndFullWidthForms;"
    +"FFF0..FFFD:InSpecials";
   
   static{
      //*
      DIGIT.setDigit(false);
      WORDCHAR.setWordChar(false);
      SPACE.setSpace(false);
      
      UDIGIT.setDigit(true);
      UWORDCHAR.setWordChar(true);
      USPACE.setSpace(true);
      
      NONDIGIT.setDigit(false); NONDIGIT.setPositive(false); 
      NONWORDCHAR.setWordChar(false); NONWORDCHAR.setPositive(false); 
      NONSPACE.setSpace(false); NONSPACE.setPositive(false); 
      
      UNONDIGIT.setDigit(true); UNONDIGIT.setPositive(false);
      UNONWORDCHAR.setWordChar(true); UNONWORDCHAR.setPositive(false);
      UNONSPACE.setSpace(true); UNONSPACE.setPositive(false);
      
      initPosixClasses();
   }
   
   private static void registerClass(String name,Bitset cls,Vector realm){
      namedClasses.put(name,cls);
      if(!realm.contains(name))realm.addElement(name);
   }
   
   private static void initPosixClasses(){
      Bitset lower=new Bitset();
         lower.setRange('a','z');
         registerClass("Lower",lower,posixClasses);
      Bitset upper=new Bitset();
         upper.setRange('A','Z');
         registerClass("Upper",upper,posixClasses);
      Bitset ascii=new Bitset();
         ascii.setRange((char)0,(char)0x7f);
         registerClass("ASCII",ascii,posixClasses);
      Bitset alpha=new Bitset();
         alpha.add(lower);
         alpha.add(upper);
         registerClass("Alpha",alpha,posixClasses);
      Bitset digit=new Bitset();
         digit.setRange('0','9');
         registerClass("Digit",digit,posixClasses);
      Bitset alnum=new Bitset();
         alnum.add(alpha);
         alnum.add(digit);
         registerClass("Alnum",alnum,posixClasses);
      Bitset punct=new Bitset();
         punct.setChars("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
         registerClass("Punct",punct,posixClasses);
      Bitset graph=new Bitset();
         graph.add(alnum);
         graph.add(punct);
         registerClass("Graph",graph,posixClasses);
         registerClass("Print",graph,posixClasses);
      Bitset blank=new Bitset();
         blank.setChars(" \t");
         registerClass("Blank",blank,posixClasses);
      Bitset cntrl=new Bitset();
         cntrl.setRange((char)0,(char)0x1f);
         cntrl.setChar((char)0x7f);
         registerClass("Cntrl",cntrl,posixClasses);
      Bitset xdigit=new Bitset();
         xdigit.setRange('0','9');
         xdigit.setRange('a','f');
         xdigit.setRange('A','F');
         registerClass("XDigit",xdigit,posixClasses);
      Bitset space=new Bitset();
         space.setChars(" \t\n\r\f\u000b");
         registerClass("Space",space,posixClasses);
   }
   
   private static void initNames(){
      initNamedCategory("C",new int[]{Cn,Cc,Cf,Co,Cs}); 
      initNamedCategory("Cn",Cn); 
      initNamedCategory("Cc",Cc);
      initNamedCategory("Cf",Cf);
      initNamedCategory("Co",Co);
      initNamedCategory("Cs",Cs);
      
      initNamedCategory("L",new int[]{Lu,Ll,Lt,Lm,Lo}); 
      initNamedCategory("Lu",Lu);
      initNamedCategory("Ll",Ll);
      initNamedCategory("Lt",Lt);
      initNamedCategory("Lm",Lm);
      initNamedCategory("Lo",Lo);
      
      initNamedCategory("M",new int[]{Mn,Me,Mc}); 
      initNamedCategory("Mn",Mn);
      initNamedCategory("Me",Me);
      initNamedCategory("Mc",Mc);
      
      initNamedCategory("N",new int[]{Nd,Nl,No}); 
      initNamedCategory("Nd",Nd);
      initNamedCategory("Nl",Nl);
      initNamedCategory("No",No);
      
      initNamedCategory("Z",new int[]{Zs,Zl,Zp}); 
      initNamedCategory("Zs",Zs);
      initNamedCategory("Zl",Zl);
      initNamedCategory("Zp",Zp);
      
      initNamedCategory("P",new int[]{Pd,Ps,Pi,Pe,Pf,Pc,Po}); 
      initNamedCategory("Pd",Pd);
      initNamedCategory("Ps",Ps);
      initNamedCategory("Pi",Pi);
      initNamedCategory("Pe",Pe);
      initNamedCategory("Pf",Pf);
      initNamedCategory("Pc",Pc);
      initNamedCategory("Po",Po);
      
      initNamedCategory("S",new int[]{Sm,Sc,Sk,So}); 
      initNamedCategory("Sm",Sm);
      initNamedCategory("Sc",Sc);
      initNamedCategory("Sk",Sk);
      initNamedCategory("So",So);
      
      Bitset bs=new Bitset();
      bs.setCategory(Cn);
      registerClass("UNASSIGNED",bs,unicodeCategories);
      bs=new Bitset();
      bs.setCategory(Cn);
      bs.setPositive(false);
      registerClass("ASSIGNED",bs,unicodeCategories);
      
      StringTokenizer st=new StringTokenizer(blockData,".,:;");
      while(st.hasMoreTokens()){
         try{
            int first=Integer.parseInt(st.nextToken(),16);
            int last=Integer.parseInt(st.nextToken(),16);
            String name=st.nextToken();
            initNamedBlock(name,first,last);
         }
         catch(Exception e){
            e.printStackTrace();
         }
      }
      
      initNamedBlock("ALL",0,0xffff);
      
      namesInitialized=true;
      //*/
   }
   
   private static void initNamedBlock(String name,int first,int last){
      if(first<Character.MIN_VALUE || first>Character.MAX_VALUE) throw new IllegalArgumentException("wrong start code ("+first+") in block "+name);
      if(last<Character.MIN_VALUE || last>Character.MAX_VALUE) throw new IllegalArgumentException("wrong end code ("+last+") in block "+name);
      if(last<first) throw new IllegalArgumentException("end code < start code in block "+name);
      Bitset bs=(Bitset)namedClasses.get(name);
      if(bs==null){
         bs=new Bitset();
         registerClass(name,bs,unicodeBlocks);
      }
      bs.setRange((char)first,(char)last);
   }
   
   private static void initNamedCategory(String name,int cat){
      Bitset bs=new Bitset();
      bs.setCategory(cat);
      registerClass(name,bs,unicodeCategories);
   }
   
   private static void initNamedCategory(String name,int[] cats){
      Bitset bs=new Bitset();
      for(int i=0;i<cats.length;i++){
         bs.setCategory(cats[i]);
      }
      namedClasses.put(name,bs);
   }
   
   private static Bitset getNamedClass(String name){
      if(!namesInitialized)initNames();
      return (Bitset)namedClasses.get(name);
   }
   
   static void makeICase(Term term,char c){
      Bitset bs=new Bitset();
      bs.setChar(Character.toLowerCase(c));
      bs.setChar(Character.toUpperCase(c));
      bs.setChar(Character.toTitleCase(c));
      Bitset.unify(bs,term);
   }
   
   static void makeDigit(Term term,boolean inverse,boolean unicode){
      Bitset digit=unicode? inverse? UNONDIGIT: UDIGIT : 
                            inverse? NONDIGIT: DIGIT ;
      Bitset.unify(digit,term);
   }
   
   static void makeSpace(Term term,boolean inverse,boolean unicode){
      Bitset space=unicode? inverse? UNONSPACE: USPACE : 
                            inverse? NONSPACE: SPACE ;
      Bitset.unify(space,term);
   }
   
   static void makeWordChar(Term term,boolean inverse,boolean unicode){
      Bitset wordChar=unicode? inverse? UNONWORDCHAR: UWORDCHAR: 
                               inverse? NONWORDCHAR: WORDCHAR ;
      Bitset.unify(wordChar,term);
   }
   
   static void makeWordBoundary(Term term,boolean inverse,boolean unicode){
      makeWordChar(term,inverse,unicode);
      term.type=unicode? UBOUNDARY: BOUNDARY;
   }
   
   static void makeWordStart(Term term,boolean unicode){
      makeWordChar(term,false,unicode);
      term.type=unicode? UDIRECTION: DIRECTION;
   }
   
   static void makeWordEnd(Term term,boolean unicode){
      makeWordChar(term,true,unicode);
      term.type=unicode? UDIRECTION: DIRECTION;
   }
   
   final static void parseGroup(char[] data,int i,int out,Term term,boolean icase,boolean skipspaces,
                               boolean unicode,boolean xml) throws PatternSyntaxException{
      Bitset sum=new Bitset();
      Bitset bs=new Bitset();
      int mode=ADD;
      char c;
      for(;i<out;){
         switch(c=data[i++]){
            case '+':
               mode=ADD;
               continue;
            case '-':
               mode=SUBTRACT;
               continue;
            case '&':
               mode=INTERSECT;
               continue;
            case '[':
               bs.reset();
               i=parseClass(data,i,out,bs,icase,skipspaces,unicode,xml);
               switch(mode){
                  case ADD:
                     sum.add(bs);
                     break;
                  case SUBTRACT:
                     sum.subtract(bs);
                     break;
                  case INTERSECT:
                     sum.intersect(bs);
                     break;
               }
               continue;
            case ')':
               throw new  PatternSyntaxException("unbalanced class group");
         }
      }
      Bitset.unify(sum,term);
   }
   
   final static int parseClass(char[] data,int i,int out,Term term,boolean icase,boolean skipspaces,
                               boolean unicode,boolean xml) throws PatternSyntaxException{
      Bitset bs=new Bitset();
      i=parseClass(data,i,out,bs,icase,skipspaces,unicode,xml);
      Bitset.unify(bs,term);
      return i;
   }
   
   final static int parseName(char[] data,int i,int out,Term term, boolean inverse,
                              boolean skipspaces) throws PatternSyntaxException{
      StringBuffer sb=new StringBuffer();
      i=parseName(data,i,out,sb,skipspaces);
      Bitset bs=getNamedClass(sb.toString());
      if(bs==null) throw new PatternSyntaxException("unknow class: {"+sb+"}");
      Bitset.unify(bs,term);
      term.inverse=inverse;
      return i;
   }
   
   /*
   * @param mode add/subtract
   */
   private final static int parseClass(char[] data,int i,int out,Bitset bs,
                                       boolean icase,boolean skipspaces,
                                       boolean unicode,boolean xml) throws PatternSyntaxException{
//System.out.println("parseClass("+new String(data)+","+i+","+out+",....)");
      char c;
      int prev=-1;
      boolean isFirst=true,setFirst=false,inRange=false;
      Bitset bs1=null;
      StringBuffer sb=null;
      for(;i<out;isFirst=setFirst,setFirst=false){
//System.out.println("   c="+data[i]);
         handle_special: switch(c=data[i++]){
            case ']':
               //if(inRange) throw new PatternSyntaxException("[...-] is illegal");
               if(isFirst) break; //treat as normal char
               if(inRange){
                  bs.setChar('-');
               }
               if(prev>=0){
                  char c1=(char)prev;
                  if(icase){
                     bs.setChar(Character.toLowerCase(c1));
                     bs.setChar(Character.toUpperCase(c1));
                     bs.setChar(Character.toTitleCase(c1));
                  }
                  else bs.setChar(c1);
               }
               return i;
               
            case '-':
               if(isFirst) break;
               //if(isFirst) throw new PatternSyntaxException("[-...] is illegal");
               if(inRange) break;
               //if(inRange) throw new PatternSyntaxException("[...--...] is illegal");
               inRange=true;
               continue;
               
            case '[':
               if(inRange && xml){ //[..-[..]]
                  if(prev>=0) bs.setChar((char)prev);
                  if(bs1==null) bs1=new Bitset();
                  else bs1.reset();
                  i=parseClass(data,i,out,bs1,icase,skipspaces,unicode,xml);
//System.out.println("     i="+i);
                  bs.subtract(bs1);
                  inRange=false;
                  prev=-1;
                  continue;
               }
               else break handle_special;
               
            case '^':
               //if(!isFirst) throw new PatternSyntaxException("'^' isn't a first char in a class def");
               //bs.setPositive(false);
               //setFirst=true;
               //continue;
               if(isFirst){
                  bs.setPositive(false);
                  setFirst=true;
                  continue;
               }
               //treat as normal char
               break;
               
            case ' ':
            case '\r':
            case '\n':
            case '\t':
            case '\f':
               if(skipspaces) continue;
               else break handle_special;
            case '\\':
               Bitset negatigeClass=null;
               boolean inv=false;
               handle_escape: switch(c=data[i++]){
                  case 'r':
                     c='\r';
                     break handle_special;
                     
                  case 'n':
                     c='\n';
                     break handle_special;
                     
                  case 'e':
                     c='\u001B';
                     break handle_special;
                     
                  case 't':
                     c='\t';
                     break handle_special;
                     
                  case 'f':
                     c='\f';
                     break handle_special;
                     
                  case 'u':
                     if(i>=out-4) throw  new PatternSyntaxException("incomplete escape sequence \\uXXXX");
                     c=(char)((toHexDigit(c)<<12)
                             +(toHexDigit(data[i++])<<8)
                             +(toHexDigit(data[i++])<<4)
                             +toHexDigit(data[i++]));
                     break handle_special;
                     
                  case 'v':
                     c=(char)((toHexDigit(c)<<24)+
                              (toHexDigit(data[i++])<<16)+
                              (toHexDigit(data[i++])<<12)+
                              (toHexDigit(data[i++])<<8)+
                              (toHexDigit(data[i++])<<4)+
                               toHexDigit(data[i++]));
                     break handle_special;
                     
                  case 'b':
                     c=8; // backspace
                     break handle_special;

                  case 'x':{   // hex 2-digit number
                     int hex=0;
                     char d;
                     if((d=data[i++])=='{'){
                        while((d=data[i++])!='}'){
                           hex=(hex<<4)+toHexDigit(d);
                        }
                        if(hex>0xffff) throw new PatternSyntaxException("\\x{<out of range>}");
                     }
                     else{
                        hex=(toHexDigit(d)<<4)+toHexDigit(data[i++]);
                     }
                     c=(char)hex;
                     break handle_special;
                  }
                  case '0':   // oct 2- or 3-digit number
                  case 'o':   // oct 2- or 3-digit number
                     int oct=0;
                     for(;;){
                        char d=data[i++];
                        if(d>='0' && d<='7'){
                           oct*=8;
                           oct+=d-'0';
                           if(oct>0xffff) break;
                        }
                        else {
                            i--;
                            break;
                        }
                     }
                     c=(char)oct;
                     break handle_special;
                     
                  case 'm':   // decimal number -> char
                     int dec=0;
                     for(;;){
                        char d=data[i++];
                        if(d>='0' && d<='9'){
                           dec*=10;
                           dec+=d-'0';
                           if(dec>0xffff) break;
                        } else {
                            i--;
                            break;
                        }
                     }
                     c=(char)dec;
                     break handle_special;
                     
                  case 'c':   // ctrl-char
                     c=(char)(data[i++]&0x1f);
                     break handle_special;
                  
                  //classes;
                  //
                  case 'D':   // non-digit
                     negatigeClass=unicode? UNONDIGIT: NONDIGIT;
                     break handle_escape;
                     
                  case 'S':   // space
                     negatigeClass=unicode? UNONSPACE: NONSPACE;
                     break handle_escape;
                     
                  case 'W':   // space
                     negatigeClass=unicode? UNONWORDCHAR: NONWORDCHAR;
                     break handle_escape;
                     
                  case 'd':   // digit
                     if(inRange) throw new PatternSyntaxException("illegal range: [..."+prev+"-\\d...]");
                     bs.setDigit(unicode);
                     continue;
                     
                  case 's':   // digit
                     if(inRange) throw new PatternSyntaxException("illegal range: [..."+prev+"-\\s...]");
                     bs.setSpace(unicode);
                     continue;
                     
                  case 'w':   // digit
                     if(inRange) throw new PatternSyntaxException("illegal range: [..."+prev+"-\\w...]");
                     bs.setWordChar(unicode);
                     continue;
                     
                  case 'P':   // \\P{..}
                     inv=true;
                  case 'p':   // \\p{..}
                     if(inRange) throw new PatternSyntaxException("illegal range: [..."+prev+"-\\w...]");
                     if(sb==null) sb=new StringBuffer();
                     else sb.setLength(0);
                     i=parseName(data,i,out,sb,skipspaces);
                     Bitset nc=getNamedClass(sb.toString());
                     if(nc==null) throw new PatternSyntaxException("unknown named class: {"+sb+"}");
                     bs.add(nc,inv);
                     continue;
                     
                  default:
                     //other escaped treat as normal
                     break handle_special;
               }
               //negatigeClass;
               //\S,\D,\W
               if(inRange) throw new PatternSyntaxException("illegal range: [..."+prev+"-\\"+c+"...]");
               bs.add(negatigeClass);
               continue;
               /* should probably not be here...
            case '{':   //
               if(inRange) throw new PatternSyntaxException("illegal range: [..."+prev+"-\\w...]");
               if(sb==null) sb=new StringBuffer();
               else sb.setLength(0);
               i=parseName(data,i-1,out,sb,skipspaces);
               Bitset nc=getNamedClass(sb.toString());
               if(nc==null) throw new PatternSyntaxException("unknown named class: {"+sb+"}");
               bs.add(nc,false);
               continue;
               */
            default:
         }
         //c is a normal char
          //System.out.println("      normal c="+c+", inRange="+inRange+", prev="+(char)prev);
         if(prev<0){
            prev=c;
            inRange=false;
            continue;
         }
         if(!inRange){
            char c1=(char)prev;
            if(icase){
               bs.setChar(Character.toLowerCase(c1));
               bs.setChar(Character.toUpperCase(c1));
               bs.setChar(Character.toTitleCase(c1));
            }
            else bs.setChar(c1);
            prev=c;
         }
         else{
            if(prev>c) throw new PatternSyntaxException("illegal range: "+prev+">"+c);
            char c0=(char)prev;
            inRange=false;
            prev=-1;
            if(icase){
               bs.setRange(Character.toLowerCase(c0),Character.toLowerCase(c));
               bs.setRange(Character.toUpperCase(c0),Character.toUpperCase(c));
               bs.setRange(Character.toTitleCase(c0),Character.toTitleCase(c));
            }
            else bs.setRange(c0,c);
         }
      }
      throw new PatternSyntaxException("unbalanced brackets in a class def");
   }
   
      
   final static int parseName(char[] data,int i,int out,StringBuffer sb,
                              boolean skipspaces) throws PatternSyntaxException{
      char c;
      int start=-1;
      while(i<out){
         switch(c=data[i++]){
            case '{':
               start=i;
               continue;
            case '}':
               return i;
            case ' ':
            case '\r':
            case '\n':
            case '\t':
            case '\f':
               if(skipspaces) continue;
               //else pass on
            default:
               if(start<0)throw new PatternSyntaxException("named class doesn't start with '{'");
               sb.append(c);
         }
      }
      throw new PatternSyntaxException("wrong class name: "+new String(data,i,out-i));
   }
   
   static String stringValue0(boolean[] arr){
/*
System.out.println("stringValue0():");
System.out.println("arr="+arr);
for(int i=0;i<BLOCK_SIZE;i++){
   if(arr[i]) if(i>32 && i<127)System.out.print((char)i); else System.out.print("["+i+"]");
}
System.out.println();
*/
      StringBuffer b=new StringBuffer();
      int c=0;
      
      loop: for(;;){
         while(!arr[c]){
//System.out.println(c+": "+arr[c]);
            c++;
            if(c>=0xff) break loop;
         }      
         int first=c;
         while(arr[c]){
//System.out.println(c+": "+arr[c]);
            c++;
            if(c>0xff) break;
         }     
         int last=c-1;
         if(last==first) b.append(stringValue(last));
         else{
            b.append(stringValue(first));
            b.append('-');
            b.append(stringValue(last));
         }
         if(c>0xff) break;
      }
      return b.toString();
   }
   
   /* Mmm.. what is it? 
   static String stringValueC(boolean[] categories){
      StringBuffer sb=new StringBuffer();
      for(int i=0;i<categories.length;i++){
         if(!categories[i]) continue;
         String name=(String)unicodeCategoryNames.get(new Integer(i));
         sb.append('{');
         sb.append(name);
         sb.append('}');
      }
      return sb.toString();
   }
   */
   
   static String stringValue2(boolean[][] arr){
      StringBuffer b=new StringBuffer();
      int c=0;
      loop: for(;;){
         boolean marked=false;
         for(;;){
            boolean[] marks=arr[c>>8];
            if(marks!=null && marks[c&255]) break;
            c++;
            if(c>0xffff) break loop;
         }
         int first=c;
         for(;c<=0xffff;){
            boolean[] marks=arr[c>>8];
            if(marks==null || !marks[c&255]) break;
            c++;
         }
         int last=c-1;
         if(last==first) b.append(stringValue(last));
         else{
            b.append(stringValue(first));
            b.append('-');
            b.append(stringValue(last));
         }
         if(c>0xffff) break;
      }
      return b.toString();
   }
   
   static String stringValue(int c){
      StringBuffer b=new StringBuffer(5);
      if(c<32){
         switch(c){
            case '\r':
               b.append("\\r");
               break;
            case '\n':
               b.append("\\n");
               break;
            case '\t':
               b.append("\\t");
               break;
            case '\f':
               b.append("\\f");
               break;
            default:
               b.append('(');
               b.append((int)c);
               b.append(')');
         }
      }
      else if(c<256){
         b.append((char)c);
      }
      else{
         b.append('\\');
         b.append('x');
         b.append(Integer.toHexString(c));
      }
      return b.toString();
   }
   
   static int toHexDigit(char d) throws PatternSyntaxException{
      int val=0;
      if(d>='0' && d<='9') val=d-'0';
      else if(d>='a' && d<='f') val=10+d-'a';
      else if(d>='A' && d<='F') val=10+d-'A';
      else throw new PatternSyntaxException("hexadecimal digit expected: "+d);
      return val;
   }
   
   public static void main(String[] args){
      if(!namesInitialized)initNames();
      if(args.length==0){
         System.out.println("Class usage: \\p{Class},\\P{Class}");
         printRealm(posixClasses,"Posix classes");
         printRealm(unicodeCategories,"Unicode categories");
         printRealm(unicodeBlocks,"Unicode blocks");
      }
      else{
         for(int i=0;i<args.length;i++){
            System.out.print(args[i]);
            System.out.print(": ");
            System.out.println(namedClasses.containsKey(args[i])? "supported": "not supported");
         }
      }
      /*
      int[][] data=new int[CATEGORY_COUNT][BLOCK_SIZE+2];
      for(int i=Character.MIN_VALUE;i<=Character.MAX_VALUE;i++){
         int cat=Character.getType((char)i);
         data[cat][BLOCK_SIZE]++;
         int b=(i>>8)&0xff;
         if(data[cat][b]==0){
            data[cat][b]=1;
            data[cat][BLOCK_SIZE+1]++;
         }
      }
      for(int i=0;i<CATEGORY_COUNT;i++){
         System.out.print(unicodeCategoryNames.get(new Integer(i))+": ");
         System.out.println(data[i][BLOCK_SIZE]+" chars, "+data[i][BLOCK_SIZE+1]+" blocks, "+(data[i][BLOCK_SIZE]/data[i][BLOCK_SIZE+1])+" chars/block");
      }
      */
   }
   
   private static void printRealm(Vector realm,String name){
      System.out.println(name+":");
      Enumeration e=realm.elements();
      while(e.hasMoreElements()){
         System.out.println("  "+e.nextElement());
      }
   }
}
