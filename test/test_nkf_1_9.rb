require 'test/unit'
require 'nkf'
require 'test/test_helper'

class NKFTest19 < Test::Unit::TestCase
  include TestHelper
  
  def test_encoding
    assert_equal(Encoding.find("Shift_JIS"), NKF.nkf("-s", "a").encoding)
  end
end
