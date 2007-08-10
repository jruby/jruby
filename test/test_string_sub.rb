require 'test/unit'

class UnitTest < Test::Unit::TestCase

  def initialize(test_method_name)
    super(test_method_name)
  end

  EOL="\r\n"

  def check buf, e1, e2, e3

    assert_equal e1, buf.size
    head = ''

    #from cgi.rb..
    buf = buf.sub(/\A((?:.|\n)*?#{EOL})#{EOL}/n) do
      head = $1.dup
      ""
    end
    # ..cgi.rb

    assert_equal e2,  head.size
    assert_equal e3,  buf.size
  end

  def test_unit_method
    check "a"  + EOL + EOL + "a"  , 6, 3, 1  # 1byte + 2byte + 2byte + 1byte
    check "a"  + EOL + EOL + "あ" , 8, 3, 3  # 1byte + 2byte + 2byte + 3byte
    check "あ" + EOL + EOL + "a"  , 8, 5, 1  # 3byte + 2byte + 2byte + 1byte failure!!
    check "あ" + EOL + EOL + "あ" ,10, 5, 3  # 3byte + 2byte + 2byte + 3byte failure!!
  end
end

