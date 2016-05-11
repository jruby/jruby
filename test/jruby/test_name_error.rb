require 'test/unit'

class TestNameError < Test::Unit::TestCase
  def test_receiver_on_name_error_coming_from_struct
    obj = Struct.new(:does_exist).new(nil)
    name_error = assert_raise(NameError) { obj[:doesnt_exist] }

    assert_equal obj, name_error.receiver
  end
end
