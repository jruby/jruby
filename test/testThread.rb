require 'minirunit'
test_check "Test Thread:"
thread = Thread.new {
  $toto = 1
}
thread.join
test_equal(1, $toto)
test_equal(false, thread.status)

v = nil
t = Thread.new { v = 1 }
t.join
test_equal(1, v)

t = Thread.new(10) {|argument| v = argument }
t.join
test_equal(10, v)

t = Thread.current
test_equal(t, Thread.current)

v = nil
t = Thread.new {
  Thread.current[:x] = 1234
  test_equal(1234, Thread.current[:x])
  test_equal(nil, Thread.current[:y])
  test_ok(Thread.current.key?(:x))
  test_ok(! Thread.current.key?(:y))
}
t.join
test_ok(! Thread.current.key?(:x))

v = nil
t = Thread.new {
  v = Thread.current.status
}
t.join
test_equal("run", v)

def thread_foo()
  raise "hello"
end
e = nil
t = Thread.new {
  thread_foo()
}
begin
  t.join
rescue RuntimeError => error
  e = error
end
test_ok(! e.nil?)
test_equal(nil, t.status)

e = nil
begin
  Thread.current.join
rescue ThreadError => error
  e = error
end
test_ok(! e.nil?)
