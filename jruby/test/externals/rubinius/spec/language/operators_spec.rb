require File.dirname(__FILE__) + '/../spec_helper'

=begin
(Top to bottom)
:: .
[]
**
-(unary) +(unary) ! ~
*  /  %
+  -
<<  >>
&
|  ^
>  >=  <  <=
<=> == === != =~ !~
&&
||
.. ...
=(+=, -=...)
not
and or

All of the above are just methods except these:

=, ::, ., .., ..., !, not, &&, and, ||, or, !=, !~

In addition, assignment operators(+= etc.) are not user-definable.
=end

# TODO : Check the operations and precedence 
context "Operators" do

end
