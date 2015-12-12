/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
grammar Printf;

sequence : directive* ;

directive : '%' CURLY_KEY                                       # string
          | '%' ANGLE_KEY?
                flag*
                width=NUMBER?
                ('.' precision=NUMBER)?
                TYPE                                            # format
          | '%%'                                                # escape
          | TEXT                                                # text
          | NUMBER                                              # number ;

flag : ' '
     | '0'
     | '+'
     | '-'
     | '*'
     | NUMBER '$' ;

CURLY_KEY : '{' .+? '}' ;
ANGLE_KEY : '<' .+? '>' ;

TYPE : [bBdiouxXeEfgGaAcps] ;

TEXT : ~[%0-9]+ ;
NUMBER : [0-9]+ ;
