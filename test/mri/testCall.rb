require 'test/minirunit'

test_check "call"

def aaa(a, b=100, *rest)
  res = [a, b]
  res += rest if rest
  return res
end

# not enough argument
begin
  aaa()				# need at least 1 arg
  test_ok(false)
rescue
  test_ok(true)
end

begin
  aaa				# no arg given (exception raised)
  test_ok(false)
rescue
  test_ok(true)
end

test_equal([1, 100], aaa(1))
test_equal([1, 2], aaa(1, 2))
test_equal([1, 2, 3, 4], aaa(1, 2, 3, 4))
test_equal([1, 2, 3, 4], aaa(1, *[2, 3, 4]))
