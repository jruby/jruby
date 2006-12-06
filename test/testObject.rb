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



#test singleton_methods
module Other
  def three() end
end

class Single
  def Single.four() end
end

a = Single.new

def a.one() end

class << a
  include Other
  def two() end
end

test_equal(%w(four),Single.singleton_methods)

a1 = a.singleton_methods(false)
a2 = a.singleton_methods(true)
a3 = a.singleton_methods

test_equal(2,a1.length)
test_ok(a1.include?('one'))
test_ok(a1.include?('two'))

test_equal(3,a2.length)
test_ok(a2.include?('one'))
test_ok(a2.include?('two'))
test_ok(a2.include?('three'))

test_equal(3,a3.length)
test_ok(a3.include?('one'))
test_ok(a3.include?('two'))
test_ok(a3.include?('three'))
