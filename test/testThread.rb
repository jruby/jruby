require 'test/minirunit'
test_check "Test Thread:"

# Running and finishing
thread = Thread.new {
  $toto = 1
}
thread.join
test_equal(1, $toto)
test_equal(false, thread.status)

# Local variables
v = nil
t = Thread.new { v = 1 }
t.join
test_equal(1, v)

# Taking arguments
t = Thread.new(10) {|argument| v = argument }
t.join
test_equal(10, v)

# Current
t = Thread.current
test_equal(t, Thread.current)

# Thread-local variables
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

# Status
v = nil
t = Thread.new {
  v = Thread.current.status
}
t.join
test_equal("run", v)
test_equal(false, t.status)

# Error handling
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

# Joining itself
e = nil
begin
  Thread.current.join
rescue ThreadError => error
  e = error
end
test_ok(! e.nil?)

# raise
e = nil
t = Thread.new {
  while true
    Thread.pass
  end
}
t.raise("Die")
begin
  t.join
rescue RuntimeError => error
  e = error
end
test_ok(e.kind_of?(RuntimeError))
