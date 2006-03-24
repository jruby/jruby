require 'test/minirunit'
test_check "Test hash:"

h = {1=>2,3=>4,5=>6}
h.replace({1=>100})
test_equal({1=>100}, h)

h = {1=>2,3=>4}
h2 = {3=>300, 4=>400}
h.update(h2)
test_equal(2, h[1])
test_equal(300, h[3])
test_equal(400, h[4])

h = {1=>2,3=>4}
test_exception(IndexError) { h.fetch(10) }
test_equal(2, h.fetch(1))
test_equal("hello", h.fetch(10, "hello"))
test_equal("hello 10", h.fetch(10) { |e| "hello #{e}" })

h = {}
k1 = [1]
h[k1] = 1
k1[0] = 100
test_equal(nil, h[k1])
h.rehash
test_equal(1, h[k1])

h = {1=>2,3=>4,5=>6}
test_equal([2, 6], h.values_at(1, 5))

h = {1=>2,3=>4}
test_equal(1, h.index(2))
test_equal(nil, h.index(10))
test_equal(nil, h.default_proc)
h.default = :hello
test_equal(nil, h.default_proc)
test_equal(1, h.index(2))
test_equal(nil, h.index(10))

h = Hash.new {|h,k| h[k] = k.to_i*10 }

test_ok(!nil, h.default_proc)
test_equal(100, h[10])
test_equal(0, h.default)
test_equal(20, h.default(2))

h.default = 5
test_equal(nil, h.default_proc)

test_equal(5, h[12])

class << h
 def default(k); 2; end
end

test_equal(nil, h.default_proc)
test_equal(2, h[30])

