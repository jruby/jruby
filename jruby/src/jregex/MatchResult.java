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

public interface MatchResult{
   public int MATCH=0;
   public int PREFIX=-1;
   public int SUFFIX=-2;
   public int TARGET=-3;
   
   public Pattern pattern();
   
   public int groupCount();
   
   public boolean isCaptured();
   public boolean isCaptured(int groupId);
   public boolean isCaptured(String groupName);
   
   public String group(int n);
   public boolean getGroup(int n,StringBuffer sb);
   public boolean getGroup(int n,TextBuffer tb);
   
   public String group(String name);
   public boolean getGroup(String name,StringBuffer sb);
   public boolean getGroup(String name,TextBuffer tb);
   
   public String prefix();
   public String suffix();
   public String target();
   
   public int targetStart();
   public int targetEnd();
   public char[] targetChars();
   
   public int start();
   public int end();
   public int length();
   
   public int start(int n);
   public int end(int n);
   public int length(int n);
   
   public char charAt(int i);
   public char charAt(int i,int groupNo);
}