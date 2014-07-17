require "test/unit"

class TestIncludingModuleBustsConstantCaches < Test::Unit::TestCase
  module M
    A = 123
  end

  module N
    A = 456
  end

  class Foo
    include M

    def self.get
      A
    end
  end

  def test_including_module_busts_constant_caches
    assert_equal 123, Foo.get
    Foo.send(:include, N)
    assert_equal 456, Foo.get
  end
end
