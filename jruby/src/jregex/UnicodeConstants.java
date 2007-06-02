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

interface UnicodeConstants{
   int CATEGORY_COUNT=32;
   int Cc=Character.CONTROL;
   int Cf=Character.FORMAT;
   int Co=Character.PRIVATE_USE;
   int Cn=Character.UNASSIGNED;
   int Lu=Character.UPPERCASE_LETTER;
   int Ll=Character.LOWERCASE_LETTER;
   int Lt=Character.TITLECASE_LETTER;
   int Lm=Character.MODIFIER_LETTER;
   int Lo=Character.OTHER_LETTER;
   int Mn=Character.NON_SPACING_MARK;
   int Me=Character.ENCLOSING_MARK;
   int Mc=Character.COMBINING_SPACING_MARK;
   int Nd=Character.DECIMAL_DIGIT_NUMBER;
   int Nl=Character.LETTER_NUMBER;
   int No=Character.OTHER_NUMBER;
   int Zs=Character.SPACE_SEPARATOR;
   int Zl=Character.LINE_SEPARATOR;
   int Zp=Character.PARAGRAPH_SEPARATOR;
   int Cs=Character.SURROGATE;
   int Pd=Character.DASH_PUNCTUATION;
   int Ps=Character.START_PUNCTUATION;
   int Pi=Character.START_PUNCTUATION;
   int Pe=Character.END_PUNCTUATION;
   int Pf=Character.END_PUNCTUATION;
   int Pc=Character.CONNECTOR_PUNCTUATION;
   int Po=Character.OTHER_PUNCTUATION;
   int Sm=Character.MATH_SYMBOL;
   int Sc=Character.CURRENCY_SYMBOL;
   int Sk=Character.MODIFIER_SYMBOL;
   int So=Character.OTHER_SYMBOL;
   
   int BLOCK_COUNT=256;
   int BLOCK_SIZE=256;
   
   int MAX_WEIGHT=Character.MAX_VALUE+1;
   int[] CATEGORY_WEIGHTS=new int[CATEGORY_COUNT];
}