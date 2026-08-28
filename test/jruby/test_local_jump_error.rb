require 'test/unit'

class TestLocalJumpError < Test::Unit::TestCase
  def test_lje_structure
    begin
      break 1
    rescue LocalJumpError => lje
      assert_equal(:break, lje.reason)
      assert_equal(1, lje.exit_value)
      assert_equal(["@reason", "@exit_value"], lje.instance_variables)
    end
    
    begin
      yield 1
    rescue LocalJumpError => lje
      assert_equal(:noreason, lje.reason)
      assert_equal(nil, lje.exit_value)
    end
    
=begin This seems like it should work, but it doesn't, even in Ruby...
    begin
      next
    rescue LocalJumpError => lje
      assert_equal(:next, lje.reason)
      assert_equal(nil, lje.exit_value)
    end
=end
  end
end