require 'test/unit'

# JRUBY-2573: thread-local return jump was actually JVM-local (final static)
class TestThreadedNonlocalReturn < Test::Unit::TestCase
  def foo(x)
    bar { return x }
  end

  def bar
    yield
  end

  def test_threaded_nonlocal_return
    t = []
    aggs = [[],[],[],[],[],[],[],[],[],[]]
    10.times {|i| t << Thread.new(i) {|j| Thread.stop; 100.times { aggs[j] << foo(j) } } }
    # make sure they're all asleep, wake them all up, join them all in sequence
    t.each {|th| Thread.pass until th.status == "sleep"}
    t.each {|th| th.wakeup}
    t.each {|th| th.join}
    
    expected = []
    10.times {|i| expected << ([i] * 100)}
    assert_equal(expected, aggs)
  end
end
