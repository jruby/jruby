require 'minirunit'
test_check "Test Thread:"
aProc = proc { 
	$toto = 1
}
thread = Thread.new &aProc
thread.join
test_equal(1, $toto)

v = nil
t = Thread.new { v = 1 }
t.join
test_equal(1, v)

t = Thread.new(10) {|argument| v = argument }
t.join
test_equal(10, v)

#t = Thread.current
#test_equal(t, Thread.current)

#v = nil
#t = Thread.new {
#  Thread.current[:x] = 1234
#  test_equal(1234, Thread.current[:x])
#  test_equal(nil, Thread.current[:y])
#  test_ok(Thread.current.key?(:x))
#  test_ok(! Thread.current.key?(:y))
#}
#test_ok(! Thread.current.key?(:x))
