require 'test/minirunit'

test_check "Test Taint"

test_ok(Module.new.taint.dup.tainted?)
test_ok(Class.new.taint.dup.tainted?)
test_ok(Object.new.taint.dup.tainted?)
test_ok(String.new.taint.dup.tainted?)
test_ok([].taint.dup.tainted?)
test_ok({}.taint.dup.tainted?)
test_ok(//.taint.dup.tainted?)

test_ok(Module.new.taint.clone.tainted?)
test_ok(Class.new.taint.clone.tainted?)
test_ok(Object.new.taint.clone.tainted?)
test_ok(String.new.taint.clone.tainted?)
test_ok([].taint.clone.tainted?)
test_ok({}.taint.clone.tainted?)
test_ok(//.taint.clone.tainted?)

test_check "Test Freeze"

test_ok(!Module.new.freeze.dup.frozen?)
test_ok(!Class.new.freeze.dup.frozen?)
test_ok(!Object.new.freeze.dup.frozen?)
test_ok(!String.new.freeze.dup.frozen?)
test_ok(![].freeze.dup.frozen?)
test_ok(!{}.freeze.dup.frozen?)
test_ok(!//.freeze.dup.frozen?)

test_ok(Module.new.freeze.clone.frozen?)
test_ok(Class.new.freeze.clone.frozen?)
test_ok(Object.new.freeze.clone.frozen?)
test_ok(String.new.freeze.clone.frozen?)
test_ok([].freeze.clone.frozen?)
test_ok({}.freeze.clone.frozen?)
test_ok(//.freeze.clone.frozen?)
