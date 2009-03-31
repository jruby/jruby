require 'jruby'
require 'pp'

LINES = false

class FirstTransform
  include org.jruby.ast.visitor.NodeVisitor

  def initialize
    @join = nil
  end
  
  def visit_root_node(node)
    [:root, *node.child_nodes.map {|child| child.accept(self)}]
  end

  def visit_newline_node(node)
    if LINES
      [:line, node.next_node.accept(self)]
    else
      node.next_node.accept(self)
    end
  end

  def visit_block_node(node)
    [:block, *node.child_nodes.map {|child| child.accept(self)}]
  end

  def visit_local_asgn_node(node)
    [:lset, node.index, node.value_node.accept(self)]
  end

  def visit_fixnum_node(node)
    [:fixnum, node.value]
  end

  def visit_if_node(node)
    [:if, node.condition.accept(self), (node.then_body.accept(self) if node.then_body), (node.else_body.accept(self) if node.else_body)]
  end

  def visit_call_node(node)
    [:call, node.receiver_node.accept(self), :ARGS, :ITER]
  end

  def visit_fcall_node(node)
    [:call, nil, :ARGS, :ITER]
  end

  def visit_local_var_node(node)
    [:lget, node.index]
  end
end

def get_extents(node)
  case node
  when org.jruby.ast.BlockNode
    head, tail = get_extents(node.child_nodes[0])
    i = 1
    while i < node.child_nodes.size
      h, t = get_extents(node.child_nodes[i])
      tail << h
      tail = t
      i += 1
    end
  when org.jruby.ast.CallNode
    head, tail = get_extents(node.receiver_node)
    i = 0
    while i < node.args_node.child_nodes.size
      h, t = get_extents(node.args_node.child_nodes[i])
      tail << h
      tail = t
      i += 1
    end
    t = [:call, node.name, i]
    tail << t
    tail = t
  when org.jruby.ast.FCallNode
    head = tail = [:self]
    i = 0
    while i < node.args_node.child_nodes.size
      h, t = get_extents(node.args_node.child_nodes[i])
      tail << h
      tail = t
      i += 1
    end
    t = [:call, node.name, i]
    tail << t
    tail = t
  when org.jruby.ast.FixnumNode
    head = tail = [:fixnum, node.value]
  when org.jruby.ast.IfNode
    head, tail = get_extents(node.condition)
    join = [:phi]
    if_head = [:if]

    tail << if_head
    tail = if_head

    if node.then_body
      then_head, then_tail = get_extents(node.then_body)
      then_tail << join
    else
      then_head = then_tail = join
    end

    if node.else_body
      else_head, else_tail = get_extents(node.else_body)
      else_tail << join
    else
      else_head = else_tail = join
    end
    
    tail << [then_head, else_head]
    tail = join
  when org.jruby.ast.LocalAsgnNode
    head, tail = get_extents(node.value_node)
    t = [:lset, node.index]
    tail << t
    tail = t
  when org.jruby.ast.LocalVarNode
    head = tail = [:lget, node.index]
  when org.jruby.ast.NewlineNode
    return get_extents(node.next_node)
  when org.jruby.ast.RootNode
    return get_extents(node.body_node)
  when org.jruby.ast.VCallNode
    head = tail = [:self]
    t = [:call, node.name, 0]
    tail << t
    tail = t
  else
    raise "unknown node: " + node.to_s
  end

  return head, tail
end

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

node = JRuby.parse(code4)

pp get_extents(node)[0]

head, tail = get_extents(node)
puts(head.inspect)

require 'rubygems'
require 'graphviz'

g = GraphViz.new('G')

def add_graph(g, prev, head, hash = {})
  hash[head[0]] ||= 0
  nxt = hash[head.object_id]
  unless nxt
    hash[head[0]] += 1
    shape = head[0] == :phi ? "ellipse" : "record"
    nxt = g.add_node(head[0].to_s + hash[head[0]].to_s, :label => "#{label(head)}", :shape => shape)
    hash[head.object_id] = nxt
  end
  hash["edge #{prev.object_id},#{nxt.object_id}"] ||= (g.add_edge(prev, nxt); true)
  tails = head[-1]
  if Array === tails
    if Array === tails[0]
      tails.each do |tail|
        add_graph(g, nxt, tail, hash)
      end
    elsif Symbol === tails[0]
      add_graph(g, nxt, tails, hash)
    end
  end
end

def label(node)
  case node[0]
  when :lget
    "lget #{node[1]}"
  when :lset
    "lset #{node[1]}"
  when :call
    "call #{node[1]}"
  when :fixnum
    "fixnum #{node[1]}"
  else
    node[0].to_s
  end
end

root = g.add_node("root")

add_graph(g, root, head)

g.output(:output => "png", :file => "output.png")

system "open output.png"
