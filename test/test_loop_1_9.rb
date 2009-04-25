require 'test/unit'

class TestLoop19 < Test::Unit::TestCase
  def test_loop_catches_rescues_iteration_and_does_not_export_scope
    loop do
      n = 1
      raise StopIteration
    end
    assert_raises NameError do n end
  end

  def test_loop_does_not_catch_other_exceptions
    assert_raise ArgumentError do
      loop do
	raise ArgumentError
      end
    end
  end

  def test_loop_rescues_subexceptions
    finish = Class::new( StopIteration )
    loop do
      raise finish
    end
    assert true
  end
end
