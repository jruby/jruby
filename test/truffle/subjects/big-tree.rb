# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

begin
  require 'objspace'
rescue Exception
end

class Node
  attr_reader :left
  attr_reader :right
  attr_accessor :label

  def initialize(left, right, label)
    @left = left
    @right = right
    @label = label
  end
end

def create_tree(size)
  if size == 1
    left = nil
    right = nil
  elsif size == 2
    left = create_tree(size - 1)
    right = nil
  else
    child_size = size - 1
    left = create_tree(child_size / 2)
    right = create_tree(child_size / 2 + child_size % 2)
  end

  Node.new(left, right, size)
end

size = 2 ** 23

tree = create_tree(size)

# In C the object would have 2 pointers and an int. In reality we could could
# beat this with compressed OOPs.

expected = size * (2 * 8 + 4)
puts expected

begin
  actual = ObjectSpace.memsize_of_all
  overhead = actual / expected.to_f
  puts overhead
rescue Exception
end

def visit_branch(tree)
  depth = 0
  node = tree
  until node.nil?
    node = node.left
    depth += 1
  end
  depth
end

def bench(i, tree)
  x = 0
  i.times do
    x += visit_branch(tree)
  end
  x
end

bench(1, tree)
bench(2, tree)
bench(3, tree)
bench(10_000_000, tree)

10_000.times do
  bench(10, tree)
end

sleep 10

start = Time.now
bench(10_000_000, tree)
puts Time.now - start
