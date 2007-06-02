require 'test/unit'


class TestGC < Test::Unit::TestCase

  def test_garbage_collect
    # This is the test from the standard test.rb
    begin
      1.upto(10000) do
        tmp = [0,1,2,3,4,5,6,7,8,9]
      end
      tmp = nil
      assert(true)
    rescue
      fail("GC")
    end
  end

  def test_s_disable
    # need test
  end

  def test_s_enable
    # need test
  end

  def test_s_start
    # need test
  end

end
