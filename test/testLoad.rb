require "test/minirunit"

test_check("Test Ruby-Init")

$ruby_init = false
require File::dirname(__FILE__) + "/RubyInitTest.jar"
test_ok($ruby_init)

