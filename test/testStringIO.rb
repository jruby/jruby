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

# JRUBY-214: new's arg 0 should have to_str called if not String, else TypeError is thrown
test_exception(TypeError) { StringIO.new(Object.new) }

StringIO.open("foo"){|io| 
  test_equal("foo", io.string)
}

# JRUBY-214: open's arg 0 should have to_str called if not String, else TypeError is thrown
test_exception(TypeError) { StringIO.open(Object.new){|io|} }

###### close, reopen, close_read?, close_write? ######
s = StringIO.open("A")
test_equal(false, s.closed?)
s.close_read
test_equal(false, s.closed?)
s.close_write
test_equal(true, s.closed?)
s.reopen("B")
test_equal(false, s.closed?)

# JRUBY-214: reopen's arg 0 should have to_str called if not String, else TypeError is thrown
s = StringIO.open("A")
s.close_read
s.close_write
test_exception(TypeError) { s.reopen(Object.new) }
class Foo
  def to_str
    "abc"
  end
end
test_no_exception { s.reopen(Foo.new) }
test_equal("abc", s.read(3))

###### fcntl ######
test_exception(NotImplementedError) { StringIO.new("").fcntl() }

###### read ######
io = StringIO.new("A")
test_equal(false, io.eof?)
test_equal("A", io.read(1))
test_equal(true, io.eof?)
test_equal(nil, io.read(1))

#JRUBY-114: read with buffer sets buffer to value read (previously appended to buffer)
io = StringIO.new("A")
buf = "abc"
test_equal("A", io.read(1, buf))
test_equal("A", buf)

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

n = StringIO.new
n.puts
n.rewind
test_equal("\n", n.gets)
test_equal(nil, n.gets)
test_equal(true, n.eof?)

n = StringIO.new
