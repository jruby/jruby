require 'test/unit'
require 'jruby'

class TestThreadService < Test::Unit::TestCase
  GC_COUNT = 10
  
  def test_ruby_thread_leaks
    svc = JRuby.runtime.thread_service
    start_rt = svc.ruby_thread_map.size
    
    # spin up 100 threads and join them
    (1..10).to_a.map {Thread.new {}}.map(&:join)
    
    # access maps and GC a couple times to flush things out
    svc.ruby_thread_map.size
    GC_COUNT.times {JRuby.gc}
    
    # confirm the size goes back to the same
    assert_equal start_rt, svc.ruby_thread_map.size
  end
  
  def test_java_thread_leaks
    svc = JRuby.runtime.thread_service
    start_rt = svc.ruby_thread_map.size

    # spin up 100 Java threads and join them
    (1..10).to_a.map {t = java.lang.Thread.new {}; t.start; t}.map(&:join)
    
    # access maps and GC a couple times to flush things out
    svc.ruby_thread_map.size
    GC_COUNT.times {JRuby.gc}

    # confirm the size goes back to the same
    assert_equal start_rt, svc.ruby_thread_map.size
  end
  
  def test_java_threads_in_thread_list
    svc = JRuby.runtime.thread_service
    start_list = Thread.list
    
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
    
    # access maps and GC a couple times to flush things out
    svc.ruby_thread_map.size
    GC_COUNT.times {JRuby.gc}
    
    # confirm the thread list is back to what it was
    assert_equal start_list, Thread.list
  end
end