require 'test/unit'

class TestCase < Test::Unit::TestCase
  def test_case_with_no_expression
    x = nil
    case
    when true
      x = 1
    when false
      x = 2
    end
    assert_equal(1, x)

    x = nil
    case
    when false
      x = 1
    when true
      x = 2
    end
    assert_equal(2, x)
  end

  def test_case_with_ranges
    x = nil
    case 10
    when 1..3
      x = 'a'
    when 4..8
      x = 'b'
    when 9..22
      x = 'c'
    else
      x = 'd'
    end
    assert_equal('c', x)
  end

  def test_case_with_else
    x = nil
    case 10
    when 1
      x = 'a'
    when 100
      x = 'b'
    else
      x = 'c'
    end
    assert_equal('c', x)
  end

  def test_case_no_match_returns_nil
    x = case nil
    when String then "HEH1"
    end
    assert_equal(nil, x)

    x = case "FOO"
    when Proc then "HEH1"
    end
    assert_equal(nil, x)
  end

  def test_case_return_value
    x = case "HEH"
    when Proc then "BAD"
    else "GOOD"
    end
    assert_equal("GOOD", x)
  end
  
  def test_case_when_splats_single
    assert_nothing_raised {
      case 1
      when *1
      end
    }
  end
end
