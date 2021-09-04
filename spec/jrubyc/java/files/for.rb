$for_result = 0

for i in [1, 2]
  $for_result += i
end

$for_nested_result = 0
for i in [1, 2]
  for j in [3, 4]
    2.times { $for_nested_result += j }
  end
end

