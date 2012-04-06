require 'benchmark/ips'

class Top
  def foo; end
end

module A
  def foo; super(); end
end

module B
  def foo; super(); end
end

module C
  def foo; super(); end
end

module D
  def foo; super(); end
end

module E
   def foo; super(); end
end

class OneSuper < Top
  include A
end

class FiveSupers < Top
  include A, B, C, D, E
end

class OneSub < Top
  def foo; super(); end
end

class TwoSub < OneSub
  def foo; super(); end
end

class ThreeSub < TwoSub
  def foo; super(); end
end

class FourSub < ThreeSub
  def foo; super(); end
end

class FiveSub < FourSub
  def foo; super(); end
end


Benchmark.ips do |bm|

  bm.report('five module super') do |n|
    i = 0
    obj = FiveSupers.new
    while i < n
      obj.foo; obj.foo; obj.foo; obj.foo; obj.foo
      i+=1
    end
  end

  bm.report('one module super') do |n|
    i = 0
    obj = OneSuper.new
    while i < n
      obj.foo; obj.foo; obj.foo; obj.foo; obj.foo
      i+=1
    end
  end

  bm.report('five module super') do |n|
    i = 0
    obj = FiveSupers.new
    while i < n
      obj.foo; obj.foo; obj.foo; obj.foo; obj.foo
      i+=1
    end
  end

  bm.report('one super') do |n|
    i = 0
    obj = OneSub.new
    while i < n
      obj.foo; obj.foo; obj.foo; obj.foo; obj.foo
      i+=1
    end
  end

  bm.report('five supers') do |n|
    i = 0
    obj = FiveSub.new
    while i < n
      obj.foo; obj.foo; obj.foo; obj.foo; obj.foo
      i+=1
    end
  end
end
