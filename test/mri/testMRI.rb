require 'minirunit'

if defined? Process.kill
  test_check "signal"

  $x = 0
  trap "SIGINT", proc{|sig| $x = 2}
  Process.kill "SIGINT", $$
  sleep 0.1
  test_ok($x == 2)

  trap "SIGINT", proc{raise "Interrupt"}

  x = false
  begin
    Process.kill "SIGINT", $$
    sleep 0.1
  rescue
    x = $!
  end
  test_ok(x && /Interrupt/ =~ x)
end

test_check "eval"
test_ok(eval("") == nil)
$bad=false
eval 'while false; $bad = true; print "foo\n" end'
test_ok(!$bad)

test_ok(eval('TRUE'))
test_ok(eval('true'))
test_ok(!eval('NIL'))
test_ok(!eval('nil'))
test_ok(!eval('FALSE'))
test_ok(!eval('false'))

$foo = 'test_ok(true)'
begin
  eval $foo
rescue
  test_ok(false)
end

test_ok(eval("$foo") == 'test_ok(true)')
test_ok(eval("true") == true)
i = 5
test_ok(eval("i == 5"))
test_ok(eval("i") == 5)
test_ok(eval("defined? i"))

# eval with binding
def test_ev
  local1 = "local1"
  lambda {
    local2 = "local2"
    return binding
  }.call
end

$x = test_ev
test_ok(eval("local1", $x) == "local1") # normal local var
test_ok(eval("local2", $x) == "local2") # nested local var
$bad = true
begin
  p eval("local1")
rescue NameError		# must raise error
  $bad = false
end
test_ok(!$bad)

module EvTest
  EVTEST1 = 25
  evtest2 = 125
  $x = binding
end
test_ok(eval("EVTEST1", $x) == 25)	# constant in module
test_ok(eval("evtest2", $x) == 125)	# local var in module
$bad = true
begin
  eval("EVTEST1")
rescue NameError		# must raise error
  $bad = false
end
test_ok(!$bad)

x = proc{}
eval "i4 = 1", x
test_ok(eval("i4", x) == 1)
x = proc{proc{}}.call
eval "i4 = 22", x
test_ok(eval("i4", x) == 22)
$x = []
x = proc{proc{}}.call
eval "(0..9).each{|i5| $x[i5] = proc{i5*2}}", x
test_ok($x[4].call == 8)

x = binding
eval "i = 1", x
test_ok(eval("i", x) == 1)
x = proc{binding}.call
eval "i = 22", x
test_ok(eval("i", x) == 22)
$x = []
x = proc{binding}.call
eval "(0..9).each{|i5| $x[i5] = proc{i5*2}}", x
test_ok($x[4].call == 8)
x = proc{binding}.call
eval "for i6 in 1..1; j6=i6; end", x
test_ok(eval("defined? i6", x))
test_ok(eval("defined? j6", x))

proc {
  p = binding
  eval "foo11 = 1", p
  foo22 = 5
  proc{foo11=22}.call
  proc{foo22=55}.call
  test_ok(eval("foo11", p) == eval("foo11"))
  test_ok(eval("foo11") == 1)
  test_ok(eval("foo22", p) == eval("foo22"))
  test_ok(eval("foo22") == 55)
}.call

p1 = proc{i7 = 0; proc{i7}}.call
test_ok(p1.call == 0)
eval "i7=5", p1
test_ok(p1.call == 5)
test_ok(!defined?(i7))

p1 = proc{i7 = 0; proc{i7}}.call
i7 = nil
test_ok(p1.call == 0)
eval "i7=1", p1
test_ok(p1.call == 1)
eval "i7=5", p1
test_ok(p1.call == 5)
test_ok(i7 == nil)

test_check "system"
test_ok(`echo foobar` == "foobar\n")
test_ok(`./miniruby -e 'print "foobar"'` == 'foobar')

tmp = open("script_tmp", "w")
tmp.print "print $zzz\n";
tmp.close

test_ok(`./miniruby -s script_tmp -zzz` == 'true')
test_ok(`./miniruby -s script_tmp -zzz=555` == '555')

tmp = open("script_tmp", "w")
tmp.print "#! /usr/local/bin/ruby -s\n";
tmp.print "print $zzz\n";
tmp.close

test_ok(`./miniruby script_tmp -zzz=678` == '678')

tmp = open("script_tmp", "w")
tmp.print "this is a leading junk\n";
tmp.print "#! /usr/local/bin/ruby -s\n";
tmp.print "print $zzz\n";
tmp.print "__END__\n";
tmp.print "this is a trailing junk\n";
tmp.close

test_ok(`./miniruby -x script_tmp` == 'nil')
test_ok(`./miniruby -x script_tmp -zzz=555` == '555')

tmp = open("script_tmp", "w")
for i in 1..5
  tmp.print i, "\n"
end
tmp.close

`./miniruby -i.bak -pe 'sub(/^[0-9]+$/){$&.to_i * 5}' script_tmp`
done = true
tmp = open("script_tmp", "r")
while tmp.gets
  if $_.to_i % 5 != 0
    done = false
    break
  end
end
tmp.close
test_ok(done)
  
File.unlink "script_tmp" or `/bin/rm -f "script_tmp"`
File.unlink "script_tmp.bak" or `/bin/rm -f "script_tmp.bak"`

$bad = false
for script in Dir["{lib,sample,ext}/**/*.rb"]
  `./miniruby -c #{script}`
  unless $?
    $bad = true
  end
end
test_ok(!$bad)

test_check "const"
TEST1 = 1
TEST2 = 2

module Const
  TEST3 = 3
  TEST4 = 4
end

module Const2
  TEST3 = 6
  TEST4 = 8
end

include Const

test_ok([TEST1,TEST2,TEST3,TEST4] == [1,2,3,4])

include Const2
STDERR.print "intentionally redefines TEST3, TEST4\n" if $VERBOSE
test_ok([TEST1,TEST2,TEST3,TEST4] == [1,2,6,8])

test_check "clone"
foo = Object.new
def foo.test
  "test"
end
bar = foo.clone
def bar.test2
  "test2"
end

test_ok(bar.test2 == "test2")
test_ok(bar.test == "test")
test_ok(foo.test == "test")  

begin
  foo.test2
  test_ok false
rescue NameError
  test_ok true
end

test_check "marshal"
$x = [1,2,3,[4,5,"foo"],{1=>"bar"},2.5,fact(30)]
$y = Marshal.dump($x)
test_ok($x == Marshal.load($y))

test_check "pack"

$format = "c2x5CCxsdils_l_a6";
# Need the expression in here to force ary[5] to be numeric.  This avoids
# test2 failing because ary2 goes str->numeric->str and ary does not.
ary = [1,-100,127,128,32767,987.654321098 / 100.0,12345,123456,-32767,-123456,"abcdef"]
$x = ary.pack($format)
ary2 = $x.unpack($format)

test_ok(ary.length == ary2.length)
test_ok(ary.join(':') == ary2.join(':'))
test_ok($x =~ /def/)

test_check "math"
test_ok(Math.sqrt(4) == 2)

include Math
test_ok(sqrt(4) == 2)

test_check "struct"
struct_test = Struct.new("Test", :foo, :bar)
test_ok(struct_test == Struct::Test)

test = struct_test.new(1, 2)
test_ok(test.foo == 1 && test.bar == 2)
test_ok(test[0] == 1 && test[1] == 2)

a, b = test.to_a
test_ok(a == 1 && b == 2)

test[0] = 22
test_ok(test.foo == 22)

test.bar = 47
test_ok(test.bar == 47)

test_check "variable"
test_ok($$.instance_of?(Fixnum))

# read-only variable
begin
  $$ = 5
  test_ok false
rescue NameError
  test_ok true
end

foobar = "foobar"
$_ = foobar
test_ok($_ == foobar)

test_check "trace"
$x = 1234
$y = 0
trace_var :$x, proc{$y = $x}
$x = 40414
test_ok($y == $x)

untrace_var :$x
$x = 19660208
test_ok($y != $x)

trace_var :$x, proc{$x *= 2}
$x = 5
test_ok($x == 10)

untrace_var :$x

test_check "defined?"

test_ok(defined?($x))		# global variable
test_ok(defined?($x) == 'global-variable')# returns description

foo=5
test_ok(defined?(foo))		# local variable

test_ok(defined?(Array))		# constant
test_ok(defined?(Object.new))	# method
test_ok(!defined?(Object.print))	# private method
test_ok(defined?(1 == 2))		# operator expression

def defined_test
  return !defined?(yield)
end

test_ok(defined_test)		# not iterator
test_ok(!defined_test{})		# called as iterator

test_check "alias"
class Alias0
  def foo; "foo" end
end
class Alias1<Alias0
  alias bar foo
  def foo; "foo+" + super end
end
class Alias2<Alias1
  alias baz foo
  undef foo
end

x = Alias2.new
test_ok(x.bar == "foo")
test_ok(x.baz == "foo+foo")

# test_check for cache
test_ok(x.baz == "foo+foo")

class Alias3<Alias2
  def foo
    defined? super
  end
  def bar
    defined? super
  end
  def quux
    defined? super
  end
end
x = Alias3.new
test_ok(!x.foo)
test_ok(x.bar)
test_ok(!x.quux)

test_check "gc"
begin
  1.upto(10000) {
    tmp = [0,1,2,3,4,5,6,7,8,9]
  }
  tmp = nil
  test_ok true
rescue
  test_ok false
end

if $failed > 0
  printf "test: %d failed %d\n", $ntest, $failed
else
  printf "end of test(test: %d)\n", $ntest
end
