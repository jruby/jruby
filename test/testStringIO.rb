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

strio = StringIO.new("a")
strio.gets
test_equal(nil, strio.gets)
test_equal(true, strio.eof?)

$/ = ':'
strio = StringIO.new("a:b")
test_equal('a:', strio.gets)
