require 'test/unit'

class TestThread < Test::Unit::TestCase

  def setup
    puts "******************" if Thread.critical
  end

  def thread_control
    @ready = false
    yield
    @ready = false
  end

  def _signal
    @ready = true
  end

  def _wait
    sleep 0.1 while !@ready
    @ready = false
  end

  class SubThread < Thread
    def initialize
      @wasCalled = true
      super
    end
    def initCalled?
      @wasCalled
    end
  end
  
  def teardown
    Thread.list.each do |t|
      if t != Thread.main
        t.kill
      end
    end
  end

  def test_AREF # '[]'
    t = Thread.current
    t2 = Thread.new { sleep 60 }

    t[:test] = "alpha"
    t2[:test] = "gamma"
    assert_equal(t[:test], "alpha")
    assert_equal(t2[:test], "gamma")
    t["test"] = "bravo"
    t2["test"] = "delta"
    assert_equal(t["test"], "bravo")
    assert_equal(t2["test"], "delta")
    assert(t[:none].nil?)
    assert(t["none"].nil?)
    assert(t2[:none].nil?)
    assert(t2["none"].nil?)
  end

  def test_ASET # '[]='
    t = Thread.current
    t2 = Thread.new { sleep 60 }

    t[:test] = "alpha"
    t2[:test] = "gamma"
    assert_equal(t[:test], "alpha")
    assert_equal(t2[:test], "gamma")
    t["test"] = "bravo"
    t2["test"] = "delta"
    assert_equal(t["test"], "bravo")
    assert_equal(t2["test"], "delta")
    assert(t[:none].nil?)
    assert(t["none"].nil?)
    assert(t2[:none].nil?)
    assert(t2["none"].nil?)
  end

  def test_abort_on_exception
    # Test default
    assert_equal(false, Thread.current.abort_on_exception)
    Thread.current.abort_on_exception = true
    assert_equal(true, Thread.current.abort_on_exception)
    Thread.current.abort_on_exception = false
    assert_equal(false, Thread.current.abort_on_exception)
  end

  class MyException < Exception; end

  def xtest_abort_on_exception=()
    save_stderr = nil
    begin
      begin
        t = Thread.new do
          raise MyException, "boom"
        end
        Thread.pass
        assert(true)
      rescue MyException
        fail("Thread exception propogated to main thread")
      end
p :here
      msg = nil
      begin
        t = Thread.new do
          Thread.current.abort_on_exception = true
          save_stderr = $stderr.dup
          $stderr.reopen(open("xyzzy.dat", "w"))
          raise MyException, "boom"
        end
        Thread.pass while t.alive?
        fail("Exception should have interrupted main thread")
      rescue SystemExit
        msg = open("xyzzy.dat") {|f| f.gets}
      ensure
        $stderr.reopen(save_stderr)
        File.unlink("xyzzy.dat")
      end
      assert_match(/\(TestThread::MyException\)$/, msg)
    rescue Exception
      fail($!.to_s)
    end
  end

  def test_alive?
    t1 = t2 = nil
    thread_control do
      t1 = Thread.new { _signal; Thread.stop }
      _wait
    end
    thread_control do
      t2 = Thread.new { _signal; sleep 60 }
      _wait
    end
    t3 = Thread.new {}
    t3.join
    assert_equal(true,Thread.current.alive?)
    assert_equal(true,t1.alive?)
    assert_equal(true,t2.alive?)
    assert_equal(false,t3.alive?)
  end

  def test_exit
    t = Thread.new { Thread.current.exit }
    t.join
    assert_equal(t,t.exit)
    assert_equal(false,t.alive?)
  end

  def test_join
    sum = 0
    t = Thread.new do
      5.times { sum += 1; sleep 0.1 }
    end
    assert(sum != 5)
    t.join
    assert_equal(5, sum)

    sum = 0
    t = Thread.new do
      5.times { sum += 1; sleep 0.1 }
    end
    t.join
    assert_equal(5, sum)

    # if you join a thread, it's exceptions become ours
    t = Thread.new do
      Thread.pass
      raise "boom"
    end

    begin
      t.join
    rescue Exception => e
      assert_equal("boom", e.message)
    end
  end

  def test_key?
    t = Thread.current
    t2 = Thread.new { sleep 60 }

    t[:test] = "alpha"
    t2[:test] = "gamma"
    assert_equal(true,t.key?(:test))
    assert_equal(true,t2.key?(:test))
    assert_equal(false,t.key?(:none))
    assert_equal(false,t2.key?(:none))
  end

  def test_kill
    t = Thread.new { Thread.current.kill }
    t.join
    assert_equal(t, t.kill)
    assert_equal(false, t.alive?)
  end

  def test_priority
    assert_equal(0, Thread.current.priority)
  end

  def test_priority=()
    c1 = 0
    c2 = 0
    my_priority = Thread.current.priority
    begin
      Thread.current.priority = 10
      a = Thread.new { Thread.stop; loop { c1 += 1 }}
      b = Thread.new { Thread.stop; loop { c2 += 1 }}
      a.priority = my_priority - 2
      b.priority = my_priority - 1
      1 until a.stop? and b.stop?
      a.wakeup
      b.wakeup
      sleep 1
      Thread.critical = true
      begin
	assert(c2 > c1)
	c1 = 0
	c2 = 0
	a.priority = my_priority - 1
	b.priority = my_priority - 2
	Thread.critical = false
	sleep 1 
	Thread.critical = true
	assert (c1 > c2)
	a.kill
	b.kill
      ensure
	Thread.critical = false
      end
    ensure
      Thread.current.priority = my_priority
    end
  end

  def test_raise
    madeit = false
    t = nil

    thread_control do
      t = Thread.new do
	_signal
	sleep 5
	madeit = true 
      end
      _wait
    end
    t.raise "Gotcha"
    assert(!t.alive?)
    assert_equal(false,madeit)
  end

  def test_run
    wokeup = false
    t1 = nil
    thread_control do
      t1 = Thread.new { _signal; Thread.stop; wokeup = true ; _signal}
      _wait
      assert_equal(false, wokeup)
      t1.run
      _wait
      assert_equal(true, wokeup)
    end

    wokeup = false
    thread_control do
      t1 = Thread.new { _signal; Thread.stop; _signal; wokeup = true }
      _wait

      assert_equal(false, wokeup)
      Thread.critical = true
      t1.run
      assert_equal(false, wokeup)
      Thread.critical = false
      t1.run
      _wait
      t1.join
      assert_equal(true, wokeup)
    end
  end

  def test_safe_level
    t = Thread.new do
      assert_equal(0, Thread.current.safe_level)
      $SAFE=1
      assert_equal(1, Thread.current.safe_level)
      $SAFE=2
      assert_equal(2, Thread.current.safe_level)
      $SAFE=3
      assert_equal(3, Thread.current.safe_level)
      $SAFE=4
      assert_equal(4, Thread.current.safe_level)
      Thread.pass
    end
    t.join rescue nil
    assert_equal(0, Thread.current.safe_level)
    assert_equal(4, t.safe_level)
  end

  def test_status
    a = b = c = nil

    thread_control do
      a = Thread.new { _signal; raise "dead" }
      _wait
    end
    
    thread_control do
      b = Thread.new { _signal; Thread.stop }
      _wait
    end

    thread_control do
      c = Thread.new { _signal;  }
      _wait
    end

    assert_equal("run",   Thread.current.status)
    assert_equal(nil,     a.status)
    assert_equal("sleep", b.status)
    assert_equal(false,   c.status)
  end

  def test_stop?
    a = nil
    thread_control do
      a = Thread.new { _signal; Thread.stop }
      _wait
    end
    assert_equal(true, a.stop?)
    assert_equal(false, Thread.current.stop?)
  end

  def test_value
    t=[]
    10.times { |i|
      t[i] = Thread.new { i }
    }
    result = 0
    10.times { |i|
      result += t[i].value
    }
    assert_equal(45, result)
  end

  def test_wakeup
    madeit = false
    t = Thread.new { Thread.stop; madeit = true }
    assert_equal(false, madeit)
    Thread.pass while t.status != "sleep"
    t.wakeup
    assert_equal(false, madeit) # Hasn't run it yet
    t.run
    t.join
    assert_equal(true, madeit)
  end

  def test_s_abort_on_exception
    assert_equal(false,Thread.abort_on_exception)
    Thread.abort_on_exception = true
    assert_equal(true,Thread.abort_on_exception)
    Thread.abort_on_exception = false
    assert_equal(false,Thread.abort_on_exception)
  end

  def xtest_s_abort_on_exception=
    save_stderr = nil

    begin
      Thread.new do
	raise "boom"
      end
      Thread.pass
      assert(true)
    rescue Exception
      fail("Thread exception propagated to main thread")
    end

    msg = nil
    begin
      Thread.abort_on_exception = true
      t = Thread.new do
	save_stderr = $stderr.dup
	$stderr.reopen(open("xyzzy.dat", "w"))
	raise MyException, "boom"
      end
      Thread.pass while t.alive?
      fail("Exception should have interrupted main thread")
    rescue SystemExit
      msg = open("xyzzy.dat") {|f| f.gets}
    ensure
      Thread.abort_on_exception = false
      $stderr.reopen(save_stderr)
      File.unlink("xyzzy.dat")
    end
    assert_match(/\(TestThread::MyException\)$/, msg)
  end

  def test_s_critical
    assert_equal(false,Thread.critical)
    Thread.critical = true
    assert_equal(true,Thread.critical)
    Thread.critical = false
    assert_equal(false,Thread.critical)
  end

  def test_s_critical=
    count = 0
    a = nil
    thread_control do
      a = Thread.new { _signal; loop { count += 1; Thread.pass }}
      _wait
    end
begin
    Thread.critical = true
    saved = count # Fixnum, will copy the value
    10000.times { |i| Math.sin(i) ** Math.tan(i/2) }
    assert_equal(saved, count)

    Thread.critical = false
    10000.times { |i| Math.sin(i) ** Math.tan(i/2) }
    assert(saved != count)
ensure
  Thread.critical = false
end
  end

  def test_s_current
    t = nil
    thread_control do
      t = Thread.new { _signal; Thread.stop }
      _wait
    end
    assert(Thread.current != t)
  end

  def test_s_exit
    t = Thread.new { Thread.exit }
    t.join
    assert_equal(t, t.exit)
    assert_equal(false, t.alive?)
    IO.popen("#$interpreter -e 'Thread.exit; puts 123'") do |p|
      assert_nil(p.gets)
    end
    assert_equal(0, $?)
  end

  def test_s_fork
    madeit = false
    t = Thread.fork { madeit = true }
    t.join
    assert_equal(true,madeit)
  end

  def test_s_kill
    count = 0
    t = Thread.new { loop { Thread.pass; count += 1 }}
    sleep 0.1
    Thread.critical = true
    assert_equal("run", t.status)
    saved = count
    Thread.kill(t)
    assert_equal("aborting", t.status)
    Thread.critical = false;
    sleep 0.1
    t.join
    assert_equal(saved, count)
  end

  def test_s_list
    t = []
    100.times { t << Thread.new { Thread.stop } }
    assert_equal(101, Thread.list.length)
    t.each { |i| Thread.pass while !i.stop?; i.run; i.join }
    assert_equal(1, Thread.list.length)
  end

  def test_s_main
    t = nil
    thread_control do
      t = Thread.new { _signal; Thread.stop }
      _wait
    end
    assert_equal(Thread.main, Thread.current)
    assert(Thread.main != t)
  end

  def test_s_new
    madeit = false
    t = Thread.new { madeit = true }
    t.join
    assert_equal(true,madeit)
  end

  def test_s_pass
    madeit = false
    t = Thread.new { Thread.pass; madeit = true }
    t.join
    assert_equal(true, madeit)
  end

  def test_s_start
    t = nil
    thread_control do
      t = SubThread.new { _signal; Thread.stop }
      _wait
    end
    assert_equal(true, t.initCalled?)

    thread_control do
      t = SubThread.start { _signal; Thread.stop }
      _wait
    end
    assert_equal(nil, t.initCalled?)
  end

  def test_s_stop
    t = nil
    thread_control do
      t = Thread.new { Thread.critical = true; _signal; Thread.stop }
      _wait
    end
    assert_equal(false,   Thread.critical)
    assert_equal("sleep", t.status)
  end

  if Thread.instance_method(:join).arity != 0
    def test_timeout
      start = Time.now
      t = Thread.new do
	sleep 3
      end
      timeout = proc do |i|
	s = Time.now
	assert_nil(t.join(i))
	e = Time.now
	assert_equal(true, t.alive?)
	e - s
      end
      assert(timeout[0] < 0.1)
      i = timeout[1]
      assert(0.5 < i && i < 1.5)
      i = timeout[0.5]
      assert(0.4 < i && i < 0.6)
      assert_equal(t, t.join(nil))
      i = Time.now - start
      assert(2.5 < i && i < 3.5)
    ensure
      t.kill
    end
  end

end
