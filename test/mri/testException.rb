require 'test/minirunit'

# exception handling
test_check "exception";

begin
  raise "this must be handled"
  test_ok(false)
rescue
  test_ok(true)
end

$bad = true
begin
  raise "this must be handled no.2"
rescue
  if $bad
    $bad = false
    retry
    test_ok(false)
  end
end
test_ok(true)

# exception in rescue clause
$string = "this must be handled no.3"
begin
  begin
    raise "exception in rescue clause"
  rescue 
    raise $string
  end
  test_ok(false)
rescue
  test_ok(true) if $! == $string
end
  
# exception in ensure clause
begin
  begin
    raise "this must be handled no.4"
  ensure 
    raise "exception in ensure clause"
  end
  test_ok(false)
rescue
  test_ok(true)
end

$bad = true
begin
  begin
    raise "this must be handled no.5"
  ensure
    $bad = false
  end
rescue
end
test_ok(!$bad)

$bad = true
begin
  begin
    raise "this must be handled no.6"
  ensure
    $bad = false
  end
rescue
end
test_ok(!$bad)

$bad = true
while true
  begin
    break
  ensure
    $bad = false
  end
end
test_ok(!$bad)

test_ok(catch(:foo) {
     loop do
       loop do
	 throw :foo, true
	 break
       end
       break
       test_ok(false)			# should no reach here
     end
     false
   })


