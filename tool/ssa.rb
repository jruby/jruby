require 'jruby'

code1 = "a = 1; b = 2; if a < b; c = 3; else; c = 4; end; c = 5"
code2 = "a = 1; b = 2"
code3 = "
a = 1
if a < 2
  a
else
  fib(a - 1) + fib(a - 2)
end"
code4 = "
if alpha
  if beta
    if charlie
      if delta
        echo
      end
      foxtrot
    elsif golf
      hotel
    end
    india
  else
    juliet
  end
elsif kilo
  lima
end
mike"
code5 = "
if 1
  if 2
    3
  end
else
  if 4
    5
  end
end
"
code6 = "
a = 1
while a < 100
  a += 1
end
puts a
"
code7 = "
x = y = z = 1
if y >= x
  return z
else
  return tak( tak(x-1, y, z),
              tak(y-1, z, x),
              tak(z-1, x, y))
end
"

node = JRuby.parse(code7)

head = org.jruby.compiler.DAGBuilder.get_extents(node).head

require 'rubygems'
require 'graphviz'

g = GraphViz.new('G')

def add_graph(g, gnode, head, hash = {})
  hash[head.name] ||= 0
  nxt = hash[head.object_id]
  unless nxt
    hash[head.name] += 1
    shape = head.name == "PHI" ? "ellipse" : "record"
    nxt = g.add_node(head.name + hash[head.name].to_s, :label => "#{label(head)}", :shape => shape)
    hash[head.object_id] = nxt
  end
  hash["edge #{gnode.object_id},#{nxt.object_id}"] ||= (g.add_edge(gnode, nxt); true)
  if head.tail && shape
    if org.jruby.compiler.DAGBuilder::B === head
      add_graph(g, nxt, head.tail, hash)
      add_graph(g, nxt, head.alt, hash);
    else
      add_graph(g, nxt, head.tail, hash)
    end
  end
end

def label(node)
  label = node.name
  if node.payload.size > 0
    label << " [#{node.payload.to_a.join(', ')}] (#{node.type})"
  end
  label
end

root = g.add_node("root")

add_graph(g, root, head)

g.output(:output => "png", :file => "output.png")

system "open output.png"
