require 'test/minirunit'
test_check "Test Object"


class Dummy
  attr_reader :var
  
  def initialize
    @var = 99
  end
  
  def remove
    remove_instance_variable(:@var)
  end
  
  def remove_bad
    remove_instance_variable(:@foo)
  end
end

d = Dummy.new
test_equal(99, d.var)
test_equal(99, d.remove)
test_equal(nil, d.var)
test_exception(NameError) { d.remove_bad }