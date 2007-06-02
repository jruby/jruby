require 'test/unit'

class TestExceptions < Test::Unit::TestCase

  def testBasic
    begin
      raise "this must be handled"
      fail "Should have raised exception"
    rescue
      assert(true)
    end
  end

  def testBasicWithRetry
    again = true
    begin
      raise "this must be handled no.2"
    rescue
      if again
        again = false
        retry
        fail "should have retried"
      end
    end
    assert(!again)
  end

  def testExceptionInRescueClause
    string = "this must be handled no.3"
    begin
      begin
        raise "exception in rescue clause"
      rescue 
        raise string
      end
      fail "should have raised exception"
    rescue
      assert_equal(string, $!.message)
    end
  end

  
  def testExceptionInEnsureClause
    begin
      begin
        raise "this must be handled no.4"
      ensure 
        raise "exception in ensure clause"
      end
      fail "exception should have been raised"
    rescue
      assert(true)
    end
  end

  def testEnsureInNestedException
    bad = true
    begin
      begin
        raise "this must be handled no.5"
      ensure
        bad = false
      end
    rescue
    end
    assert(!bad)
  end


  def testEnsureTriggeredByBreak
    bad = true
    while true
      begin
        break
      ensure
        bad = false
      end
    end
    assert(!bad)
  end

end
