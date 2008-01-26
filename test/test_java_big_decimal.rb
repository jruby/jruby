include Java

include_class 'org.jruby.javasupport.BigDecimalHolder'

require 'test/unit'
require 'bigdecimal'

JBigDecimal = java.math.BigDecimal

class TestJavaBigDecimal < Test::Unit::TestCase
  def test_conversion
    number = BigDecimal.new("123123123123123123123123")

    holder = BigDecimalHolder.new
    holder.number = number
    
    assert_equal number, holder.number
  end
    
end
