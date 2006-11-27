require 'test/minirunit'

test_check "Test NoMethodError:"
begin
  raise NoMethodError
rescue => e
  test_equal(NoMethodError, e.class)
  test_equal(NameError, e.class.superclass)
end
