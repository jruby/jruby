require 'test/unit'

class TestScope < Test::Unit::TestCase

  # $_ is a local variable but not be shared by threads.
  def wakeup(t)
    t.run
  end
  private :wakeup

  def test_scope_uscore_string
    sub = proc {|x| "#{x, $_ = $_, x; x}"}
    assert_equal("", sub.call("first"))
    assert_equal("first", sub.call("second"))
  end

  def test_scope_match_string
    sub = proc {|x| "#{tmp = $&; /.*/ =~ x; tmp}"}
    assert_equal("", sub.call("first"))
    assert_equal("first", sub.call("second"))
  end

  def test_threadscope_uscore
    # shares local scope
    t = Thread.start do
      $_ = "sub"
      loop do
	Thread.stop
	assert_equal("sub", $_)
      end
    end

    begin
      $_ = "main"
      # another thread may change?
      t.run
      assert_equal("main", $_)
    ensure
      t.kill
      t.join
    end
  end

  def test_threadscope_match
    # shares local scope
    t = Thread.start do
      /.*/ =~ "sub"
      loop do
	Thread.stop
	assert_equal("sub", $&)
      end
    end

    begin
      /.*/ =~ "main"
      # another thread may change?
      t.run
      assert_equal("main", $&)
    ensure
      t.kill
      t.join
    end
  end

  def test_threadscope_uscore_sub
    t = Thread.start do
      $_ = "sub"
      loop do
        Thread.stop
        assert_equal("sub", $_)
      end
    end

    begin
      $_ = "main"
      # another thread waked in function may change?
      wakeup(t)
      assert_equal("main", $_)
    ensure
      t.kill
      t.join
    end
  end

  def test_threadscope_match_sub
    t = Thread.start do
      /.*/ =~ "sub"
      loop do
        Thread.stop
        assert_equal("sub", $&)
      end
    end

    begin
      /.*/ =~ "main"
      # another thread waked in function may change?
      wakeup(t)
      assert_equal("main", $&)
    ensure
      t.kill
      t.join
    end
  end

  def test_threadscope_uscore_main
    t = Thread.new(Thread.current) do |main|
      $_ = "sub"
      loop do
        assert_equal("sub", $_)
        Thread.stop
        wakeup(main)
      end
    end

    begin
      $_ = "main"
      # another thread waked in function may change while this thread
      # is absent this scope?
      wakeup(t)
      assert_equal("main", $_)
    ensure
      t.kill
      t.join
    end
  end

  def test_threadscope_match_main
    t = Thread.new(Thread.current) do |main|
      /.*/ =~ "sub"
      loop do
        assert_equal("sub", $&)
        Thread.stop
        wakeup(main)
      end
    end

    begin
      /.*/ =~ "main"
      wakeup(t)
      assert_equal("main", $&)
    ensure
      t.kill
      t.join
    end
  end

  def test_threadscope_uscore_proc
    sub = nil
    Thread.start do
      $_ = "sub"
      sub = proc {$_}
    end.join

    $_ = "main"
    assert_equal("main", $_)
    assert_equal("sub", sub.call)
    assert_equal("main", $_)
  end

  def test_threadscope_match_proc
    sub = nil
    Thread.start do
      /.*/ =~ "sub"
      sub = proc {$&}
    end.join

    /.*/ =~ "main"
    assert_equal("main", $&)
    assert_equal("sub", sub.call)
    assert_equal("main", $&)
  end

  
  def test_threadscope_local
    sub = nil
    Thread.start do
      var = "sub"
      sub = proc {var}
    end.join

    var = "main"
    assert_equal("main", var)
    assert_equal("sub", sub.call)
    assert_equal("main", var)
  end

  def test_threadscope_shared
    sub = nil
    var = "main"
    Thread.start do
      sub = proc {
	tmp, var = var, "sub"
	tmp
      }
    end.join

    assert_equal("main", sub.call)
    assert_equal("sub", var)
  end
end
