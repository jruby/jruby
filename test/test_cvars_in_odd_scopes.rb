require 'test/unit'

# JRUBY-3714: ActiveMessaging poller stops silently with JRuby 1.3.0RC1 and RC2.
class TestCvarsInOddScopes < Test::Unit::TestCase
  def test_lambda_in_eigenclass
    $result
    Class.new do
      @@running = true
      class << self
        $result = lambda do
          @@running
        end.call
      end
    end

    assert_equal true, $result
  end

  def test_proc_in_eigenclass
    $result
    Class.new do
      @@running = true
      class << self
        $result = Proc.new do
          @@running
        end.call
      end
    end

    assert_equal true, $result
  end

  def test_thread_in_eigenclass
    $result = false
    Class.new do
      @@running = true
      class << self
        $result = Thread.new do
          @@running
        end.value
      end
    end

    assert_equal true, $result
  end

  def test_thread_in_method_in_eigenclass
    result = Class.new do
      @@running = true
      class << self
        def go
          Thread.new do
            @@running
          end.value
        end
      end
    end.go

    assert_equal true, result
  end
end
