require 'minirunit'
test_check "Test Exception (2)"

test_ok(ArgumentError < Exception)

e = nil
begin
  raise ArgumentError.new("hello")
rescue ArgumentError
  e = $!
end
test_ok(e.kind_of?(ArgumentError))

e = nil
begin
  raise "hello"
rescue RuntimeError
  e = $!
end
test_ok(e.kind_of?(RuntimeError))

e = nil
type = ArgumentError
begin
  raise ArgumentError.new("hello")
rescue type
  e = $!
end
test_ok(e.kind_of?(ArgumentError))
