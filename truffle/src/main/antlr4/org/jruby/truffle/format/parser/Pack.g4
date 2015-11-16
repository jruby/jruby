/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
grammar Pack;

sequence : directive* ;

directive : C count?                            # character
          | (S NATIVE? LITTLE | 'v') count?     # shortLittle
          | (S NATIVE? BIG | 'n') count?        # shortBig
          | S NATIVE? count?                    # shortNative
          | I NATIVE? count?                    # intNative
          | (L LITTLE | 'V') count?             # longLittle
          | (L BIG | 'N') count?                # longBig
          | L count?                            # longNative
          | (Q | L NATIVE) LITTLE count?        # quadLittle
          | (Q | L NATIVE) BIG count?           # quadBig
          | (Q | L NATIVE) count?               # quadNative
          | 'U' count?                          # utf8Character
          | 'w' count?                          # berInteger
          | D count?                            # doubleNative
          | F count?                            # floatNative
          | 'E' count?                          # doubleLittle
          | 'e' count?                          # floatLittle
          | 'G' count?                          # doubleBig
          | 'g' count?                          # floatBig
          | 'A' count?                          # binaryStringSpacePadded
          | 'a' count?                          # binaryStringNullPadded
          | 'Z' count?                          # binaryStringNullStar
          | 'B' count?                          # bitStringMSBFirst
          | 'b' count?                          # bitStringMSBLast
          | 'H' count?                          # hexStringHighFirst
          | 'h' count?                          # hexStringLowFirst
          | 'u' count?                          # uuString
          | 'M' INT?                            # mimeString
          | 'm' count?                          # base64String
          | ('p' | 'P')                         # pointer
          | '@' INT                             # at
          | 'X' count?                          # back
          | 'x' count?                          # nullByte
          | '(' directive+ ')' INT              # subSequence
          | ('v' | 'n' | 'V' | 'N' | 'U' | 'w' | D | F | 'E' | 'e' | 'g' | 'G' | 'A' | 'a' | 'Z' | 'B' | 'b' | 'H'
                    | 'h' | 'u' | 'M' | 'm' | 'p' | 'P' | 'X' | 'x') '_'
                { notifyErrorListeners("'_' allowed only after types sSiIlLqQ"); } #errorUnderscore ;

count  : INT | '*' ;

C      : [cC] ;
S      : [sS] ;
I      : [iI] ;
L      : [lL] ;
Q      : [qQ] ;
D      : [dD] ;
F      : [fF] ;

LITTLE : '<' ;
BIG    : '>' ;
NATIVE : '_' | '!' ;

INT    : [0-9]+ ;

WS     : [ \t\n\u000b\f\r]+ -> skip ;
