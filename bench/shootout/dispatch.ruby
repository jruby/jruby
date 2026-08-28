#!/usr/bin/ruby
#
# The Great Computer Language Shootout
# http://shootout.alioth.debian.org
# 
# contributed by Jesse Millikan

class BottleState
 attr_reader :tag
 private_class_method :new

 def initialize(tag)
  @tag = tag
 end

 def BottleState.initial; @@empty; end

 def BottleState.pressurized_initial; @@unpressurized_empty; end

# Thanks to dbrock on #ruby-lang on freenode for some tips on this.

 @@empty = new(1)

 def @@empty.next(bottle); bottle.state = @@full; end

 @@full = new(2)
 
 def @@full.next(bottle); bottle.state = @@sealed; end

 @@sealed = new(3)

 def @@sealed.next(bottle); bottle.state = @@empty; end

 @@unpressurized_empty = new(4)

 def @@unpressurized_empty.next(bottle); bottle.state = @@unpressurized_full; end

 @@unpressurized_full = new(5)
 
 def @@unpressurized_full.next(bottle); bottle.state = @@pressurized_unsealed; end

 @@pressurized_unsealed = new(6)
 
 def @@pressurized_unsealed.next(bottle); bottle.state = @@pressurized_sealed; end

 @@pressurized_sealed = new(7)
 
 def @@pressurized_sealed.next(bottle); bottle.state = @@unpressurized_empty; end
end

#Someone with judgement on style could pare this down a bit.
class Bottle
  attr_writer :state

 def initialize(id)
  @id = id
  @state = initial
 end

 def initial; BottleState.initial; end

 def cycle; fill; seal; empty; end
 
 def next; @state.next(self); end

 alias_method :empty, :next
 alias_method :fill, :next
 alias_method :seal, :next

 def check(c); @state.tag + @id + c; end
end

class PressurizedBottle < Bottle
 def initial; BottleState.pressurized_initial; end
 
 alias_method :pressurize, :next

 def cycle; fill; pressurize; seal; empty; end
end

def bottle_check(a1, a2, a3, a4, a5, i)
 a1.cycle; a2.cycle; a3.cycle; a4.cycle; a5.cycle

 c = i % 2

 a1.check(c) + a2.check(c) + a3.check(c) + a4.check(c) + a5.check(c)
end

n = 0
n = ARGV[0].to_i unless ARGV.empty?

b1 = Bottle.new(1); b2 = Bottle.new(2)
b3 = Bottle.new(3); b4 = Bottle.new(4)
b5 = Bottle.new(5); b6 = Bottle.new(6)
b7 = Bottle.new(7); b8 = Bottle.new(8)
b9 = Bottle.new(9); b0 = Bottle.new(0)

p1 = PressurizedBottle.new(1); p2 = PressurizedBottle.new(2)
p3 = PressurizedBottle.new(3); p4 = PressurizedBottle.new(4)
p5 = PressurizedBottle.new(5); p6 = PressurizedBottle.new(6)
p7 = PressurizedBottle.new(7); p8 = PressurizedBottle.new(8)
p9 = PressurizedBottle.new(9); p0 = PressurizedBottle.new(0)

check = 0

for i in 1..n
 check += bottle_check(b1, b2, b3, b4, b5, i)
 check += bottle_check(b6, b7, b8, b9, b0, i)

 check += bottle_check(p1, p2, p3, p4, p5, i)
 check -= bottle_check(p6, p7, p8, p9, p0, i)
end

puts "#{check}"
