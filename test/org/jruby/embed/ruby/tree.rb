# tree.rb [jruby-embed]
# jsr223.JRubyEngineTest

class Tree
  attr_reader :name, :shape, :foliage, :flower

  def initialize(name, shape, foliage, flower)
    @name = name
    @shape = shape
    @foliage = foliage
    @flower = flower
  end
  def to_s
    "#{name.capitalize} is a #{shape} shaped, #{foliage} tree, and blooms #{flower.color} flowers in #{flower.bloomtime}."
  end
end

class Flower
  attr_reader :color, :bloomtime
  def initialize(color, bloomtime)
    @color = color
    @bloomtime = bloomtime
  end
end

flower = Flower.new("pink", "March - April")
Tree.new("cherry blossom", "round", "deciduous", flower)


