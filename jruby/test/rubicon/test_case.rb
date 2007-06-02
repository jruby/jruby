require 'test/unit'

class TestCase < Test::Unit::TestCase

  def testBasic
    case 5
    when 1, 2, 3, 4, 6, 7, 8
      fail("case 5 missed")
    when 5
      assert(true)
    end

    case 5
    when 5
      assert(true)
    when 1..10
      fail("case 5 missed")
    end

    case 5
    when 1..10
      assert(true)
    else
      fail("else taken in error")
    end
    
    case 5
    when 5
      assert(true)
    else
      fail("case 5 didn't match 5")
    end

    case "foobar"
    when /^f.*r$/
      assert(true)
    else
      fail("Regexp didn't match in case")
    end
  end

end
