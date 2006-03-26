require 'test/minirunit'
test_check "Test Time:"

t1 = Time.at(981173106)
t2 = Time.utc(2001, 2, 3, 4, 5, 6)
t3 = Time.at(981201906)
t4 = Time.local(2001, 2, 3, 4, 5, 6)

test_equal(true, t1 == t2)
test_equal(true, t1 === t2)
test_equal(false, t1.equal?(t2))
test_equal(0, t1 <=> t2)

test_equal(false, t3 == t4)
test_equal(false, t3 === t4)
test_equal(false, t3.equal?(t4))
test_equal(1, t3 <=> t4)

t = Time.at(0.5)
test_equal(0, t.tv_sec)
#test_equal(500_000, t.tv_usec)

t = Time.at(0.1)
test_equal(0, t.tv_sec)
#test_equal(100_000, t.tv_usec)

t = Time.at(0.9)
test_equal(0, t.tv_sec)
#test_equal(900_000, t.tv_usec)

# Time floors floating point values if explicit usecs provided (odd)
t = Time.at(0.5, 500)
test_equal(0, t.tv_sec)
test_equal(500, t.tv_usec)

t = Time.at(0.1, 500)
test_equal(0, t.tv_sec)
test_equal(500, t.tv_usec)

t = Time.at(0.9, 500)
test_equal(0, t.tv_sec)
test_equal(500, t.tv_usec)

# test comparison with nil
t = Time.now
test_equal(nil, t == nil)

# Time.utc can accept float values (by turning them into ints)
test_no_exception { Time::utc(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0) }
