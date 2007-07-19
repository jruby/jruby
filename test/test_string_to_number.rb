require 'test/unit'

# Tests for string-to-number conversions (primarily those found
# in org.jruby.util.Convert.java).
# 
# TODO Consolidate/merge with tests found in rubicon/test_floats.rb.
# 
class TestStringToNumber < Test::Unit::TestCase

  # Test fix for JRUBY-1215  
  def test_invalid_float_from_string
    for string in [ 
      "1a",
      "a1",
      "1.0a",
      "10a",
      "10.1a",
      "0.10a",
      "1.1e1a",
      "1.1e10a",
      "\3 1",
      "1 \3",
    ]
      assert_raises(ArgumentError) { Float(string) }
    end
  end
  
  # Test fix for JRUBY-1215-related (unreported) bug
  def test_invalid_integer_from_string
    for string in [ 
      "a1",
      "1a",
      "10a",
      "\3 1",
      "1 \3",
    ]
      assert_raises(ArgumentError) { Integer(string) }
    end
  end
  
end