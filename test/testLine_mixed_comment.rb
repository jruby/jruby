=begin
line 2
line 3
=end
test_equal(5, __LINE__)
# line 6
test_equal(7, __LINE__)
=begin
...
=end
test_equal(11, __LINE__)
# line 12
# line 13
test_equal(14, __LINE__)


