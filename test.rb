


# Float test

puts "Float test\n"

puts 3.5 + 5.7
puts 3.0 * 3.3
puts 2.5 / 2.1
puts 2.5 ** 4
puts 2.56

# Block test

5.times do |i|
   print i, " "
end

puts

2.step 10, 2 do |i|
    print i, " "
end

puts

puts 78.chr

#Range test

(5..10).each do |i|
    print i, " * ", i, " = " , i * i, "\n"
end
