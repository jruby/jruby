require 'minirunit'
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

