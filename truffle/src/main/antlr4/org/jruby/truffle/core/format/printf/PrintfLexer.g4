/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
lexer grammar PrintfLexer;

FORMAT  : '%' -> mode(FORMAT_MODE);
LITERAL : (~'%')+ ;

mode FORMAT_MODE;

ANGLE_KEY   : '<' .*? '>' ;
ZERO        : '0' ;
NUMBER      : [1-9] [0-9]* ;
SPACE       : ' ' ;
PLUS        : '+' ;
MINUS       : '-' ;
STAR        : '*' ;
DOLLAR      : '$' ;
DOT         : '.' ;
HASH        : '#' ;
CURLY_KEY   : '{' .*? '}' -> mode(DEFAULT_MODE) ;
TYPE        : [bBdiouxXeEfgGaAcps] -> mode(DEFAULT_MODE) ;
ESCAPED     : '%' -> mode(DEFAULT_MODE) ;
