require 'test/minirunit'
test_check "Test Exception (2)"

def raise_test(thing_to_raise, exception_to_rescue)
  e = nil
  begin
    raise thing_to_raise
  rescue exception_to_rescue
    e = $!
  end
  test_equal(exception_to_rescue, e.class)
end

test_ok(ArgumentError < Exception)

raise_test(ArgumentError.new("hello"), ArgumentError)
raise_test("hello", RuntimeError)

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

class Foo
end

class Har 
  def exception(message)
    Bar.new(message)
  end
end

class Bar < Exception
  def exception(message)
    1
  end
end

class Gar < Exception
  def exception(message)
     Bar.new(message)
  end
end

test_exception(TypeError) { raise nil }
test_exception(TypeError) { raise Foo }
test_exception(TypeError) { raise Foo, "HEH" }
test_exception(TypeError) { raise Foo, "HEH", caller }
test_exception(TypeError) { raise Har }
test_exception(TypeError) { raise Har, "HEH" }
test_exception(TypeError) { raise Har, "HEH", caller }
test_exception(Bar) { raise Bar }
test_exception(Bar) { raise Bar, "HEH" }
test_exception(Bar) { raise Bar, "HEH", caller }
test_exception(Bar) { raise Gar.new, "HEH" }
test_exception(TypeError) { raise Bar.new, "HEH" }
test_exception(TypeError) { raise Bar.new, "HEH", caller }

