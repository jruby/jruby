require 'test/unit'
require 'java'

class TestMe; end

class TestCacheMapLeak < Test::Unit::TestCase

  def setup
    @num = 100
    @num.times { class << TestMe.new; def foo; end; end }
    
    t = Time.now
    (JRuby.gc; sleep 0.1) until (ObjectSpace.each_object(TestMe){} < @num || (Time.now - t > 5))
  end
  
  def test_objects_are_released_by_cache_map
    assert(@num != ObjectSpace.each_object(TestMe){}, "Objects not being release by CacheMap" )
  end
  
end
