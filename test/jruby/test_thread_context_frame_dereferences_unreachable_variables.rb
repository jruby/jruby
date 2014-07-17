require 'test/unit'
require 'weakref'
require 'java'

class TestThreadContextFrameDereferencesUnreachableVariables < Test::Unit::TestCase

  def test_dereference_unreachable_variable
    o = 'foo'
    o = WeakRef.new(o)
    poll do 
      java.lang.System.gc
      !o.weakref_alive?
    end
    assert !o.weakref_alive?, "object was not collected"
  end

  private

  def poll(seconds=1.0) 
    (seconds * 10).to_i.times do 
      return if yield
      sleep 0.1
    end
  end

end
