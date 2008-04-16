grammar Ruby;

tokens {
        UPLUS         /* unary+ */;
        UMINUS        /* unary- */;
        UMINUS_NUM    /* unary- */;
        POW           /* ** */;
        CMP           /* <=> */;
        EQ            /* == */;
        EQQ           /* === */;
        NEQ           /* != */;
        GEQ           /* >= */;
        LEQ           /* <= */;
        ANDOP;
        OROP   /* && and || */;
        MATCH;
        NMATCH /* =~ and !~ */;
        DOT           /* Is just '.' in ruby and not a token */;
        DOT2;
        DOT3    /* .. and ... */;
        AREF;
        ASET    /* [] and []= */;
        LSHFT;
        RSHFT  /* << and >> */;
        COLON2        /* :: */;
        COLON3        /* :: at EXPR_BEG */;
        OP_ASGN       /* +=, -=  etc. */;
        ASSOC         /* => */;
        LPAREN        /* ( */;
        LPAREN2        /* ( Is just '(' in ruby and not a token */;
        RPAREN        /* ) */;
        LPAREN_ARG    /* ( */;
        LBRACK        /* [ */;
        RBRACK        /* ] */;
        LBRACE        /* { */;
        LBRACE_ARG    /* { */;
        STAR          /* * */;
        STAR2         /* *  Is just '*' in ruby and not a token */;
        AMPER         /* & */;
        AMPER2        /* &  Is just '&' in ruby and not a token */;
        TILDE         /* ` is just '`' in ruby and not a token */;
        PERCENT       /* % is just '%' in ruby and not a token */;
        DIVIDE        /* / is just '/' in ruby and not a token */;
        PLUS          /* + is just '+' in ruby and not a token */;
        MINUS         /* - is just '-' in ruby and not a token */;
        LT            /* < is just '<' in ruby and not a token */;
        GT            /* > is just '>' in ruby and not a token */;
        PIPE          /* | is just '|' in ruby and not a token */;
        BANG          /* ! is just '!' in ruby and not a token */;
        CARET         /* ^ is just '^' in ruby and not a token */;
        LCURLY        /* { is just '{' in ruby and not a token */;
        RCURLY        /* } is just '}' in ruby and not a token */;
        BACK_REF2     /* { is just '`' in ruby and not a token */;
        SYMBEG;
        STRING_BEG;
        XSTRING_BEG;
        REGEXP_BEG;
        WORDS_BEG;
        QWORDS_BEG;
        STRING_DBEG;
        STRING_DVAR;
        STRING_END;
        CLASS;
        MODULE;
        DEF;
        UNDEF;
        BEGIN;
        RESCUE;
        ENSURE;
        END;
        IF;
        UNLESS;
        THEN;
        ELSIF;
        ELSE;
        CASE;
        WHEN;
        WHILE;
        UNTIL;
        FOR;
        BREAK;
        NEXT;
        REDO;
        RETRY;
        IN;
        DO;
        DO_COND;
        DO_BLOCK;
        RETURN;
        YIELD;
        SUPER;
        SELF;
        NIL;
        TRUE;
        FALSE;
        AND;
        OR;
        NOT;
        IF_MOD;
        UNLESS_MOD;
        WHILE_MOD;
        UNTIL_MOD;
        RESCUE_MOD;
        ALIAS;
        DEFINED;
        LBEGIN;
        LEND;
        U_LINE_U;
        U_FILE_U;
}

program       : /*{
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
              }*/ compstmt /*{
                  if ($2 != null) {
                      // last expression should not be void
                      if ($2 instanceof BlockNode) {
                          support.checkUselessStatement($<BlockNode>2.getLast());
                      } else {
                          support.checkUselessStatement($2);
                      }
                  }
                  support.getResult().seAST(support.addRootNode($2));
              }*/
    ;

bodystmt      : compstmt opt_rescue opt_else opt_ensure /*{
                  Node node = $1;

                  if ($2 != null) {
                      node = new RescueNode(getPosition($1, true), $1, $2, $3);
                  } else if ($3 != null) {
                      warnings.warn(getPosition($1), "else without rescue is useless");
                      node = support.appendToBlock($1, $3);
                  }
                  if ($4 != null) {
                      node = new EnsureNode(getPosition($1), node, $4);
                  }

                  $$ = node;
              }*/
    ;

compstmt      : stmts opt_terms /*{
                  if ($1 instanceof BlockNode) {
                      support.checkUselessStatements($<BlockNode>1);
                  }
                  $$ = $1;
              }*/
    ;

stmts         : none
              | stmt /*{
                  $$ = support.newline_node($1, getPosition($1, true));
              }*/
              | stmts terms stmt /*{
                  $$ = support.appendToBlock($1, support.newline_node($3, getPosition($3, true)));
              }*/
              /*| error stmt {
                  $$ = $2;
              }*/
    ;

stmt          : ALIAS fitem /*{
                  lexer.setState(LexState.EXPR_FNAME);
              }*/ fitem /*{
                  $$ = new AliasNode(support.union($1, $4), (String) $2.getValue(), (String) $4.getValue());
              }*/
              | ALIAS GVAR GVAR /*{
                  $$ = new VAliasNode(getPosition($1), (String) $2.getValue(), (String) $3.getValue());
              }*/
              | ALIAS GVAR BACK_REF /*{
                  $$ = new VAliasNode(getPosition($1), (String) $2.getValue(), "$" + $<BackRefNode>3.getType()); // XXX
              }*/
              | ALIAS GVAR NTH_REF /*{
                  yyerror("can't make alias for the number variables");
              }*/
              | UNDEF undef_list /*{
                  $$ = $2;
              }*/
              | stmt IF_MOD expr_value /*{
                  $$ = new IfNode(support.union($1, $3), support.getConditionNode($3), $1, null);
              }*/
              | stmt UNLESS_MOD expr_value /*{
                  $$ = new IfNode(support.union($1, $3), support.getConditionNode($3), null, $1);
              }*/
              | stmt WHILE_MOD expr_value /*{
                  if ($1 != null && $1 instanceof BeginNode) {
                      $$ = new WhileNode(getPosition($1), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                  } else {
                      $$ = new WhileNode(getPosition($1), support.getConditionNode($3), $1, true);
                  }
              }*/
              | stmt UNTIL_MOD expr_value /*{
                  if ($1 != null && $1 instanceof BeginNode) {
                      $$ = new UntilNode(getPosition($1), support.getConditionNode($3), $<BeginNode>1.getBodyNode());
                  } else {
                      $$ = new UntilNode(getPosition($1), support.getConditionNode($3), $1);
                  }
              }*/
              | stmt RESCUE_MOD stmt /*{
                  $$ = new RescueNode(getPosition($1), $1, new RescueBodyNode(getPosition($1), null,$3, null), null);
              }*/
              | LBEGIN /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("BEGIN in method");
                  }
                  support.pushLocalScope();
              }*/ LCURLY compstmt RCURLY /*{
                  support.getResult().addBeginNode(support.getCurrentScope(), $4);
                  support.popCurrentScope();
                  $$ = null; //XXX 0;
              }*/
              | LEND LCURLY compstmt RCURLY /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("END in method; use at_exit");
                  }
                  // FIXME: Totally broken if scoping is important...though if not this is an improvement over iterNode
                  support.getResult().addEndNode(new PostExeNode(getPosition($1), $3));
                  $$ = null;
              }*/
              | lhs '=' command_call /*{
                  support.checkExpression($3);
                  $$ = support.node_assign($1, $3);
              }*/
              | mlhs '=' command_call /*{
                  support.checkExpression($3);
                  if ($1.getHeadNode() != null) {
                      $1.setValueNode(new ToAryNode(getPosition($1), $3));
                  } else {
                      $1.setValueNode(new ArrayNode(getPosition($1), $3));
                  }
                  $$ = $1;
              }*/
              | var_lhs OP_ASGN command_call /*{
                  support.checkExpression($3);

                  String name = $<INameNode>1.getName();
                  String asgnOp = (String) $2.getValue();
                  if (asgnOp.equals("||")) {
                      $1.setValueNode($3);
                      $$ = new OpAsgnOrNode(support.union($1, $3), support.gettable2(name, $1.getPosition()), $1);
                  } else if (asgnOp.equals("&&")) {
                      $1.setValueNode($3);
                      $$ = new OpAsgnAndNode(support.union($1, $3), support.gettable2(name, $1.getPosition()), $1);
                  } else {
                      $1.setValueNode(support.getOperatorCallNode(support.gettable2(name, $1.getPosition()), asgnOp, $3));
                      $1.setPosition(support.union($1, $3));
                      $$ = $1;
                  }
              }*/
              | primary_value '[' aref_args RBRACK OP_ASGN command_call /*{
                  $$ = new OpElementAsgnNode(getPosition($1), $1, (String) $5.getValue(), $3, $6);

              }*/
              | primary_value DOT IDENTIFIER OP_ASGN command_call /*{
                  $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }*/
              | primary_value DOT CONSTANT OP_ASGN command_call /*{
                  $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }*/
              | primary_value COLON2 IDENTIFIER OP_ASGN command_call /*{
                  $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }*/
              | backref OP_ASGN command_call /*{
                  support.backrefAssignError($1);
              }*/
              | lhs '=' mrhs /*{
                  $$ = support.node_assign($1, new SValueNode(getPosition($1), $3));
              }*/
              | mlhs '=' arg_value /*{
                  if ($1.getHeadNode() != null) {
                      $1.setValueNode(new ToAryNode(getPosition($1), $3));
                  } else {
                      $1.setValueNode(new ArrayNode(getPosition($1), $3));
                  }
                  $$ = $1;
              }*/
              | mlhs '=' mrhs /*{
                  $<AssignableNode>1.setValueNode($3);
                  $$ = $1;
                  $1.setPosition(support.union($1, $3));
              }*/
              | expr 
    ;

expr          : command_call 
              | expr AND expr /*{
                  $$ = support.newAndNode($1, $3);
              }*/
              | expr OR expr /*{
                  $$ = support.newOrNode($1, $3);
              }*/
              | NOT expr /*{
                  $$ = new NotNode(support.union($1, $2), support.getConditionNode($2));
              }*/
              | BANG command_call /*{
                  $$ = new NotNode(support.union($1, $2), support.getConditionNode($2));
              }*/
              | arg
    ;

expr_value    : expr /*{
                  support.checkExpression($1);
              }*/
    ;

command_call  : command
              | block_command
              | RETURN call_args /*{
                  $$ = new ReturnNode(support.union($1, $2), support.ret_args($2, getPosition($1)));
              }*/
              | BREAK call_args /*{
                  $$ = new BreakNode(support.union($1, $2), support.ret_args($2, getPosition($1)));
              }*/
              | NEXT call_args /*{
                  $$ = new NextNode(support.union($1, $2), support.ret_args($2, getPosition($1)));
              }*/
    ;

block_command : block_call
              | block_call DOT operation2 command_args /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
              | block_call COLON2 operation2 command_args /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
    ;

cmd_brace_block : LBRACE_ARG /*{
                    support.pushBlockScope();
                }*/ opt_block_var compstmt RCURLY /*{
                    $$ = new IterNode(getPosition($1), $3, support.getCurrentScope(), $4);
                    support.popCurrentScope();
                }*/
    ;

command       : operation command_args  /*%prec LOWEST*/ /*{
                  $$ = support.new_fcall($1, $2, null);
              }*/
              | operation command_args cmd_brace_block /*{
                  $$ = support.new_fcall($1, $2, $3); 
              }*/
              | primary_value DOT operation2 command_args /*%prec LOWEST*/ /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
              | primary_value DOT operation2 command_args cmd_brace_block /*{
                  $$ = support.new_call($1, $3, $4, $5); 
              }*/
              | primary_value COLON2 operation2 command_args /*%prec LOWEST*/ /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
              | primary_value COLON2 operation2 command_args cmd_brace_block /*{
                  $$ = support.new_call($1, $3, $4, $5); 
              }*/
              | SUPER command_args /*{
                  $$ = support.new_super($2, $1); // .setPosFrom($2);
              }*/
              | YIELD command_args /*{
                  $$ = support.new_yield(getPosition($1), $2);
              }*/
    ;

mlhs          : mlhs_basic
              | LPAREN mlhs_entry RPAREN /*{
                  $$ = $2;
              }*/
    ;

mlhs_entry    : mlhs_basic
              | LPAREN mlhs_entry RPAREN /*{
                  $$ = new MultipleAsgnNode(getPosition($1), new ArrayNode(getPosition($1), $2), null);
              }*/
    ;

mlhs_basic    : mlhs_head /*{
                  $$ = new MultipleAsgnNode(getPosition($1), $1, null);
              }*/
              | mlhs_head mlhs_item /*{
                  $$ = new MultipleAsgnNode(support.union($<Node>1, $<Node>2), $1.add($2), null);
                  $<Node>1.setPosition(support.union($<Node>1, $<Node>2));
              }*/
              | mlhs_head STAR mlhs_node /*{
                  $$ = new MultipleAsgnNode(getPosition($1), $1, $3);
              }*/
              | mlhs_head STAR /*{
                  $$ = new MultipleAsgnNode(getPosition($1), $1, new StarNode(getPosition(null)));
              }*/
              | STAR mlhs_node /*{
                  $$ = new MultipleAsgnNode(getPosition($1), null, $2);
              }*/
              | STAR /*{
                  $$ = new MultipleAsgnNode(getPosition($1), null, new StarNode(getPosition(null)));
              }*/
    ;

mlhs_item     : mlhs_node 
              | LPAREN mlhs_entry RPAREN /*{
                  $$ = $2;
              }*/
    ;

mlhs_head     : mlhs_item ',' /*{
                  $$ = new ArrayNode($1.getPosition(), $1);
              }*/
              | mlhs_head mlhs_item ',' /*{
                  $$ = $1.add($2);
              }*/
    ;

mlhs_node     : variable /*{
                  $$ = support.assignable($1, null);
              }*/
              | primary_value '[' aref_args RBRACK /*{
                  $$ = support.aryset($1, $3);
              }*/
              | primary_value DOT IDENTIFIER /*{
                  $$ = support.attrset($1, (String) $3.getValue());
              }*/
              | primary_value COLON2 IDENTIFIER /*{
                  $$ = support.attrset($1, (String) $3.getValue());
              }*/
              | primary_value DOT CONSTANT /*{
                  $$ = support.attrset($1, (String) $3.getValue());
              }*/
              | primary_value COLON2 CONSTANT /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("dynamic constant assignment");
                  }

                  ISourcePosition position = support.union($1, $3);

                  $$ = new ConstDeclNode(position, null, new Colon2Node(position, $1, (String) $3.getValue()), null);
              }*/
              | COLON3 CONSTANT /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("dynamic constant assignment");
                  }

                  ISourcePosition position = support.union($1, $2);

                  $$ = new ConstDeclNode(position, null, new Colon3Node(position, (String) $2.getValue()), null);
              }*/
              | backref /*{
                  support.backrefAssignError($1);
              }*/
    ;

lhs           : variable /*{
                  $$ = support.assignable($1, null);
              }*/
              | primary_value '[' aref_args RBRACK /*{
                  $$ = support.aryset($1, $3);
              }*/
              | primary_value DOT IDENTIFIER /*{
                  $$ = support.attrset($1, (String) $3.getValue());
              }*/
              | primary_value COLON2 IDENTIFIER /*{
                  $$ = support.attrset($1, (String) $3.getValue());
              }*/
              | primary_value DOT CONSTANT /*{
                  $$ = support.attrset($1, (String) $3.getValue());
              }*/
              | primary_value COLON2 CONSTANT /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("dynamic constant assignment");
                  }
                        
                  ISourcePosition position = support.union($1, $3);

                  $$ = new ConstDeclNode(position, null, new Colon2Node(position, $1, (String) $3.getValue()), null);
              }*/
              | COLON3 CONSTANT /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("dynamic constant assignment");
                  }

                  ISourcePosition position = support.union($1, $2);

                  $$ = new ConstDeclNode(position, null, new Colon3Node(position, (String) $2.getValue()), null);
              }*/
              | backref /*{
                   support.backrefAssignError($1);
              }*/
    ;

cname         : IDENTIFIER /*{
                  yyerror("class/module name must be CONSTANT");
              }*/
              | CONSTANT
    ;

cpath         : COLON3 cname /*{
                  $$ = new Colon3Node(support.union($1, $2), (String) $2.getValue());
              }*/
              | cname /*{
                  $$ = new Colon2Node($1.getPosition(), null, (String) $1.getValue());
              }*/
              | primary_value COLON2 cname /*{
                  $$ = new Colon2Node(support.union($1, $3), $1, (String) $3.getValue());
              }*/
    ;

// Token:fname - A function name [!null]
fname         : IDENTIFIER | CONSTANT | FID
              | op /*{
                  lexer.setState(LexState.EXPR_END);
                  $$ = $1;
              }*/
              // FIXME: reswords is really Keyword which is not a Token...This should bomb
              | reswords /*{
                  lexer.setState(LexState.EXPR_END);
                  $$ = $<>1;
              }*/
    ;

fitem         : fname | symbol
    ;

undef_list    : fitem /*{
                  $$ = new UndefNode(getPosition($1), (String) $1.getValue());
              }*/
              | undef_list ',' /*{
                  lexer.setState(LexState.EXPR_FNAME);
              }*/ fitem /*{
                  $$ = support.appendToBlock($1, new UndefNode(getPosition($1), (String) $4.getValue()));
              }*/
    ;

// Token:op - inline operations [!null]
op            : PIPE | CARET | AMPER2 | CMP | EQ | EQQ | MATCH | GT
              | GEQ | LT | LEQ | LSHFT | RSHFT | PLUS  | MINUS | STAR2
              | STAR | DIVIDE | PERCENT | POW | TILDE | UPLUS | UMINUS
              | AREF | ASET | BACK_REF2
    ;

// Keyword:reswords - reserved words [!null]
reswords        : U_LINE_U | U_FILE_U  | LBEGIN | LEND
                | ALIAS | AND | BEGIN | BREAK | CASE | CLASS | DEF
                | DEFINED | DO | ELSE | ELSIF | END | ENSURE | FALSE
                | FOR | IN | MODULE | NEXT | NIL | NOT
                | OR | REDO | RESCUE | RETRY | RETURN | SELF | SUPER
                | THEN | TRUE | UNDEF | WHEN | YIELD
                | IF_MOD | UNLESS_MOD | WHILE_MOD | UNTIL_MOD | RESCUE_MOD
    ;

arg           : lhs '=' arg /*{
                  $$ = support.node_assign($1, $3);
                  // FIXME: Consider fixing node_assign itself rather than single case
                  $<Node>$.setPosition(support.union($1, $3));
              }*/
              | lhs '=' arg RESCUE_MOD arg /*{
                  ISourcePosition position = support.union($4, $5);
                  $$ = support.node_assign($1, new RescueNode(position, $3, new RescueBodyNode(position, null, $5, null), null));
              }*/
              | var_lhs OP_ASGN arg /*{
                  support.checkExpression($3);
                  String name = $<INameNode>1.getName();
                  String asgnOp = (String) $2.getValue();

                  if (asgnOp.equals("||")) {
                      $1.setValueNode($3);
                      $$ = new OpAsgnOrNode(support.union($1, $3), support.gettable2(name, $1.getPosition()), $1);
                  } else if (asgnOp.equals("&&")) {
                      $1.setValueNode($3);
                      $$ = new OpAsgnAndNode(support.union($1, $3), support.gettable2(name, $1.getPosition()), $1);
                  } else {
                      $1.setValueNode(support.getOperatorCallNode(support.gettable2(name, $1.getPosition()), asgnOp, $3));
                      $1.setPosition(support.union($1, $3));
                      $$ = $1;
                  }
              }*/
              | primary_value '[' aref_args RBRACK OP_ASGN arg /*{
                  $$ = new OpElementAsgnNode(getPosition($1), $1, (String) $5.getValue(), $3, $6);
              }*/
              | primary_value DOT IDENTIFIER OP_ASGN arg /*{
                  $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }*/
              | primary_value DOT CONSTANT OP_ASGN arg /*{
                  $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }*/
              | primary_value COLON2 IDENTIFIER OP_ASGN arg /*{
                  $$ = new OpAsgnNode(getPosition($1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
              }*/
              | primary_value COLON2 CONSTANT OP_ASGN arg /*{
                  yyerror("constant re-assignment");
              }*/
              | COLON3 CONSTANT OP_ASGN arg /*{
                  yyerror("constant re-assignment");
              }*/
              | backref OP_ASGN arg /*{
                  support.backrefAssignError($1);
              }*/
              | arg DOT2 arg /*{
                  support.checkExpression($1);
                  support.checkExpression($3);
                  $$ = new DotNode(support.union($1, $3), $1, $3, false);
              }*/
              | arg DOT3 arg /*{
                  support.checkExpression($1);
                  support.checkExpression($3);
                  $$ = new DotNode(support.union($1, $3), $1, $3, true);
              }*/
              | arg PLUS arg /*{
                  $$ = support.getOperatorCallNode($1, "+", $3, getPosition(null));
              }*/
              | arg MINUS arg /*{
                  $$ = support.getOperatorCallNode($1, "-", $3, getPosition(null));
              }*/
              | arg STAR2 arg /*{
                  $$ = support.getOperatorCallNode($1, "*", $3, getPosition(null));
              }*/
              | arg DIVIDE arg /*{
                  $$ = support.getOperatorCallNode($1, "/", $3, getPosition(null));
              }*/
              | arg PERCENT arg /*{
                  $$ = support.getOperatorCallNode($1, "%", $3, getPosition(null));
              }*/
              | arg POW arg /*{
                  $$ = support.getOperatorCallNode($1, "**", $3, getPosition(null));
              }*/
              | UMINUS_NUM INTEGER POW arg /*{
                  $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, getPosition(null)), "-@");
              }*/
              | UMINUS_NUM FLOAT POW arg /*{
                  $$ = support.getOperatorCallNode(support.getOperatorCallNode($2, "**", $4, getPosition(null)), "-@");
              }*/
              | UPLUS arg /*{
                  if ($2 != null && $2 instanceof ILiteralNode) {
                      $$ = $2;
                  } else {
                      $$ = support.getOperatorCallNode($2, "+@");
                  }
              }*/
              | UMINUS arg /*{
                  $$ = support.getOperatorCallNode($2, "-@");
              }*/
              | arg PIPE arg /*{
                  $$ = support.getOperatorCallNode($1, "|", $3, getPosition(null));
              }*/
              | arg CARET arg /*{
                  $$ = support.getOperatorCallNode($1, "^", $3, getPosition(null));
              }*/
              | arg AMPER2 arg /*{
                  $$ = support.getOperatorCallNode($1, "&", $3, getPosition(null));
              }*/
              | arg CMP arg /*{
                  $$ = support.getOperatorCallNode($1, "<=>", $3, getPosition(null));
              }*/
              | arg GT arg /*{
                  $$ = support.getOperatorCallNode($1, ">", $3, getPosition(null));
              }*/
              | arg GEQ arg /*{
                  $$ = support.getOperatorCallNode($1, ">=", $3, getPosition(null));
              }*/
              | arg LT arg /*{
                  $$ = support.getOperatorCallNode($1, "<", $3, getPosition(null));
              }*/
              | arg LEQ arg /*{
                  $$ = support.getOperatorCallNode($1, "<=", $3, getPosition(null));
              }*/
              | arg EQ arg /*{
                  $$ = support.getOperatorCallNode($1, "==", $3, getPosition(null));
              }*/
              | arg EQQ arg /*{
                  $$ = support.getOperatorCallNode($1, "===", $3, getPosition(null));
              }*/
              | arg NEQ arg /*{
                  $$ = new NotNode(support.union($1, $3), support.getOperatorCallNode($1, "==", $3, getPosition(null)));
              }*/
              | arg MATCH arg /*{
                  $$ = support.getMatchNode($1, $3);
              }*/
              | arg NMATCH arg /*{
                  $$ = new NotNode(support.union($1, $3), support.getMatchNode($1, $3));
              }*/
              | BANG arg /*{
                  $$ = new NotNode(support.union($1, $2), support.getConditionNode($2));
              }*/
              | TILDE arg /*{
                  $$ = support.getOperatorCallNode($2, "~");
              }*/
              | arg LSHFT arg /*{
                  $$ = support.getOperatorCallNode($1, "<<", $3, getPosition(null));
              }*/
              | arg RSHFT arg /*{
                  $$ = support.getOperatorCallNode($1, ">>", $3, getPosition(null));
              }*/
              | arg ANDOP arg /*{
                  $$ = support.newAndNode($1, $3);
              }*/
              | arg OROP arg /*{
                  $$ = support.newOrNode($1, $3);
              }*/
              | DEFINED opt_nl arg /*{
                  $$ = new DefinedNode(getPosition($1), $3);
              }*/
              | arg '?' arg ':' arg /*{
                  $$ = new IfNode(getPosition($1), support.getConditionNode($1), $3, $5);
              }*/
              | primary /*{
                  $$ = $1;
              }*/
    ;

arg_value     : arg /*{
                  support.checkExpression($1);
                  $$ = $1;   
              }*/
    ;

aref_args     : none
              | command opt_nl /*{
                  $$ = new ArrayNode(getPosition($1), $1);
              }*/
              | args trailer /*{
                  $$ = $1;
              }*/
              | args ',' STAR arg opt_nl /*{
                  support.checkExpression($4);
                  $$ = support.arg_concat(getPosition($1), $1, $4);
              }*/
              | assocs trailer /*{
                  $$ = new ArrayNode(getPosition($1), new HashNode(getPosition(null), $1));
              }*/
              | STAR arg opt_nl /*{
                  support.checkExpression($2);
                  $$ = new NewlineNode(getPosition($1), new SplatNode(getPosition($1), $2));
              }*/
    ;

paren_args    : LPAREN2 none RPAREN /*{
                  $$ = new ArrayNode(support.union($1, $3));
              }*/
              | LPAREN2 call_args opt_nl RPAREN /*{
                  $$ = $2;
                  $<Node>$.setPosition(support.union($1, $4));
              }*/
              | LPAREN2 block_call opt_nl RPAREN /*{
                  $$ = new ArrayNode(getPosition($1), $2);
              }*/
              | LPAREN2 args ',' block_call opt_nl RPAREN /*{
                  $$ = $2.add($4);
              }*/
    ;

opt_paren_args: none | paren_args 
    ;

// Node:call_args - Arguments for a function call
call_args     : command /*{
                  $$ = new ArrayNode(getPosition($1), $1);
              }*/
              | args opt_block_arg /*{
                  $$ = support.arg_blk_pass($1, $2);
              }*/
              | args ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), $1, $4);
                  $$ = support.arg_blk_pass($<Node>$, $5);
              }*/
              | assocs opt_block_arg /*{
                  $$ = new ArrayNode(getPosition($1), new HashNode(getPosition(null), $1));
                  $$ = support.arg_blk_pass((Node)$$, $2);
              }*/
              | assocs ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), new ArrayNode(getPosition($1), new HashNode(getPosition(null), $1)), $4);
                  $$ = support.arg_blk_pass((Node)$$, $5);
              }*/
              | args ',' assocs opt_block_arg /*{
                  $$ = $1.add(new HashNode(getPosition(null), $3));
                  $$ = support.arg_blk_pass((Node)$$, $4);
              }*/
              | args ',' assocs ',' STAR arg opt_block_arg /*{
                  support.checkExpression($6);
                  $$ = support.arg_concat(getPosition($1), $1.add(new HashNode(getPosition(null), $3)), $6);
                  $$ = support.arg_blk_pass((Node)$$, $7);
              }*/
              | STAR arg_value opt_block_arg /*{
                  $$ = support.arg_blk_pass(new SplatNode(getPosition($1), $2), $3);
              }*/
              | block_arg /*{}*/
    ;

call_args2    : arg_value ',' args opt_block_arg /*{
                  $$ = support.arg_blk_pass(new ArrayNode(getPosition($1), $1).addAll($3), $4);
              }*/
              | arg_value ',' block_arg /*{
                  $$ = support.arg_blk_pass(new ArrayNode(getPosition($1), $1), $3);
              }*/
              | arg_value ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), new ArrayNode(getPosition($1), $1), $4);
                  $$ = support.arg_blk_pass((Node)$$, $5);
              }*/
              | arg_value ',' args ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), new ArrayNode(getPosition($1), $1).addAll(new HashNode(getPosition(null), $3)), $6);
                  $$ = support.arg_blk_pass((Node)$$, $7);
              }*/
              | assocs opt_block_arg /*{
                  $$ = new ArrayNode(getPosition($1), new HashNode(getPosition(null), $1));
                  $$ = support.arg_blk_pass((Node)$$, $2);
              }*/
              | assocs ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), new ArrayNode(getPosition($1), new HashNode(getPosition(null), $1)), $4);
                  $$ = support.arg_blk_pass((Node)$$, $5);
              }*/
              | arg_value ',' assocs opt_block_arg /*{
                  $$ = new ArrayNode(getPosition($1), $1).add(new HashNode(getPosition(null), $3));
                  $$ = support.arg_blk_pass((Node)$$, $4);
              }*/
              | arg_value ',' args ',' assocs opt_block_arg /*{
                  $$ = new ArrayNode(getPosition($1), $1).addAll($3).add(new HashNode(getPosition(null), $5));
                  $$ = support.arg_blk_pass((Node)$$, $6);
              }*/
              | arg_value ',' assocs ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), new ArrayNode(getPosition($1), $1).add(new HashNode(getPosition(null), $3)), $6);
                  $$ = support.arg_blk_pass((Node)$$, $7);
              }*/
              | arg_value ',' args ',' assocs ',' STAR arg_value opt_block_arg /*{
                  $$ = support.arg_concat(getPosition($1), new ArrayNode(getPosition($1), $1).addAll($3).add(new HashNode(getPosition(null), $5)), $8);
                  $$ = support.arg_blk_pass((Node)$$, $9);
              }*/
              | STAR arg_value opt_block_arg /*{
                  $$ = support.arg_blk_pass(new SplatNode(getPosition($1), $2), $3);
              }*/
              | block_arg /*{}*/
    ;

command_args  : /* none */ /*{ 
                  $$ = new Long(lexer.getCmdArgumentState().begin());
              }*/ open_args /*{
                  lexer.getCmdArgumentState().reset($<Long>1.longValue());
                  $$ = $2;
              }*/
    ;

 open_args    : call_args
              | LPAREN_ARG  /*{                    
                  lexer.setState(LexState.EXPR_ENDARG);
              }*/ RPAREN /*{
                  warnings.warn(getPosition($1), "don't put space before argument parentheses");
                  $$ = null;
              }*/
              | LPAREN_ARG call_args2 /*{
                  lexer.setState(LexState.EXPR_ENDARG);
              }*/ RPAREN /*{
                  warnings.warn(getPosition($1), "don't put space before argument parentheses");
                  $$ = $2;
              }*/
    ;

block_arg     : AMPER arg_value /*{
                  support.checkExpression($2);
                  $$ = new BlockPassNode(support.union($1, $2), $2);
              }*/
    ;

opt_block_arg : ',' block_arg /*{
                  $$ = $2;
              }*/
              | none_block_pass
    ;

args          : arg_value /*{
                  $$ = new ArrayNode(getPosition2($1), $1);
              }*/
              | args ',' arg_value /*{
                  $$ = $1.add($3);
              }*/
    ;

mrhs          : args ',' arg_value /*{
                  $$ = $1.add($3);
              }*/
              | args ',' STAR arg_value /*{
                  $$ = support.arg_concat(getPosition($1), $1, $4);
              }*/
              | STAR arg_value /*{  
                  $$ = new SplatNode(getPosition($1), $2);
              }*/
    ;

primary       : literal
              | strings
              | xstring 
              | regexp
              | words
              | qwords
              | var_ref
              | backref
              | FID /*{
                  $$ = new FCallNode($1.getPosition(), (String) $1.getValue(), null);
              }*/
              | BEGIN bodystmt END /*{
                  $$ = new BeginNode(support.union($1, $3), $2);
              }*/
              | LPAREN_ARG expr /*{ 
                  lexer.setState(LexState.EXPR_ENDARG); 
              }*/ opt_nl RPAREN /*{
                  warnings.warning(getPosition($1), "(...) interpreted as grouped expression");
                  $$ = $2;
              }*/
              | LPAREN compstmt RPAREN /*{
                  $$ = $2;
              }*/
              | primary_value COLON2 CONSTANT /*{
                  $$ = new Colon2Node(support.union($1, $3), $1, (String) $3.getValue());
              }*/
              | COLON3 CONSTANT /*{
                  $$ = new Colon3Node(support.union($1, $2), (String) $2.getValue());
              }*/
              | primary_value '[' aref_args RBRACK /*{
                  if ($1 instanceof SelfNode) {
                      $$ = new FCallNode(getPosition($1), "[]", $3);
                  } else {
                      $$ = new CallNode(getPosition($1), $1, "[]", $3);
                  }
              }*/
              | LBRACK aref_args RBRACK /*{
                  ISourcePosition position = support.union($1, $3);
                  if ($2 == null) {
                      $$ = new ZArrayNode(position); // zero length array
                  } else {
                      $$ = $2;
                      $<ISourcePositionHolder>$.setPosition(position);
                  }
              }*/
              | LBRACE assoc_list RCURLY /*{
                  $$ = new HashNode(support.union($1, $3), $2);
              }*/
              | RETURN /*{
                  $$ = new ReturnNode($1.getPosition(), null);
              }*/
              | YIELD LPAREN2 call_args RPAREN /*{
                  $$ = support.new_yield(support.union($1, $4), $3);
              }*/
              | YIELD LPAREN2 RPAREN /*{
                  $$ = new YieldNode(support.union($1, $3), null, false);
              }*/
              | YIELD /*{
                  $$ = new YieldNode($1.getPosition(), null, false);
              }*/
              | DEFINED opt_nl LPAREN2 expr RPAREN /*{
                  $$ = new DefinedNode(getPosition($1), $4);
              }*/
              | operation brace_block /*{
                  $$ = new FCallNode(support.union($1, $2), (String) $1.getValue(), null, $2);
              }*/
              | method_call
              | method_call brace_block /*{
                  if ($1 != null && 
                      $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(getPosition($1), "Both block arg and actual block given.");
                  }
                  $<BlockAcceptingNode>1.setIterNode($2);
                  $<Node>1.setPosition(support.union($1, $2));
              }*/
              | IF expr_value then compstmt if_tail END /*{
                  $$ = new IfNode(support.union($1, $6), support.getConditionNode($2), $4, $5);
              }*/
              | UNLESS expr_value then compstmt opt_else END /*{
                  $$ = new IfNode(support.union($1, $6), support.getConditionNode($2), $5, $4);
              }*/
              | WHILE /*{ 
                  lexer.getConditionState().begin();
              }*/ expr_value do /*{
                  lexer.getConditionState().end();
              }*/ compstmt END /*{
                  $$ = new WhileNode(support.union($1, $7), support.getConditionNode($3), $6);
              }*/
              | UNTIL /*{
                  lexer.getConditionState().begin();
              }*/ expr_value do /*{
                  lexer.getConditionState().end();
              }*/ compstmt END /*{
                  $$ = new UntilNode(getPosition($1), support.getConditionNode($3), $6);
              }*/
              | CASE expr_value opt_terms case_body END /*{
                  $$ = new CaseNode(support.union($1, $5), $2, $4);
              }*/
              | CASE opt_terms case_body END /*{
                  $$ = new CaseNode(support.union($1, $4), null, $3);
              }*/
              | CASE opt_terms ELSE compstmt END /*{
                  $$ = $4;
              }*/
              | FOR block_var IN /*{
                  lexer.getConditionState().begin();
              }*/ expr_value do /*{
                  lexer.getConditionState().end();
              }*/ compstmt END /*{
                  $$ = new ForNode(support.union($1, $9), $2, $8, $5);
              }*/
              | CLASS cpath superclass /*{
                  if (support.isInDef() || support.isInSingle()) {
                      yyerror("class definition in method body");
                  }
                  support.pushLocalScope();
              }*/ bodystmt END /*{
                  $$ = new ClassNode(support.union($1, $6), $<Colon3Node>2, support.getCurrentScope(), $5, $3);
                  support.popCurrentScope();
              }*/
              | CLASS LSHFT expr /*{
                  $$ = new Boolean(support.isInDef());
                  support.setInDef(false);
              }*/ term /*{
                  $$ = new Integer(support.getInSingle());
                  support.setInSingle(0);
                  support.pushLocalScope();
              }*/ bodystmt END /*{
                  $$ = new SClassNode(support.union($1, $8), $3, support.getCurrentScope(), $7);
                  support.popCurrentScope();
                  support.setInDef($<Boolean>4.booleanValue());
                  support.setInSingle($<Integer>6.intValue());
              }*/
              | MODULE cpath /*{
                  if (support.isInDef() || support.isInSingle()) { 
                      yyerror("module definition in method body");
                  }
                  support.pushLocalScope();
              }*/ bodystmt END /*{
                  $$ = new ModuleNode(support.union($1, $5), $<Colon3Node>2, support.getCurrentScope(), $4);
                  support.popCurrentScope();
              }*/
              | DEF fname /*{
                  support.setInDef(true);
                  support.pushLocalScope();
              }*/ f_arglist bodystmt END /*{
                    // NOEX_PRIVATE for toplevel
                  $$ = new DefnNode(support.union($1, $6), new ArgumentNode($2.getPosition(), (String) $2.getValue()), $<ArgsNode>4, support.getCurrentScope(), $5, Visibility.PRIVATE);
                  support.popCurrentScope();
                  support.setInDef(false);
              }*/
              | DEF singleton dot_or_colon /*{
                  lexer.setState(LexState.EXPR_FNAME);
              }*/ fname /*{
                  support.setInSingle(support.getInSingle() + 1);
                  support.pushLocalScope();
                  lexer.setState(LexState.EXPR_END); // force for args
              }*/ f_arglist bodystmt END /*{
                  $$ = new DefsNode(support.union($1, $9), $2, new ArgumentNode($5.getPosition(), (String) $5.getValue()), $<ArgsNode>7, support.getCurrentScope(), $8);
                  support.popCurrentScope();
                  support.setInSingle(support.getInSingle() - 1);
              }*/
              | BREAK /*{
                  $$ = new BreakNode($1.getPosition());
              }*/
              | NEXT /*{
                  $$ = new NextNode($1.getPosition());
              }*/
              | REDO /*{
                  $$ = new RedoNode($1.getPosition());
              }*/
              | RETRY /*{
                  $$ = new RetryNode($1.getPosition());
              }*/
    ;

primary_value : primary /*{
                  support.checkExpression($1);
                  $$ = $1;
              }*/
    ;
 
then          : term
              | ":"
              | THEN
              | term THEN
    ;

do            : term
              | ":"
              | DO_COND
    ;

if_tail       : opt_else 
              | ELSIF expr_value then compstmt if_tail /*{
                  //mirko: support.union($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1)) ?
                  $$ = new IfNode(getPosition($1), support.getConditionNode($2), $4, $5);
              }*/
    ;

opt_else      : none 
              | ELSE compstmt /*{
                  $$ = $2;
              }*/
    ;

block_var     : lhs
              | mlhs /*{}*/
    ;

opt_block_var : none
              | PIPE /* none */ PIPE /*{
                  $$ = new ZeroArgNode(support.union($1, $2));
              }*/
              | OROP /*{
                  $$ = new ZeroArgNode($1.getPosition());
              }*/
              | PIPE block_var PIPE /*{
                  $$ = $2;

                  // Include pipes on multiple arg type
                  if ($2 instanceof MultipleAsgnNode) {
                      $2.setPosition(support.union($1, $3));
                  } 
              }*/
    ;

do_block      : DO_BLOCK /*{
                  support.pushBlockScope();
              }*/ opt_block_var compstmt END /*{
                  $$ = new IterNode(support.union($1, $5), $3, support.getCurrentScope(), $4);
                  support.popCurrentScope();
              }*/
    ;

block_call    : command do_block /*{
                  if ($1 != null && 
                      $<BlockAcceptingNode>1.getIterNode() instanceof BlockPassNode) {
                      throw new SyntaxException(getPosition($1), "Both block arg and actual block given.");
                  }
                  $<BlockAcceptingNode>1.setIterNode($2);
                  $<Node>1.setPosition(support.union($1, $2));
              }*/
              | block_call DOT operation2 opt_paren_args /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
              | block_call COLON2 operation2 opt_paren_args /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
    ;

method_call   : operation paren_args /*{
                  $$ = support.new_fcall($1, $2, null);
              }*/
              | primary_value DOT operation2 opt_paren_args /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
              | primary_value COLON2 operation2 paren_args /*{
                  $$ = support.new_call($1, $3, $4, null);
              }*/
              | primary_value COLON2 operation3 /*{
                  $$ = support.new_call($1, $3, null, null);
              }*/
              | SUPER paren_args /*{
                  $$ = support.new_super($2, $1);
              }*/
              | SUPER /*{
                  $$ = new ZSuperNode($1.getPosition());
              }*/
    ;

// IterNode:brace_block - block invocation argument (foo >{...}< | foo >do end<) [!null]
brace_block   : LCURLY /*{
                  support.pushBlockScope();
              }*/ opt_block_var compstmt RCURLY /*{
                  $$ = new IterNode(support.union($1, $5), $3, support.getCurrentScope(), $4);
                  support.popCurrentScope();
              }*/
              | DO /*{
                  support.pushBlockScope();
              }*/ opt_block_var compstmt END /*{
                  $$ = new IterNode(support.union($1, $5), $3, support.getCurrentScope(), $4);
                  $<ISourcePositionHolder>0.setPosition(support.union($<ISourcePositionHolder>0, $<ISourcePositionHolder>$));
                  support.popCurrentScope();
              }*/
    ;

case_body     : WHEN when_args then compstmt cases /*{
                  $$ = new WhenNode(support.union($1, support.unwrapNewlineNode($4)), $2, $4, $5);
              }*/
    ;

when_args     : args
              | args ',' STAR arg_value /*{
                  $$ = $1.add(new WhenNode(getPosition($1), $4, null, null));
              }*/
              | STAR arg_value /*{
                  $$ = new ArrayNode(getPosition($1), new WhenNode(getPosition($1), $2, null, null));
              }*/
    ;

cases         : opt_else | case_body
    ;

opt_rescue    : RESCUE exc_list exc_var then compstmt opt_rescue /*{
                  Node node;
                  if ($3 != null) {
                     node = support.appendToBlock(support.node_assign($3, new GlobalVarNode(getPosition($1), "$!")), $5);
                     if($5 != null) {
                        node.setPosition(support.unwrapNewlineNode($5).getPosition());
                     }
                  } else {
                     node = $5;
                  }
                  $$ = new RescueBodyNode(getPosition($1, true), $2, node, $6);
              }*/
              | /*{$$ = null;}*/
    ;

exc_list      : arg_value /*{
                  $$ = new ArrayNode($1.getPosition(), $1);
              }*/
              | mrhs
              | none
    ;

exc_var       : ASSOC lhs /*{
                  $$ = $2;
              }*/
              | none
    ;

opt_ensure    : ENSURE compstmt /*{
                  if ($2 != null) {
                      $$ = $2;
                  } else {
                      $$ = new NilNode(getPosition(null));
                  }
              }*/
              | none
    ;

literal       : numeric
              | symbol /*{
                  $$ = new SymbolNode($1.getPosition(), (String) $1.getValue());
              }*/
              | dsym
    ;

strings       : string /*{
                  if ($1 instanceof EvStrNode) {
                      $$ = new DStrNode(getPosition($1)).add($1);
                  } else {
                      $$ = $1;
                  }
              }*/ 
    ;

string        : string1
              | string string1 /*{
                  $$ = support.literal_concat(getPosition($1), $1, $2);
              }*/
    ;

string1       : STRING_BEG string_contents STRING_END /*{
                  $$ = $2;
                  $<ISourcePositionHolder>$.setPosition(support.union($1, $3));
                  int extraLength = ((String) $1.getValue()).length() - 1;

                  // We may need to subtract addition offset off of first 
                  // string fragment (we optimistically take one off in
                  // ParserSupport.literal_concat).  Check token length
                  // and subtract as neeeded.
                  if (($2 instanceof DStrNode) && extraLength > 0) {
                     Node strNode = ((DStrNode)$2).get(0);
                     assert strNode != null;
                     strNode.getPosition().adjustStartOffset(-extraLength);
                  }
              }*/
    ;

xstring       : XSTRING_BEG xstring_contents STRING_END /*{
                  ISourcePosition position = support.union($1, $3);

                  if ($2 == null) {
                      $$ = new XStrNode(position, null);
                  } else if ($2 instanceof StrNode) {
                      $$ = new XStrNode(position, (ByteList) $<StrNode>2.getValue().clone());
                  } else if ($2 instanceof DStrNode) {
                      $$ = new DXStrNode(position, $<DStrNode>2);

                      $<Node>$.setPosition(position);
                  } else {
                      $$ = new DXStrNode(position).add($2);
                  }
              }*/
    ;

regexp        : REGEXP_BEG xstring_contents REGEXP_END /*{
                  int options = $3.getOptions();
                  Node node = $2;

                  if (node == null) {
                      $$ = new RegexpNode(getPosition($1), ByteList.create(""), options & ~ReOptions.RE_OPTION_ONCE);
                  } else if (node instanceof StrNode) {
                      $$ = new RegexpNode($2.getPosition(), (ByteList) ((StrNode) node).getValue().clone(), options & ~ReOptions.RE_OPTION_ONCE);
                  } else if (node instanceof DStrNode) {
                      $$ = new DRegexpNode(getPosition($1), (DStrNode) node, options, (options & ReOptions.RE_OPTION_ONCE) != 0);
                  } else {
                      $$ = new DRegexpNode(getPosition($1), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
                  }
               }*/
    ;

words          : WORDS_BEG ' ' STRING_END /*{
                   $$ = new ZArrayNode(support.union($1, $3));
               }*/
               | WORDS_BEG word_list STRING_END /*{
                   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition(support.union($1, $3));
               }*/
    ;

word_list      : /* none */ /*{
                   $$ = new ArrayNode(getPosition(null));
               }*/
               | word_list word ' ' /*{
                   $$ = $1.add($2 instanceof EvStrNode ? new DStrNode(getPosition($1)).add($2) : $2);
               }*/
    ;

word           : string_content
               | word string_content /*{
                   $$ = support.literal_concat(getPosition($1), $1, $2);
               }*/
    ;

qwords         : QWORDS_BEG ' ' STRING_END /*{
                   $$ = new ZArrayNode(support.union($1, $3));
               }*/
               | QWORDS_BEG qword_list STRING_END /*{
                   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition(support.union($1, $3));
               }*/
    ;

qword_list     : /* none */ /*{
                   $$ = new ArrayNode(getPosition(null));
               }*/
               | qword_list STRING_CONTENT ' ' /*{
                   $$ = $1.add($2);
               }*/
    ;

string_contents: /* none */ /*{
                   $$ = new StrNode($<Token>0.getPosition(), ByteList.create(""));
               }*/
               | string_contents string_content /*{
                   $$ = support.literal_concat(getPosition($1), $1, $2);
               }*/
    ;

xstring_contents: /* none */ /*{
                   $$ = null;
               }*/
               | xstring_contents string_content /*{
                   $$ = support.literal_concat(getPosition($1), $1, $2);
               }*/
    ;

string_content : STRING_CONTENT /*{
                   $$ = $1;
               }*/
               | STRING_DVAR /*{
                   $$ = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
               }*/ string_dvar /*{
                   lexer.setStrTerm($<StrTerm>2);
                   $$ = new EvStrNode(support.union($1, $3), $3);
               }*/
               | STRING_DBEG /*{
                   $$ = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.setState(LexState.EXPR_BEG);
               }*/ compstmt RCURLY /*{
                   lexer.setStrTerm($<StrTerm>2);

                   $$ = support.newEvStrNode(support.union($1, $4), $3);
               }*/
    ;

string_dvar    : GVAR /*{
                   $$ = new GlobalVarNode($1.getPosition(), (String) $1.getValue());
               }*/
               | IVAR /*{
                   $$ = new InstVarNode($1.getPosition(), (String) $1.getValue());
               }*/
               | CVAR /*{
                   $$ = new ClassVarNode($1.getPosition(), (String) $1.getValue());
               }*/
               | backref
    ;

symbol         : SYMBEG sym /*{
                   lexer.setState(LexState.EXPR_END);
                   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition(support.union($1, $2));
               }*/
    ;

sym            : fname | IVAR | GVAR | CVAR
    ;

dsym           : SYMBEG xstring_contents STRING_END /*{
                   lexer.setState(LexState.EXPR_END);

                   // DStrNode: :"some text #{some expression}"
                   // StrNode: :"some text"
                   // EvStrNode :"#{some expression}"
                   DStrNode node;

                   if ($2 == null) {
                       yyerror("empty symbol literal");
                   }

                   if ($2 instanceof DStrNode) {
                       $$ = new DSymbolNode(support.union($1, $3), $<DStrNode>2);
                   } else {
                       ISourcePosition position = support.union($2, $3);

                       // We substract one since tsymbeg is longer than one
                       // and we cannot union it directly so we assume quote
                       // is one character long and subtract for it.
                       position.adjustStartOffset(-1);
                       $2.setPosition(position);
                       
                       $$ = new DSymbolNode(support.union($1, $3));
                       $<DSymbolNode>$.add($2);
                   }
               }*/
    ;

// Node:numeric - numeric value [!null]
numeric        : INTEGER | FLOAT /*{
                   $$ = $1;
               }*/
               | UMINUS_NUM INTEGER          /*%prec LOWEST*/ /*{
                   $$ = support.negateInteger($2);
               }*/
               | UMINUS_NUM FLOAT            /*%prec LOWEST*/ /*{
                   $$ = support.negateFloat($2);
               }*/
    ;

// Token:variable - name (special and normal onces)
variable       : IDENTIFIER | IVAR | GVAR | CONSTANT | CVAR
               | NIL /*{ 
                   $$ = new Token("nil", $1.getPosition());
               }*/
               | SELF /*{
                   $$ = new Token("self", $1.getPosition());
               }*/
               | TRUE /*{ 
                   $$ = new Token("true", $1.getPosition());
               }*/
               | FALSE /*{
                   $$ = new Token("false", $1.getPosition());
               }*/
               | U_FILE_U /*{
                   $$ = new Token("__FILE__", $1.getPosition());
               }*/
               | U_LINE_U /*{
                   $$ = new Token("__LINE__", $1.getPosition());
               }*/
    ;

var_ref        : variable /*{
                   $$ = support.gettable((String) $1.getValue(), $1.getPosition());
               }*/
    ;

var_lhs        : variable /*{
                   $$ = support.assignable($1, null);
               }*/
    ;

backref        : NTH_REF | BACK_REF
    ;

superclass     : term /*{
                   $$ = null;
               }*/
               | LT /*{
                   lexer.setState(LexState.EXPR_BEG);
               }*/ expr_value term /*{
                   $$ = $3;
               }*/
               | error term /*{
                   yyerrok();
                   $$ = null;
               }*/
    ;

// f_arglist: Function Argument list for definitions
f_arglist      : LPAREN2 f_args opt_nl RPAREN /*{
                   $$ = $2;
                   $<ISourcePositionHolder>$.setPosition(support.union($1, $4));
                   lexer.setState(LexState.EXPR_BEG);
               }*/
               | f_args term /*{
                   $$ = $1;
               }*/
    ;

f_args         : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg /*{
                   $$ = new ArgsNode(support.union($1, $6), $1, $3, ((Integer) $5.getValue()).intValue(), $6);
               }*/
               | f_arg ',' f_optarg opt_f_block_arg /*{
                   $$ = new ArgsNode(getPosition($1), $1, $3, -1, $4);
               }*/
               | f_arg ',' f_rest_arg opt_f_block_arg /*{
                   $$ = new ArgsNode(support.union($1, $4), $1, null, ((Integer) $3.getValue()).intValue(), $4);
               }*/
               | f_arg opt_f_block_arg /*{
                   $$ = new ArgsNode($<ISourcePositionHolder>1.getPosition(), $1, null, -1, $2);
               }*/
               | f_optarg ',' f_rest_arg opt_f_block_arg /*{
                   $$ = new ArgsNode(getPosition($1), null, $1, ((Integer) $3.getValue()).intValue(), $4);
               }*/
               | f_optarg opt_f_block_arg /*{
                   $$ = new ArgsNode(getPosition($1), null, $1, -1, $2);
               }*/
               | f_rest_arg opt_f_block_arg /*{
                   $$ = new ArgsNode(getPosition($1), null, null, ((Integer) $1.getValue()).intValue(), $2);
               }*/
               | f_block_arg /*{
                   $$ = new ArgsNode(getPosition($1), null, null, -1, $1);
               }*/
               | /* none */ /*{
                   $$ = new ArgsNode(support.createEmptyArgsNodePosition(getPosition(null)), null, null, -1, null);
               }*/
    ;

f_norm_arg     : CONSTANT /*{
                   yyerror("formal argument cannot be a constant");
               }*/
               | IVAR /*{
                   yyerror("formal argument cannot be an instance variable");
               }*/
               | CVAR /*{
                   yyerror("formal argument cannot be a class variable");
               }*/
               | IDENTIFIER /*{
                   String identifier = (String) $1.getValue();
                   if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                       yyerror("formal argument must be local variable");
                   } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate argument name");
                   }

                   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   $$ = $1;
               }*/
    ;

f_arg          : f_norm_arg /*{
                    $$ = new ListNode($<ISourcePositionHolder>1.getPosition());
                    ((ListNode) $$).add(new ArgumentNode($<ISourcePositionHolder>1.getPosition(), (String) $1.getValue()));
               }*/
               | f_arg ',' f_norm_arg /*{
                   $1.add(new ArgumentNode($<ISourcePositionHolder>3.getPosition(), (String) $3.getValue()));
                   $1.setPosition(support.union($1, $3));
                   $$ = $1;
               }*/
    ;

f_opt          : IDENTIFIER '=' arg_value /*{
                   String identifier = (String) $1.getValue();

                   if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                       yyerror("formal argument must be local variable");
                   } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                       yyerror("duplicate optional argument name");
                   }
                   support.getCurrentScope().getLocalScope().addVariable(identifier);
                   $$ = support.assignable($1, $3);
              }*/
    ;

f_optarg      : f_opt /*{
                  $$ = new BlockNode(getPosition($1)).add($1);
              }*/
              | f_optarg ',' f_opt /*{
                  $$ = support.appendToBlock($1, $3);
              }*/
    ;

restarg_mark  : STAR2 | STAR
    ;

f_rest_arg    : restarg_mark IDENTIFIER /*{
                  String identifier = (String) $2.getValue();

                  if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                      yyerror("rest argument must be local variable");
                   } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate rest argument name");
                  }
                  $1.setValue(new Integer(support.getCurrentScope().getLocalScope().addVariable(identifier)));
                  $$ = $1;
              }*/
              | restarg_mark /*{
                  $1.setValue(new Integer(-2));
                  $$ = $1;
              }*/
    ;

blkarg_mark   : AMPER2 | AMPER
    ;

f_block_arg   : blkarg_mark IDENTIFIER /*{
                  String identifier = (String) $2.getValue();

                  if (IdUtil.getVarType(identifier) != IdUtil.LOCAL_VAR) {
                      yyerror("block argument must be local variable");
                  } else if (support.getCurrentScope().getLocalScope().isDefined(identifier) >= 0) {
                      yyerror("duplicate block argument name");
                  }
                  $$ = new BlockArgNode(support.union($1, $2), support.getCurrentScope().getLocalScope().addVariable(identifier), identifier);
              }*/
    ;

opt_f_block_arg: ',' f_block_arg /*{
                  $$ = $2;
              }*/
              | /* none */ /*{
                  $$ = null;
              }*/
    ;

singleton     : var_ref /*{
                  if (!($1 instanceof SelfNode)) {
                      support.checkExpression($1);
                  }
                  $$ = $1;
              }*/
              | LPAREN2 /*{
                  lexer.setState(LexState.EXPR_BEG);
              }*/ expr opt_nl RPAREN /*{
                  if ($3 instanceof ILiteralNode) {
                      yyerror("Can't define single method for literals.");
                  }
                  support.checkExpression($3);
                  $$ = $3;
              }*/
    ;

// ListNode:assoc_list - list of hash values pairs, like assocs but also
//   will accept ordinary list-style (e.g. a,b,c,d or a=>b,c=>d) [?null]
assoc_list    : none /*{ // [!null]
                  $$ = new ArrayNode(getPosition(null));
              }*/
              | assocs trailer /*{ // [!null]
                  $$ = $1;
              }*/
              | args trailer /*{
                  if ($1.size() % 2 != 0) {
                      yyerror("Odd number list for Hash.");
                  }
                  $$ = $1;
              }*/
    ;

// ListNode:assocs - list of hash value pairs (e.g. a => b, c => d) [!null]
assocs        : assoc // [!null]
              | assocs ',' assoc /*{ // [!null]
                  $$ = $1.addAll($3);
              }*/
    ;

// ListNode:assoc - A single hash value pair (e.g. a => b) [!null]
assoc         : arg_value ASSOC arg_value /*{ // [!null]
                  $$ = new ArrayNode(support.union($1, $3), $1).add($3);
              }*/
    ;

operation     : IDENTIFIER | CONSTANT | FID
    ;

operation2    : IDENTIFIER | CONSTANT | FID | op
    ;

operation3    : IDENTIFIER | FID | op
    ;

dot_or_colon  : DOT | COLON2
    ;

opt_terms     : /* none */ | terms
    ;

opt_nl        : /* none */ | '\n'
    ;

trailer       : /* none */ | '\n' | ','
    ;

term          : ';' /*{
                  yyerrok();
              }*/
              | '\n'
    ;

terms         : term
              | terms ';' /*{
                  yyerrok();
              }*/
    ;

none          : /* none */ /*{
                  $$ = null;
              }*/
    ;

none_block_pass: /* none */ /*{  
                  $$ = null;
              }*/
    ;
