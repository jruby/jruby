require 'test/minirunit'
test_check "Test Variables and method:"
a = String.new("Hello World")
b = a.reverse
c = " "
d = "Hello".reverse
e = a[6, 5].reverse

f = 100 + 35
g =  2 * 10
h = 13 % 5
test_ok("Hello World" ==  a)
test_ok("dlroW olleH" == b)
test_ok("Hello" == d.reverse)
test_ok("World" == e.reverse)
test_ok(135 == f)
test_ok(20 == g)
test_ok(3 == h)
$a = a
$b = b
$c = c
$d = d
$e = e
$f = f
$g = g
$h = h
NIL_CONSTANT=nil
test_ok(NIL_CONSTANT == nil)
test_exception(NameError) { NO_SUCH_CONSTANT }

x, (y, z) = 1, 2, 3; test_ok([1,2,nil] == [x,y,z])
