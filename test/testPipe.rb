require 'test/minirunit'

test_check "Test IO"

test_ok IO.respond_to?(:pipe, "IO.pipe exists")

begin
rd, wr = IO.pipe
result = ""
t1 = Thread.new { result = rd.read; rd.close }
wr.write "Foo"
wr.close
t1.join
test_equal result , "Foo"
rescue
test_fail "Pipe working"
end
