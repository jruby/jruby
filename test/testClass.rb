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


class Froboz
  include Enumerable
end
f = Froboz.new
test_equal([Froboz, Enumerable, Object, Kernel], f.type.ancestors)
test_ok(f.kind_of?(Froboz))
test_ok(f.kind_of?(Enumerable))

class CM1
  def a
    A
  end
end

class CM2
  def a
    A
  end

  def CM2::constant_missing
    "A"
  end
end

test_exception(NameError) {CM1.new.a}
test_equal(CM2.new.a, "A")
