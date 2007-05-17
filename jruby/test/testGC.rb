require "test/minirunit"
test_check "Test GC"

GC.start

count = 0
result = ObjectSpace.each_object(Symbol) {|o|
  count += 1
}
test_equal(count, result)
