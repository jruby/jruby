require 'test/minirunit'
test_check "Test Constant Scoping:"

module A
  module B
    class C
      def foo
	"ABC"
      end
    end
  end
end

module B
  class C
    def foo
      "BC"
    end
  end
end

test_equal("ABC", A::B::C.new.foo)

module Foo
  class ObjectSpace
  end

  test_equal('ObjectSpace', ::ObjectSpace.name)
end

class Gamma
end

module Bar
        class Gamma
        end

	test_equal('Gamma', ::Gamma.name)
end

FOO = "foo"

test_equal(::FOO, "foo")

module X
   def X.const_missing(name)
     "missing"
   end
 end

test_equal(X::String, "missing")

module Y
  String = 1
end

test_equal(Y::String, 1)

module Z1
  ZOOM = "zoom"
  module Z2
    module Z3
      test_equal(ZOOM, "zoom")
      test_equal(Z1::ZOOM, "zoom")
    end
  end
end
