# block-param-scope.rb [jruby-embed]
# This snippet is borrowed from http://gihyo.jp/dev/serial/01/ruby/0003

# defines local variable x
x = "bear"

# A block local variable x is used in this block but 1.9+ does not share it.
["dog", "cat", "panda"].each do |x|
  # This x is a block local variable.
  break if x == "cat"
end

# This x is a local variable since it is used outside of the block.
x

