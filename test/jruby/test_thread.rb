require 'test/unit'
require 'thread'

class TestThread < Test::Unit::TestCase
  def test_running_and_finishing
    thread = Thread.new {
      $toto = 1
    }
    thread.join
    assert_equal(1, $toto)
    assert_equal(false, thread.status)
  end

  def test_local_variables
    v = nil
    t = Thread.new { v = 1 }
    t.join
    assert_equal(1, v)
  end

  def test_taking_arguments
    v = nil
    t = Thread.new(10) {|argument| v = argument }
    t.join
    assert_equal(10, v)
  end

  def test_thread_current
    t = Thread.current
    assert_equal(t, Thread.current)
  end

  def test_thread_local_variables
    v = nil
    t = Thread.new {
      Thread.current[:x] = 1234
      assert_equal(1234, Thread.current[:x])
      assert_equal(nil, Thread.current[:y])
      assert(Thread.current.key?(:x))
      assert(! Thread.current.key?(:y))
    }
    t.join
    assert(! Thread.current.key?(:x))
    Thread.current[:x] = 1
    assert(Thread.current.key?(:x))
    Thread.current["y"] = 2
    assert(Thread.current.key?("y"))
    assert_equal([:x, :y], Thread.current.keys.sort {|x, y| x.to_s <=> y.to_s} & [:x, :y])
    assert_raises(TypeError) { Thread.current[Object.new] }
    assert_raises(TypeError) { Thread.current[Object.new] = 1 }
    assert_raises(TypeError) { Thread.current[1] }
    assert_raises(TypeError) { Thread.current[1]  = 1}
  end

  def test_status
    t = Thread.new { Thread.current.status }
    t.join
    v = t.value
    assert_equal("run", v)
    assert_equal(false, t.status)

    # check that "run", sleep", and "dead" appear in inspected output
    q = Queue.new
    ready = false
    t = Thread.new { q << Thread.current.inspect; ready = true; sleep }
    Thread.pass until ready && (t.status == "sleep" || !t.alive?)
    assert(q.shift(true)["run"])
    assert(t.inspect["sleep"])
    t.kill
    t.join rescue nil
    assert(t.inspect["dead"])
  end

  def thread_foo()
    raise "hello"
  end
  def test_error_handling
    e = nil
    t = Thread.new {
      thread_foo()
    }
    begin
      t.join
    rescue RuntimeError => error
      e = error
    end
    assert(! e.nil?)
    assert_equal(nil, t.status)
  end

  def test_joining_itself
    e = nil
    begin
      Thread.current.join
    rescue ThreadError => error
      e = error
    end
    assert(! e.nil?)
    assert_match(/thread [0-9a-z]+ tried to join itself/, e.message)
  end

  def test_raise
    e = nil
    t = Thread.new {
      while true
        Thread.pass
      end
    }
    t.raise("Die")
    begin
      t.join
    rescue RuntimeError => error
      e = error
    end
    assert(e.kind_of?(RuntimeError))

    # test raising in a sleeping thread
    e = 1
    set = false
    begin
      t = Thread.new { e = 2; set = true; sleep(100); e = 3 }
      while !set
        sleep(1)
      end
      t.raise("Die")
    rescue; end

    assert_equal(2, e)
    assert_raise(RuntimeError) { t.value }
  end

  def test_thread_value
    assert_raise(ArgumentError) { Thread.new { }.value(100) }
    assert_equal(2, Thread.new { 2 }.value)
    assert_raise(RuntimeError) { Thread.new { raise "foo" }.value }
  end

  class MyThread < Thread
    def initialize
      super do; 1; end
    end
  end

  def test_thread_subclass_zsuper
    x = MyThread.new
    x.join
    assert_equal(1, x.value)
    x = MyThread.start { 2 }
    x.join
    assert_equal(2, x.value)
  end

  # Because a Ruby thread may use a pooled thread, we will
  # not preserve priorities set into dead threads. Because
  # this is a meaningless feature, anyway, I remove it here
  # and consider this behavior undefined. CON@20120306

  # def test_dead_thread_priority
  #   x = Thread.new {}
  #   1 while x.alive?
  #   x.priority = 5
  #   assert_equal(5, x.priority)
  # end

  def test_join_returns_thread
    x = Thread.new {}
    assert_nothing_raised { x.join.to_s }
  end

  def test_abort_on_exception_does_not_blow_up
    # CON: I had an issue where annotated methods weren't binding right
    # where there was both a static and instance method of the same name.
    # This caused abort_on_exception to fail to bind right; a temporary fix
    # was put in place by appending _x but it shouldn't stay. This test confirms
    # the method stays callable.
    assert_nothing_raised { Thread.abort_on_exception }
    assert_nothing_raised { Thread.abort_on_exception = Thread.abort_on_exception}
  end

  # JRUBY-2021
  def test_multithreaded_method_definition
    def run_me
      sleep 0.1
      def do_stuff
        sleep 0.1
      end
    end

    threads = []
    100.times {
      threads << Thread.new { run_me }
    }
    threads.each { |t| t.join }
  end

  def test_socket_accept_can_be_interrupted
    require 'socket'
    tcps = nil
    100.times{|i|
      begin
        tcps = TCPServer.new("0.0.0.0", 10000+i)
        break
      rescue Errno::EADDRINUSE
        next
      end
    }

    flunk "unable to find open port" unless tcps

    t = Thread.new {
      tcps.accept
    }

    Thread.pass until t.status == "sleep"
    ex = Exception.new
    t.raise ex
    assert_raises(Exception) { t.join }
  end

  # JRUBY-2315
  def test_exit_from_within_thread
    begin
      a = Thread.new do
        loop do
          sleep 0.1
        end
      end

      b = Thread.new do
        sleep 0.5
        Kernel.exit(1)
      end

      a.join
      fail
      b.join
    rescue SystemExit
      # rescued!
      assert(true)
    ensure
      a.kill rescue nil
      b.kill rescue nil
    end
  end

  def call_to_s(a)
    a.to_s
  end

  # JRUBY-2477 - polymorphic calls are not thread-safe
  def test_poly_calls_thread_safe
    # Note this isn't a perfect test, but it's not possible to test perfectly
    # This might only fail on multicore machines
    results = [false] * 20
    threads = []
    sym = :foo
    str = "foo"

    20.times {|i| threads << Thread.new { 10_000.times { call_to_s(sym); call_to_s(str) }; results[i] = true }}

    threads.pop.join until threads.empty?
    assert_equal [true] * 20, results
  end

  def test_thread_exit_does_not_deadlock
    100.times do
      t = Thread.new { Thread.stop; Thread.current.exit }
      Thread.pass until t.status == "sleep"
      t.wakeup; t.join
    end
  end

  # JRUBY-2380: Thread.list has a race condition
  # Fix is to make sure the thread is added to the global list before returning from Thread#new
  def test_new_thread_in_list
    1000.times do
      t = Thread.new do
        sleep
      end
      fail("new thread was not in Thread.list") unless Thread.list.include? t
      Thread.pass until t.status == 'sleep'
      t.wakeup
      t.join
    end
  end

  # JRUBY-3568: thread group is inherited from parent
  def test_inherits_thread_group
    tg = ThreadGroup.new
    og = nil
    tg.add(Thread.current)
    Thread.new { og = Thread.current.group }.join
    assert_equal(tg, og)
  end

  # JRUBY-3740: Thread#wakeup not working
  def test_wakeup_wakes_sleeping_thread
    awoke = false
    t = Thread.new { sleep; awoke = true }
    Thread.pass until t.status == "sleep"
    t.wakeup.join
    assert awoke

    awoke = false
    start_time = Time.now
    done = false
    t = Thread.new { sleep 100; done = true }
    Thread.pass until t.status == "sleep"
    t.wakeup
    loop {
      break if done || Time.now - start_time > 10
      Thread.pass
    }
    assert done
  end

  # JRUBY-5290
  def test_default_priority
    t = Thread.new { sleep 1 while true }
    assert_equal 0, t.priority
    t.exit
  end

  # Simpler case for sleep/wakeup close together, which can race if thread state is not managed well
  def test_sleep_wakeup_interlacing
    go = false
    ret = []
    t = Thread.new do
      10000.times do
        Thread.pass until go
        sleep
        ret << 'ok'
      end
    end
    10000.times do
      go = true
      Thread.pass until t.status == 'sleep'
      go = false
      t.wakeup
    end
    t.join
    assert_equal(10000, ret.size)
  end

  def test_inspect_and_to_s
    t = Thread.new {}.join
    assert_match(/#<Thread:0x[0-9a-z]+>/, t.to_s)
    # TODO we do not have file/line right :
    # MRI: #<Thread:0x000000014b0e28@test/jruby/test_thread.rb:346 dead>
    #assert_match(/#<Thread:0x[0-9a-z]+@test\/jruby\/test_thread\.rb\:346 \w+>/, t.inspect)
    assert_match(/#<Thread:0x[0-9a-z]+(@.*\.rb\:\d+)? \w+>/, t.inspect)

    assert_nil t.name

    t = Thread.new {}.join
    t.name = 'universal'
    assert_match(/#<Thread:0x[0-9a-z]+>/, t.to_s)
    assert_match(/#<Thread:0x[0-9a-z]+@universal(@.*\.rb\:\d+)? \w+>/, t.inspect)
  end

  def test_thread_name
    Thread.new do
      assert_match(/\#\<Thread\:0x\h+(@[\w\/\.\-_]+\:\d+)?\srun\>/, Thread.current.inspect)
      # TODO? currently in JIT file comes as "" and line as 0
      assert_match(/Ruby\-\d+\-Thread\-\d+\:\s(.*\.rb)?\:\d+/, native_thread_name(Thread.current)) if defined? JRUBY_VERSION
    end.join

    Thread.new do
      Thread.current.name = 'foo'
      assert_match(/\#\<Thread\:0x\h+@foo(@[\w\/\.\-_]+\:\d+)?\srun\>/, Thread.current.inspect)
      assert_match(/Ruby\-\d+\-Thread\-\d+\@foo:\s(.*\.rb)?\:\d+/, native_thread_name(Thread.current)) if defined? JRUBY_VERSION

      Thread.current.name = 'bar'
      assert_match(/\#\<Thread\:0x\h+@bar(@[\w\/\.\-_]+\:\d+)?\srun\>/, Thread.current.inspect)
      assert_match(/Ruby\-\d+\-Thread\-\d+\@bar:\s(.*\.rb)?\:\d+/, native_thread_name(Thread.current)) if defined? JRUBY_VERSION

      Thread.current.name = nil
      assert_match(/\#\<Thread\:0x\h+(@[\w\/\.\-_]+\:\d+)?\srun\>/, Thread.current.inspect)
      assert_match(/Ruby\-\d+\-Thread\-\d+\:\s(.*\.rb)?\:\d+/, native_thread_name(Thread.current)) if defined? JRUBY_VERSION
    end.join


    Thread.new do
      Thread.current.to_java.native_thread.name = 'user-set-native-thread-name'
      Thread.current.name = 'foo'

      assert Thread.current.inspect.index('@foo')
      assert_equal 'user-set-native-thread-name', native_thread_name(Thread.current) if defined? JRUBY_VERSION

      Thread.current.name = nil
      assert ! Thread.current.inspect.index('@foo')
      assert_equal 'user-set-native-thread-name', native_thread_name(Thread.current) if defined? JRUBY_VERSION
    end.join
  end

  private

  def native_thread_name(thread)
    thread.to_java.native_thread.name
  end

end
