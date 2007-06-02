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
import java.util.Vector;

/**
 * A special-purpose subclass of the Pattern class.
 * Has two different applications:
 * <li> to search files by their paths using special patterns;
 * <li> to match path strings
 * Syntax:
 * <li><code><b>?</b></code> - any character but path separator
 * <li><code><b>*</b></code> - any string no including path separators
 * <li><code><b>**</b></code> - any path<br>
 * <br>
 * Usage:<pre>
 * PathPattern pp=new PathPattern("jregex/**"); //all files and directories
 *                                              //under the jregex directory
 * Enumeration files=pp.enumerateFiles();
 * Matcher m=pp.matcher();
 * while(files.hasMoreElements()){
 *    File f=(File)files.nextElement();
 *    m.setTarget(f.getPath());
 *    if(!m.matches()) System.out.println("Error in jregex.io.PathPattern");
 * }
 * </pre>
 * @see    jregex.WildcardPattern
 */

public class PathPattern extends Pattern{
   private static final int RESERVED=1;
   private static int GRP_NO=RESERVED+1;
   private static final int ANY_G=GRP_NO++;
   private static final int FS_G=GRP_NO++;
   private static final int STAR_G=GRP_NO++;
   private static final int QMARK_G=GRP_NO++;
   private static final int SPCHAR_G=GRP_NO++;
   private static final int NONROOT_G=GRP_NO++;
   private static final String grp(int gno,String s){
      return "({"+gno+"}"+s+")";
   }
   private static final String fsChars="/\\"+File.separator;
   private static final String fsClass="["+fsChars+"]";
   private static final String nfsClass="[^"+fsChars+"]";
   private static final String fName=nfsClass+"+";
   private static final Pattern fs=new Pattern(fsClass);
   private static final Pattern spCharPattern=new Pattern(
                  grp(NONROOT_G,"^(?!"+fsClass+")")+
                    "|"+
                  grp(ANY_G,fsClass+"?\\*\\*"+fsClass+"?")+
                    "|"+
                  grp(FS_G,fsClass)+
                    "|"+
                  grp(STAR_G,"\\*")+
                    "|"+
                  grp(QMARK_G,"\\?")+
                    "|"+
                  grp(SPCHAR_G,"[.()\\{\\}+|^$\\[\\]\\\\]")
               );
   
   private static final Replacer spCharProcessor=new Replacer(
      spCharPattern,
      new Substitution(){
         public void appendSubstitution(MatchResult mr,TextBuffer dest){
//System.out.println("spCharProcessor.appendSubstitution(): "+((Matcher)mr).groupv());
            if(mr.isCaptured(FS_G)){
               dest.append(fsClass);
            }
            else if(mr.isCaptured(ANY_G)){
               dest.append("(?:(?:");
               dest.append(fsClass);
               dest.append("|^)((?:");
               dest.append(fName);
               dest.append("(?:");
               dest.append(fsClass);
               dest.append(fName);
               dest.append(")*)?))?");
               dest.append("(?:");
               dest.append(fsClass);
               dest.append("|$)");
            }
            else if(mr.isCaptured(STAR_G)){
               dest.append("(");
               dest.append(nfsClass);
               dest.append("*)");
            }
            else if(mr.isCaptured(QMARK_G)){
               dest.append("(");
               dest.append(nfsClass);
               dest.append(")");
            }
            else if(mr.isCaptured(SPCHAR_G)){
               dest.append("\\");
               mr.getGroup(SPCHAR_G,dest);
            }
            else if(mr.isCaptured(NONROOT_G)){
               dest.append("(?:\\.");
               dest.append(fsClass);
               dest.append(")?");
            }
         }
      }
   );
   
   private String str;
   private String root;
   private File rootf;
   private PathElementMask queue,last;
   
   public PathPattern(String ptn){
      this(ptn,DEFAULT);
   }
   
   public PathPattern(String ptn,boolean icase){
      this(ptn,icase? DEFAULT|IGNORE_CASE: DEFAULT);
   }
   
   public PathPattern(String path,int flags){
      this(null,path,flags);
   }
   
   public PathPattern(File dir,String path,boolean icase){
      this(null,path,icase? DEFAULT|IGNORE_CASE: DEFAULT);
   }
   
   public PathPattern(File dir,String path,int flags){
      if(path==null || path.length()==0)throw new IllegalArgumentException("empty path not allowed");
      
      str=path;
      RETokenizer tok=new RETokenizer(fs.matcher(path),true);
      String s=tok.nextToken();
      if(s.equals("")){
         if(dir!=null)rootf=dir;
         else root="/";
      }
      else{
         if(dir!=null)rootf=dir;
         else root=".";
         addElement(newMask(s,flags,tok.hasMore()));
      }
      while(tok.hasMore()){
         s=tok.nextToken();
         boolean hasMore=tok.hasMore();
         if(s.equals("")){
            if(hasMore)throw new IllegalArgumentException("\"//\" not allowed");
            else break;
         }
         addElement(newMask(s,flags,hasMore));
      }
      compile(spCharProcessor.replace(path),flags);
//System.out.println(spCharProcessor.replace(path));
   }
   
   private void addElement(PathElementMask mask){
      if(queue==null){
        queue=last=mask;
      }
      else{
         last=(last.next=mask);
      }
   }
   
   public Enumeration enumerateFiles(){
      PathElementEnumerator fe=queue.newEnumerator();
      fe.setDir(rootf!=null? rootf: new File(root));
      return fe;
   }
   
   public File[] files(){
      Enumeration e=enumerateFiles();
      Vector v=new Vector();
      while(e.hasMoreElements()) v.addElement(e.nextElement());
      File[] files=new File[v.size()];
      v.copyInto(files);
      return files;
   }
   
  /**
   * @deprecated Is meaningless with regard to variable paths (since v.1.2)
   */
   public String[] names(){
      return null;
   }
   
  /**
   * @deprecated Is meaningless with regard to  variable paths (since v.1.2) 
   */
   public File directory(){
      return null;
   }
   
   private static PathElementMask newMask(String s,int flags,boolean dirsOnly){
      if(s==null || s.length()==0)throw new IllegalArgumentException("Error: empty path element not allowed");
      if(s.indexOf('*')<0 && s.indexOf('?')<0){
         //if((flags&IGNORE_CASE)==0) return PathElementMask.fixedMask(s,dirsOnly);
         //just a dirty trick, 
         //on windows this could be a disk name ("D:"),
         //and so won't be listed, so the RegularMask won't help
         if((flags&IGNORE_CASE)==0 || s.indexOf(':')>=0) return PathElementMask.fixedMask(s,dirsOnly);
         else return PathElementMask.regularMask(s,flags,dirsOnly);
      }
      else if(s.equals("*")) return PathElementMask.anyFile(dirsOnly);
      else if(s.equals("**")) return PathElementMask.anyPath(dirsOnly);
      else return PathElementMask.regularMask(s,flags,dirsOnly);
   }
   
   public String toString(){
      return str;
   }
   
   //public static void main(String[] args)throws Exception{
   //   PathPattern path=new PathPattern(args.length>0? args[0]: "/**/*tmp*/**",true);
   //   //PathPattern path=new PathPattern(args.length>0? args[0]: "*/*",true);
   //   //PathPattern path=new PathPattern(args.length>0? args[0]: "/**/*abc*",true);
   //   Enumeration e=path.enumerateFiles();
   //   int c=0;
   //   int err=0;
   //   Matcher m=path.matcher();
   //   long t0=System.currentTimeMillis();
   //   //while(e.hasMoreElements()){
   //   //while(e.hasMoreElements() && c<30){
   //   while(e.hasMoreElements() && err<10){
   //      File f=(File)e.nextElement();
   //      if(!m.matches(f.getPath())){
   //         System.out.println("error with file: "+f);
   //         err++;
   //      }
   //      else{
   //         //System.out.println("file matches: "+m.groupv());
   //      }
   //      c++;
   //   }
   //   long t1=System.currentTimeMillis();
   //   System.out.println("found "+err+" errors in "+c+" files, time="+(t1-t0));
   //}
}