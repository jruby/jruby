require 'test/minirunit'

test_check "Test here document:"
aString = <<END_OF_STRING
    The body of the string
    is the input lines up to
    one ending with the same
    text that followed the '<<'
END_OF_STRING

result = 
"    The body of the string
    is the input lines up to
    one ending with the same
    text that followed the '<<'
"
test_equal("    The body of the string
    is the input lines up to
    one ending with the same
    text that followed the '<<'
", aString)

multiDoc = <<-STRING1, <<-STRING2
   Concat
   STRING1
      enate
      STRING2

test_equal(["   Concat\n","      enate\n"], multiDoc)

str = <<-EOL
blah-blah


EOL

test_equal("blah-blah\n\n\n",str)

str1 = <<EOL
baw-waw


EOL

str2 = <<-EOL
baw-waw


              EOL
              
test_equal(str1, str2)