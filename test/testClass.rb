require 'minirunit'
test_check "Test Class derivation:"

class Top
  def Top.inherited(sub)
     $hierarchy << sub
  end
end

$hierarchy = [Top]
class Middle < Top
end

class Bottom < Middle
end

test_equal([Top, Middle, Bottom] , $hierarchy)


class AttrTest
  attr :attribute1
  attr_writer :attribute1
end
attrTest = AttrTest.new
attrTest.attribute1 = 1
test_equal(1 , attrTest.attribute1)
