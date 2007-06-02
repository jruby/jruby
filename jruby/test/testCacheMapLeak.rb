require 'test/minirunit'
require 'java'

class TestMe; end

num = 10000
num.times { class << TestMe.new; def foo; end; end }

java.lang.System.gc
sleep 1

test_ok(num != ObjectSpace.each_object(TestMe){}, "Objects not being release by CacheMap" )