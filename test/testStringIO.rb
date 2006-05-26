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

###### StringIO#new, StringIO#open ######

io = StringIO.new("foo")
test_equal(102, io.getc)
test_equal(1, io.pos)
test_equal(3, io.size)
io << "bar"
test_equal(4, io.size)
test_equal(4, io.pos)
io.rewind
test_equal("fbar", io.gets)

StringIO.open("foo"){|io| 
  test_equal("foo", io.string)
}

###### close, close_read?, close_write? ######
s = StringIO.open("A")
test_equal(false, s.closed?)
s.close_read
test_equal(false, s.closed?)
s.close_write
test_equal(true, s.closed?)

###### fcntl ######
test_exception(NotImplementedError) { StringIO.new("").fcntl() }

###### read ######
io = StringIO.new("A")
test_equal(false, io.eof?)
test_equal("A", io.read(1))
test_equal(true, io.eof?)
test_equal(nil, io.read(1))

###### write ######

io = StringIO.new("a")
io.getc
test_equal(2, io.write("bc"))
io.rewind
test_equal("abc", io.string)

###### Misc. ######
$/="\n"
saved_stdin = $stdin
$stdin = StringIO.new("HEH\nWorld\n")
test_equal("HEH\n", gets)
$stdin = saved_stdin

n = StringIO.new
old_stdout = $stdout
$stdout = n
test_equal($>, $stdout)
puts "HEL\nEEEE\n"
n.rewind
test_equal("HEL\n", n.gets)
$stdout = old_stdout
n = StringIO.new
$> = n
puts "HEL\nEEEE\n"
n.rewind
test_equal("HEL\n", n.gets)

n = StringIO.new("123\n456\n789\n")
test_equal("123\n456\n789\n", n.gets(nil))
$/="\n"
saved_stdin = $stdin
$stdin = StringIO.new("HEH\nWorld\n")
#test_equal("HEH\n", gets)
$stdin = saved_stdin

n = StringIO.new
old_stdout = $stdout
$stdout = n
test_equal($>, $stdout)
puts "HEL\nEEEE\n"
n.rewind
test_equal("HEL\n", n.gets)
$stdout = old_stdout
n = StringIO.new
$> = n
puts "HEL\nEEEE\n"
n.rewind
test_equal("HEL\n", n.gets)

n = StringIO.new("123\n456\n789\n")
test_equal("123\n456\n789\n", n.gets(nil))
