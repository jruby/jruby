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
test_ok([Top, Middle, Bottom] == $hierarchy)
