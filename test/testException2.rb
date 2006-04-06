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

# empty rescue block should cause method to return nil
def do_except
  raise Exception.new
rescue Exception
end

test_equal(nil, do_except)

# Check exception hierarchy structure
test_ok(NoMemoryError < Exception)
test_ok(ScriptError < Exception)
test_ok(LoadError < ScriptError)
test_ok(NotImplementedError < ScriptError)
test_ok(SyntaxError < ScriptError)
# we don't implement SignalError or descendants
test_ok(StandardError < Exception)
test_ok(ArgumentError < StandardError)
test_ok(IOError < StandardError)
test_ok(EOFError < IOError)
test_ok(IndexError < StandardError)
test_ok(LocalJumpError < StandardError)
test_ok(NameError < StandardError)
test_ok(NoMethodError < NameError)
test_ok(RangeError < StandardError)
test_ok(FloatDomainError < RangeError)
test_ok(RegexpError < StandardError)
test_ok(RuntimeError < StandardError)
test_ok(SecurityError < StandardError)
# we don't implement SystemCallError
test_ok(ThreadError < StandardError)
test_ok(TypeError < StandardError)
test_ok(ZeroDivisionError < StandardError)
test_ok(SystemExit < Exception)
test_ok(SystemStackError < Exception)