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

  def test_case_consecutive
    params = [1,   2,  3,  5, 10, 11, 14, 15, 16, 17, 8, 9]
    expect = [10, 20, 30, 50,  0, 10, 40,  0, 10, 20, 8, 9]
    assert_equal expect, params.map { |p| case_12345(p) }
  end

  def case_12345(p)
    p = p % 5 if p >= 10
    case p
      when 1
        10
      when 2
        20
      when 3
        30
      when 4
        40
      when 5
        50
      else return p
    end
  end
  private :case_12345

  def test_case_with_holes
    params = [1,   2,   3,  5,  10, 11,  15, 16, 18,   8]
    expect = [10, nil, 30, 50, nil, 10, nil, 10, 30, nil]
    assert_equal expect, params.map { |p| case_135(p) }
  end

  def case_135(p)
    p = p % 5 if p >= 10
    case p
      when 1 then 10
      when 3 then 30
      when 5 then 50
    end
  end

  def test_case_24 # GH-4429
    args_21 = [1, 2]; args_22 = [2, 3]; args_23 = ['3', '4']
    args_41 = [1, 2, nil, 3]; args_42 = ['2', '3', '4', '5']
    expect = [2, 3, '4', 3, '5']
    assert_equal expect, [args_21, args_22, args_23, args_41, args_42].map { |a| case_24(*a) }
    assert_raise(ArgumentError) { case_24 }
    assert_raise(ArgumentError) { case_24(1) }
  end

  def case_24(*args)
    case s = args.size
      when 2 then args[1]
      when 4 then args[3]
      else raise ArgumentError.new("size: #{s} " + args.inspect)
    end
  end

  def test_big_case_with_holes
    params = [0, 1, 2, 4, 5, 10, 11, 18, 19, 20, 21, 22, -1,  -2]
    expect = [1, 0, 3, 5, 4,  9, 10, 17, 18, 21, 20, 21, nil, -3]
    assert_equal expect, params.map { |p| case_01359_21(p) }
  end

  def case_01359_21(p)
    case p + 1
      when 0 then nil
      when 1 then 1
      when 3 then 3
      when 5 then 5
      when 9 then 9
      when 21
        return 21
      else p - 1
    end
  end

  def test_multi_case_with_holes
    params = [  0, 1, 2, 3, 4, 5, 8, 9, 10, 12, 13, 14, 15, 16, 17]
    expect = [nil, 0, 1, 3, 1, 5, 1, 9, 10, 12, 13, 14, 15,  2, 17]
    assert_equal expect, params.map { |p| case_2481632(p) }
  end

  def case_2481632(p)
    case p % 100
      when 0 then nil
      when 1 then 0
      when 2, 4, 8 then 1
      when 16, 32 then 2
      else p % 31
    end
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
