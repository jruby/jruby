require 'test/unit'

class TestString < Test::Unit::TestCase

  # JRUBY-4987
  def test_paragraph
    # raises ArrayIndexOutOfBoundsException in 1.5.1
    assert_equal ["foo\n"], "foo\n".lines('').to_a
  end

  def test_try_squeeze
    ' '.squeeze
    try ' ', :squeeze # ArrayIndexOutOfBoundsException
  end

  private

  def try(obj, *a, &b) # ~ AS 4.2
    if a.empty? && block_given?
      if b.arity == 0
        obj.instance_eval(&b)
      else
        yield obj
      end
    else
      obj.public_send(*a, &b)
    end
  end

end
