require 'test/unit'
require 'java'

class TestMe; end

class TestCacheMapLeak < Test::Unit::TestCase

  def setup
    @num = 100
    @num.times { class << TestMe.new; def foo; end; end }
    
    sleep 1
    java.lang.System.gc
  end
  
  def test_objects_are_released_by_cache_map
    assert(@num != ObjectSpace.each_object(TestMe){}, "Objects not being release by CacheMap" )
  end
  
end
