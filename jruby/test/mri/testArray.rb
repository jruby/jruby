require 'test/minirunit'

test_check "array"
test_ok([1, 2] + [3, 4] == [1, 2, 3, 4])
test_ok([1, 2] * 2 == [1, 2, 1, 2])
test_ok([1, 2] * ":" == "1:2")

test_ok([1, 2].hash == [1, 2].hash)

test_ok([1,2,3] & [2,3,4] == [2,3])
test_ok([1,2,3] | [2,3,4] == [1,2,3,4])
test_ok([1,2,3] - [2,3] == [1])

$x = [0, 1, 2, 3, 4, 5]
test_ok($x[2] == 2)
test_ok($x[1..3] == [1, 2, 3])
test_ok($x[1,3] == [1, 2, 3])

$x[0, 2] = 10
test_ok($x[0] == 10 && $x[1] == 2)
  
$x[0, 0] = -1
test_ok($x[0] == -1 && $x[1] == 10)

$x[-1, 1] = 20
test_ok($x[-1] == 20 && $x.pop == 20)

# array and/or
test_ok(([1,2,3]&[2,4,6]) == [2])
test_ok(([1,2,3]|[2,4,6]) == [1,2,3,4,6])

# compact
$x = [nil, 1, nil, nil, 5, nil, nil]
$x.compact!
test_ok($x == [1, 5])

# uniq
$x = [1, 1, 4, 2, 5, 4, 5, 1, 2]
$x.uniq!
test_ok($x == [1, 4, 2, 5])

# empty?
test_ok(!$x.empty?)
$x = []
test_ok($x.empty?)

# sort
$x = ["it", "came", "to", "pass", "that", "..."]
$x = $x.sort.join(" ")
test_ok($x == "... came it pass that to")
$x = [2,5,3,1,7]
$x.sort!{|a,b| a<=>b}		# sort with condition
test_ok($x == [1,2,3,5,7])
$x.sort!{|a,b| b-a}		# reverse sort
test_ok($x == [7,5,3,2,1])

# split test
$x = "The Botest_ok of Mormon"
test_ok($x.split(//).reverse!.join == $x.reverse)
test_ok($x.reverse == $x.reverse!)
test_ok("1 byte string".split(//).reverse.join(":") == "g:n:i:r:t:s: :e:t:y:b: :1")
$x = "a b c  d"
test_ok($x.split == ['a', 'b', 'c', 'd'])
test_ok($x.split(' ') == ['a', 'b', 'c', 'd'])
test_ok(defined? "a".chomp)
test_ok("abc".scan(/./) == ["a", "b", "c"])
test_ok("1a2b3c".scan(/(\d.)/) == [["1a"], ["2b"], ["3c"]])
# non-greedy match
test_ok("a=12;b=22".scan(/(.*?)=(\d*);?/) == [["a", "12"], ["b", "22"]])

$x = [1]
test_ok(($x * 5).join(":") == '1:1:1:1:1')
test_ok(($x * 1).join(":") == '1')
test_ok(($x * 0).join(":") == '')

*$x = (1..7).to_a
test_ok($x.size == 1)
test_ok($x == [[1, 2, 3, 4, 5, 6, 7]])


