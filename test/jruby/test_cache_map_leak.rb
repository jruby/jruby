require 'test/unit'

class TestCacheMapLeak < Test::Unit::TestCase

  class TestMe; end

  def setup
    require 'jruby'
  end

  def setup_test
    @num = 100
    @num.times { class << TestMe.new; def foo; end; end }
    
    t = Time.now
    (JRuby.gc; sleep 0.1) until (ObjectSpace.each_object(TestMe){} < @num || (Time.now - t > 5))
  end
  
  def test_objects_are_released_by_cache_map
    return unless JRuby.runtime.object_space_enabled?
    setup_test
    assert(@num != ObjectSpace.each_object(TestMe){}, "Objects not being release by CacheMap" )
  end
  
end
