require 'test/minirunit'
test_check "proc"

$proc = proc{|i| i}
test_ok($proc.call(2) == 2)
test_ok($proc.call(3) == 3)

$proc = proc{|i| i*2}
test_ok($proc.call(2) == 4)
test_ok($proc.call(3) == 6)

proc{
  iii=5				# nested local variable
  $proc = proc{|i|
    iii = i
  }
  $proc2 = proc {
    $x = iii			# nested variables shared by procs
  }
  # scope of nested variables
  test_ok(defined?(iii))
}.call
test_ok(!defined?(iii))		# out of scope

$x=0
$proc.call(5)
$proc2.call
test_ok($x == 5)

