require 'test/minirunit'
test_check "Test BigDecimal"

require 'bigdecimal'

# no singleton methods on bigdecimal
num = BigDecimal.new("0.001")
test_exception(TypeError) { class << num ; def amethod ; end ; end }
test_exception(TypeError) { def num.amethod ; end }