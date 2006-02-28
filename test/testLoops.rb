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

# Make sure do while works
t = 0
loop_count = 0
 
begin
t = t > 0 ? 0 : 1
loop_count = loop_count + 1
end while t > 0

test_ok(2, t) 

# make sure until and while don't fire before condition checking

x = 1
until true do; x = 2; end
while false; x = 2; end

test_equal(1, x)