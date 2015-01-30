@bin\jruby -X+T ^
  -Xparser.warn.useless_use_of=false ^
  -Xparser.warn.not_reached=false ^
  -Xparser.warn.grouped_expressions=false ^
  -Xparser.warn.shadowing_local=false ^
  -Xparser.warn.regex_condition=false ^
  -Xparser.warn.argument_prefix=false ^
  -Xparser.warn.ambiguous_argument=false ^
  -J-ea -J-Xmx2G %*
