require 'test/unit'

class TestPredefinedVariables < Test::Unit::TestCase

  # this is the test from test.rb, but we really need to be
  # more compregensive here
  def testVariables
    assert_instance_of(Fixnum, $$)
    assert_raise(NameError) { $$ = 1 }

    foobar = "foobar"
    $_ = foobar
    assert_equal(foobar, $_)
  end

end