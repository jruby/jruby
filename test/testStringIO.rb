require 'test/minirunit'
require 'stringio'

string = <<EOF
one
two
three
EOF

strio = StringIO.new(string)

lines = []
3.times {lines << strio.readline}

test_equal(["one\n", "two\n", "three\n"], lines)

##### gets #####

strio = StringIO.new("a")
test_equal("a", strio.gets)
test_equal(nil, strio.gets)
test_equal(true, strio.eof?)

$/ = ':'
strio = StringIO.new("a:b")
test_equal('a:', strio.gets)

s = StringIO.new
$\=':'
s.puts(1,2,3)
s.print(1,2,3)
# $_ is getting lost or not working
#$_='G'
#s.print
s.printf("a %s b\n", 1)

test_equal(<<EOF, s.string)
1
2
3
123:a 1 b
EOF

s = StringIO.new("12345")
test_equal(5, s.size)

$/="\n"
saved_stdin = $stdin
$stdin = StringIO.new("HEH\nWorld\n")
test_equal("HEH\n", gets)
$stdin = saved_stdin
