require 'test/minirunit'
test_check "Test Splat:"

class SplatSubscriptAssignment
  def []=(a,b)
    test_equal(1, a)
    test_equal(2, b)
  end
end

g = [1]
SplatSubscriptAssignment.new[*g] = 2