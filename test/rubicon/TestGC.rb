$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'


class TestGC < Rubicon::TestCase

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
    skipping("need test")
  end

  def test_s_enable
    skipping("need test")
  end

  def test_s_start
    skipping("need test")
  end

end

Rubicon::handleTests(TestGC) if $0 == __FILE__
