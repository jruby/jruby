/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
parser grammar PrintfParser;

options { tokenVocab=PrintfLexer; }

sequence : (FORMAT directive | literal)* ;

directive : CURLY_KEY                  # string
          | ESCAPED                    # escaped
          | ANGLE_KEY?
            flag*
            width=NUMBER?
            (DOT precision=(ZERO|NUMBER))?
            TYPE                       # format ;

flag : SPACE
     | ZERO
     | PLUS
     | MINUS
     | STAR
     | HASH
     | NUMBER DOLLAR ;

literal : LITERAL ;
