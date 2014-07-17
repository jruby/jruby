require 'test/unit'
require 'jruby'

class TestThreadService < Test::Unit::TestCase
  GC_COUNT = 10
  
  def setup
    @service = JRuby.runtime.thread_service
  end
  
  def wait_for_list_size(start_rt)
    start_time = Time.now
    until @service.ruby_thread_map.size == start_rt || (Time.now - start_time > 30)
      JRuby.gc
    end
  end
  
  def test_ruby_thread_leaks
    start_rt = @service.ruby_thread_map.size
    
    # spin up 100 threads and join them
    (1..10).to_a.map {Thread.new {}}.map(&:join)
    
    # access map and GC repeatedly for a while to flush things out
    wait_for_list_size(start_rt)
    
    # confirm the size goes back to the same
    assert_equal start_rt, @service.ruby_thread_map.size
  end

  def join_java_threads
    # spin up Java threads and join them
    #
    # In the IR interpreter, placing this loop in a separate method ensures
    # that there is no live ref to the thread-array in the interpreter
    # tmp var array.  This can actually happen in some scenarios.  This is
    # not quite a correctness issue, but more a problem with the expectation
    # about when GC will run and what it can collect.
    (1..10).to_a.map {t = java.lang.Thread.new {}; t.start; t}.map(&:join)
  end
  
  def test_java_thread_leaks
    start_rt = @service.ruby_thread_map.size

    # spin up 100 Java threads and join them
    join_java_threads
    
    # access map and GC repeatedly for a while to flush things out
    wait_for_list_size(start_rt)

    # confirm the size goes back to the same
    assert_equal start_rt, @service.ruby_thread_map.size
  end
  
  def test_java_threads_in_thread_list
    svc = JRuby.runtime.thread_service
    start_list = Thread.list
    start_rt = @service.ruby_thread_map.size
    
    # spin up 100 Java threads and wait for them all to be ready
    state_ary = [false] * 10
    threads = (0..9).to_a.map do |i|
      t = java.lang.Thread.new do
        state_ary[i] = true
        Thread.pass while state_ary[i]
      end
      t.start
      t
    end
    
    # wait for them all to be running
    Thread.pass until state_ary.all?
    
    # check that Thread.list contains 100 more threads
    assert_equal start_list.size + 10, Thread.list.size
    
    # shut down all threads and wait for them to terminate
    0.upto(9) {|i| state_ary[i] = false}
    threads.map(&:join)
    threads = nil
    
    # access map and GC repeatedly for a while to flush things out
    wait_for_list_size(start_rt)
    
    # confirm the thread list is back to what it was
    assert_equal start_list, Thread.list
  end
end
