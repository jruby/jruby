require 'test/minirunit'

test_check "Test here document:"

# Multiple Heredocs as arguments
multiDoc = <<-STRING1, <<-STRING2
   Concat
   STRING1
      enate
      STRING2

test_equal(["   Concat\n","      enate\n"], multiDoc)

# Should retain extra newlines at end of heredocs
str = <<-EOL
blah-blah


EOL

test_equal("blah-blah\n\n\n",str)

# - (indent) operation should result in same string
# as non-indented version
str1 = <<EOL
baw-waw


EOL

str2 = <<-EOL
baw-waw


              EOL
              
test_equal(str1, str2)

$global = "global value"
s =<<EOT
#$global
EOT
test_equal($global + "\n", s)

value = "some value"
value.sub!(/\A(\S*)(.*?)(\S*)\Z/m) do |m|
  <<EOT
#$global
EOT
end
test_equal($global + "\n", value)

value = "some value"
value.sub!(/\A(\S*)(.*?)(\S*)\Z/m) do |m|
  <<EOT
#{$1} other #{$3}
EOT
end
test_equal("some other value\n", value)

value = "some value"
value.sub!(/\A(\S*)(.*?)(\S*)\Z/m) do |m|
  <<EOT
#$1 other #$3
EOT
end
test_equal("some other value\n", value)

value = "some value"
value.sub!(/\A(\S*)(.*?)(\S*)\Z/m) do |m|
  <<"EOT"
#{$1} other #{$3}
EOT
end
test_equal("some other value\n", value)

value = "some value"
value.sub!(/\A(\S*)(.*?)(\S*)\Z/m) do |m|
  <<'EOT'
#$1 other #$3
EOT
end
test_equal('#$1 other #$3', value.chomp)

# Note: This test must be last test of this file and the EOF beneath it must
# not end in a newline.  Test HEREDOC terminating without newline.
str = <<EOF
 some text
EOF
