
#def ttt
#end
#ttt{}

# yield at top level

$x = [1, 2, 3, 4]
$y = []

# iterator over array
for i in $x
  $y.push i
end

# nested iterator
def tt
  1.upto(10) {|i|
    yield i
  }
end

tt{|i| break if i == 5}

