require 'test/unit'

# In rare cases where a singleton is constructed out of an immediate,
# the object can be GCed before the class is completely defined since
# 'attached' is now a weakref. This test causes that to happen in
# most runs, and should ensure we don't regress.
class TestSingletonWithTransient < Test::Unit::TestCase
  def test_transient
    assert_nothing_raised do
      class << 'foo'
        x = 0
        while x < 10
          GC.start
          def foo; end
          x += 1
        end
      end
    end
  end
end

