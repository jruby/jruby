/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
grammar Pack;

sequence : directive* ;

directive : 'c' count?                                          # int8
          | 'C' count?                                          # uint8
          | 's' nativeOptLittle count?                          # int16Little
          | 's' nativeOptBig count?                             # int16Big
          | 's' NATIVE? count?                                  # int16Native
          | ('S' nativeOptLittle | 'v') count?                  # uint16Little
          | ('S' nativeOptBig | 'n') count?                     # uint16Big
          | 'S' NATIVE? count?                                  # uint16Native
          | ('i' nativeOptLittle | 'l' LITTLE) count?           # int32Little
          | ('i' nativeOptBig | 'l' BIG) count?                 # int32Big
          | ('i' NATIVE? | 'l') count?                          # int32Native
          | (('I' nativeOptLittle | 'L' LITTLE) | 'V') count?   # uint32Little
          | (('I' nativeOptBig | 'L' BIG) | 'N') count?         # uint32Big
          | ('I' NATIVE? | 'L') count?                          # uint32Native
          | ('q' nativeOptLittle | 'l' nativeLittle) count?     # int64Little
          | ('q' nativeOptBig | 'l' nativeBig) count?           # int64Big
          | ('q' NATIVE? | 'l' NATIVE) count?                   # int64Native
          | ('Q' nativeOptLittle | 'L' nativeLittle) count?     # uint64Little
          | ('Q' nativeOptBig | 'L' nativeBig) count?           # uint64Big
          | ('Q' NATIVE? | 'L' NATIVE) count?                   # uint64Native
          | 'U' count?                                          # utf8Character
          | 'w' count?                                          # berInteger
          | ('d' | 'D') count?                                  # f64Native
          | ('f' | 'F') count?                                  # f32Native
          | 'E' count?                                          # f64Little
          | 'e' count?                                          # f32Little
          | 'G' count?                                          # f64Big
          | 'g' count?                                          # f32Big
          | 'A' count?                                          # binaryStringSpacePadded
          | 'a' count?                                          # binaryStringNullPadded
          | 'Z' count?                                          # binaryStringNullStar
          | 'B' count?                                          # bitStringMSBFirst
          | 'b' count?                                          # bitStringMSBLast
          | 'H' count?                                          # hexStringHighFirst
          | 'h' count?                                          # hexStringLowFirst
          | 'u' count?                                          # uuString
          | 'M' count?                                          # mimeString
          | 'm' count?                                          # base64String
          | ('p' | 'P')                                         # pointer
          | '@' count?                                          # at
          | 'X' count?                                          # back
          | 'x' count?                                          # nullByte
          | subSequence                                         # subSequenceAlternate
          | ('v' | 'n' | 'V' | 'N' | 'U' | 'w' | 'd' | 'D' |
             'f' | 'F' | 'E' | 'e' | 'g' | 'G' | 'A' | 'a' |
             'Z' | 'B' | 'b' | 'H' | 'h' | 'u' | 'M' |
             'm' | 'p' | 'P' | 'X' | 'x') NATIVE                #errorDisallowedNative ;

count           : INT | '*' ;

subSequence     : '(' directive+ ')' INT? ;

nativeOptLittle : NATIVE* LITTLE NATIVE* ;
nativeOptBig    : NATIVE* BIG NATIVE* ;

nativeLittle    : NATIVE+ LITTLE NATIVE* | NATIVE* LITTLE NATIVE+ ;
nativeBig       : NATIVE+ BIG NATIVE* | NATIVE* BIG NATIVE+ ;

LITTLE          : '<' ;
BIG             : '>' ;
NATIVE          : [!_] ;

INT             : [0-9]+ ;

WS              : [ \t\n\u000b\f\r\u0000]+ -> skip ;
COMMENT         : '#' .*? (('\r'? '\n') | EOF) -> skip ;
