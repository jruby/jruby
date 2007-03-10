require 'test/unit'

class TestFloats < Test::Unit::TestCase

  #
  # Check a float for approximate equality
  #
  # FIXME: nuke this or move it to a common location.
  def assert_flequal(exp, actual, msg=nil)
    assert_in_delta(exp, actual, exp == 0.0 ? 1e-7 : exp.abs/1e7, msg)
  end

  def test_number_to_string
    tests = [
      [ 1.0,             "1.0"],
      [-3.141592654,    "-3.141592654"],
      [ 0.1,             "0.1"],
      [ 2.1e0,           "2.1"],
      [ 2.1e3,        "2100.0"],
      [-2.1E0,          "-2.1"],
      [-2.1E-3,         "-0.0021"],
    ]
    for number, string in tests
      assert_equal(string, number.to_s, "case #{number} to \"#{string}\"")
    end

    # For the big numbers... 
    assert_flequal(-2.1E10, Float(-2.1E10.to_s), "case #{number} to \"#{string}\"")
  end

  def test_malformed
    for malformed in [
	".e2",
	"1.",
	"1. 1",
	"1 .1",
	"2.1e2.1",
	"2.1e0x3",
	"2 .1e2",
	"2. 1e2",
	"2.1 e2",
	"2.1e 2",
	"2.1e",
	"2.1E",
	"2.1_e",
	"2.1_E",
	"2.1e-",
	"2.1E-",
	"2.1e+",
	"2.1E+",
      ]
      assert_raise(SyntaxError, "case #{malformed}") do 
	eval malformed
      end
    end
  end

  def test_string_to_number
    for string, number in [
	[ "1.0",          1.0,],
	["-3.141592654", -3.141592654],
	["  .1",          0.1],
	[" 2.1e+0",       2.1],
	[" 2.1e3",     2100.0],
	["-2.1E+0",      -2.1],
	["-2.1E-3",      -0.0021],
	["-2.1E10",      -2.1e+10],
      ]
      assert_equal(string.to_f, number, "case \"#{string}\" to #{number}")
    end
  end
  # MRI compatibility
  def test_whitespace_string_to_number
    for string, number in [
	[ "1_._0_",                        1.0,],
	["  -3_._141__592__654  ", -3.141592654],
	["  _._1 ",                          0.1],
	["\n\r\t _2_._1_e_+_0",             2.1],
	[" _2_._1_e_3",                  2100.0],
      ]
      assert_equal(number, string.to_f, "case \"#{string}\" to #{number}")
    end
  end
  # MRI compatibility
  def test_float_from_string
    for string, number in [
	[ "1.0",          1.0,],
	["-3.141592654", -3.141592654],
	["  .1",          0.1],
	[" 2.1e+0",       2.1],
	[" 2.1e3",     2100.0],
	["-2.1E+0",      -2.1],
	["-2.1E-3",      -0.0021],
	["-2.1E10",      -2.1e+10],
	["  -3.1_4_1_592_654  ", -3.141592654],
	["\n\r\t 2.1e+0_1",             21.0],
      ]
      assert_equal(number, Float(string), "case \"#{string}\" to #{number}")
    end
  end
end




