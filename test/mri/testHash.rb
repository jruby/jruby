require 'minirunit'

test_check "hash"
$x = {1=>2, 2=>4, 3=>6}
$y = {1, 2, 2, 4, 3, 6}

test_ok($x[1] == 2)

test_ok(begin   
     for k,v in $y
       raise if k*2 != v
     end
     true
   rescue
     false
   end)

test_ok($x.length == 3)
test_ok($x.has_key?(1))
test_ok($x.has_value?(4))
test_ok($x.indexes(2,3) == [4,6])
test_ok($x == {1=>2, 2=>4, 3=>6})

$z = $y.keys.join(":")
test_ok($z == "1:2:3")

$z = $y.values.join(":")
test_ok($z == "2:4:6")
test_ok($x == $y)

$y.shift
test_ok($y.length == 2)

$z = [1,2]
$y[$z] = 256
test_ok($y[$z] == 256)

$x = [1,2,3]
$x[1,0] = $x
test_ok($x == [1,1,2,3,2,3])

$x = [1,2,3]
$x[-1,0] = $x
test_ok($x == [1,2,1,2,3,3])

$x = [1,2,3]
$x.concat($x)
test_ok($x == [1,2,3,1,2,3])

