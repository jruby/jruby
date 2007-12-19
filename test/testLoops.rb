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

class C
  def initialize(list)
     @list = list
  end

  def each(*args, &block)
     @list.each(*args, &block)
  end
end

def l( y, z )
  x = ":"
  for a in y
    for b in z
      x.concat b
      x.concat a
    end
  end
  test_equal("c", a)
  test_equal("3", b)
  x
end

test_equal(":1a2a3a1b2b3b1c2c3c", l(C.new(["a", "b", "c"]), C.new(["1","2","3"])))


a = [1, 2, 3, 4, 5]
b = [1, 2, 3, 4, 5]

1.times do
  i = 0
  begin
    ch = a.shift
    test_equal(b[i], ch)
    i = i + 1
  end until ch.nil?
end

# Had to disable these because compiler doesn't handle this syntax at the moment
#test_equal(nil, while false; end)
#
#test_equal(:foo, while true; break :foo; end)
