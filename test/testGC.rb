require "test/minirunit"
test_check "Test GC"

GC.start

count = 0
result = ObjectSpace.each_object(Symbol) {|o|
  count += 1
}
test_equal(count, result)

# JRUBY-2388
test_ok(!Module.respond_to?(:enable))
test_ok(!Kernel.respond_to?(:start))
test_ok(!String.respond_to?(:enable))
