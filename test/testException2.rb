require 'test/minirunit'
test_check "Test Exception (2)"

test_ok(ArgumentError < Exception)

e = nil
begin
  raise ArgumentError.new("hello")
rescue ArgumentError
  e = $!
end
test_equal(ArgumentError, e.class)

e = nil
begin
  raise "hello"
rescue RuntimeError
  e = $!
end
test_equal(RuntimeError, e.class)

e = nil
type = ArgumentError
begin
  raise ArgumentError.new("hello")
rescue type
  e = $!
end
test_equal(ArgumentError, e.class)

class SomeOtherException < StandardError
end
e = Exception.new
test_ok(!e.kind_of?(SomeOtherException))
test_ok(!e.kind_of?(StandardError))
test_ok(e.kind_of?(Exception))
begin
  raise "whoah!"
rescue SomeOtherException
  test_fail()
rescue Exception
  test_ok(true)
end
