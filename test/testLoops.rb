require 'test/minirunit'
test_check "Test Loops:"
i = 0
j = 0
a = []
while (i < 10)
   break if (i == 6)

   j = j + 1
   a.push i

   redo if (j < 2)

   j = 0
   i = i + 1
end
test_ok([0,0,1,1,2,2,3,3,4,4,5,5] == a)
