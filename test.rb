size = 1_000
expression = 'x' + ('NX' * size)
array = [0x1234] * size

loop do
  start = Time.now
  10_000.times do
    array.pack_fast(expression)
  end
  puts Time.now - start
end
