$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestFloats < Rubicon::TestCase
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
end

# Run these tests if invoked directly

Rubicon::handleTests(TestFloats) if $0 == __FILE__




