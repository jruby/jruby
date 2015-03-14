size = 10_000_000
array = Array.new(size) { 0xabcdef }
expression = 'x' + ('NX' * size)
expected_sum = 3072

loop do
  start = Time.now
  result = array.pack(expression)
  #result = array.pack_fast(expression)
  puts Time.now - start
  raise unless result.sum == expected_sum
end
