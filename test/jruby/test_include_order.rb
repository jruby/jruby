#GH-1938
require "test/unit"

class TestIncludeOrder < Test::Unit::TestCase
  module X
  end

  class Q
    def foo arg = []
      arg << :Q
    end
  end

  class Y < Q
    include X
  end

  module A
    def foo arg = []
      arg << :A
      super
    end
  end

  module X
    include A
  end

  module Z
    def foo arg = []
      arg << :Z
      super
    end
  end

  class Y
    include Z
  end

  class Y
    include X
  end

  def test_include_order
    assert_equal Y.new.foo, [:Z, :A, :Q]
  end
end
