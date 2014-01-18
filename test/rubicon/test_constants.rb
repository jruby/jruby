require 'test/unit'

class TestConstants < Test::Unit::TestCase
  def testCharacterConstantsAssumingAscii
    assert_equal("a"[0], ?a)
    assert_equal(?a, ?a)
    assert_equal("\x01", ?\C-a )
#    assert_equal("\xE1", ?\M-a)  
#    assert_equal("\x81", ?\M-\C-a)
    assert_equal(?A, "a".upcase![0])
    assert_equal(?a, "A".downcase![0])

    x = "abcdef"
    y = [ ?a, ?b, ?c, ?d, ?e, ?f ]
    y.map! {|ch| ch.getbyte(0)}
    x.each_byte do |ch|
      assert_equal(y.shift, ch)
    end
  end


  
  TEST1 = 1
  TEST2 = 2

  module Const
    TEST3 = 3
    TEST4 = 4
  end

  module Const2
    TEST3 = 6
    TEST4 = 8
  end

  def testConstInModule
    self.class.instance_eval {include Const}

    assert_equal([1, 2, 3, 4], [TEST1,TEST2,TEST3,TEST4])

    self.class.instance_eval {include Const2}
    assert_equal([1, 2, 6, 8], [TEST1,TEST2,TEST3,TEST4])
  end

end
