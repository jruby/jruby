require 'test/minirunit'
test_check "Test Source Positions:"

require 'java'
require 'jruby'

def short_name(long_name)
  long_name.sub(/.*\./, '')
end

def node_string(node)
  p = node.position
  "#{short_name(node.java_class.name)},#{p.startLine},#{p.endLine},#{p.startOffset},#{p.endOffset}"
end

def test_pos_ok(a, b, entry, node)
  test_ok(a == b, "Expected [#{entry.join(',')}] but got [#{node_string(node)}]")
end

def compare_node(node, list)
    entry = list.delete_at(0)
    
    unless entry.nil? 
      position = node.position
    
      test_pos_ok(entry[0], short_name(node.java_class.name), entry, node)
      test_pos_ok(entry[1], position.startLine, entry, node)
      test_pos_ok(entry[2], position.endLine, entry, node)
      test_pos_ok(entry[3], position.startOffset, entry, node)
      test_pos_ok(entry[4], position.endOffset, entry, node)
    end
	
    node.childNodes.each {|child| compare_node(child, list) }
end

def test_tree(expected_list, script_content)
   compare_node(JRuby::parse(script_content, ""), expected_list)
end

def print_tree(node)
	p node_string(node)

    node.childNodes.each {|child| print_tree(child) }
end

list = [
nil, #['NewlineNode', 0, 1, 0, 4],
['SymbolNode', 0, 0, 0, 3]
]

test_tree(list, <<EOF)
:foo
EOF

list = [
nil, #['NewlineNode', 0, 1, 0, 2],
['VCallNode', 0, 0, 0, 2],
]

test_tree(list, <<EOF)
foo
EOF

list = [
nil, #['NewlineNode', 0, 1, 0, 2],
['VCallNode', 0, 0, 0, 2],
]

test_tree(list, "foo")

list = [
nil, #['NewlineNode', 0, 2, 0, 11],
['DefnNode', 0, 1, 0, 10],
['ArgumentNode', 0, 0, 4, 6],
nil, #['ArgsNode', 0, 1, 7, 8],
nil, #[ScopeNode, 1, 1, 10, 10]
]

test_tree(list, <<EOF)
def foo
end
EOF

list = [
nil,   #['NewlineNode', 0, 12, 0, 142],
['ClassNode', 0, 11, 0, 141],
['Colon2Node', 0, 0, 6, 6],
['ScopeNode', 1, 10, 14, 137],
['BlockNode', 1, 10, 14, 137],
['DefnNode', 1, 4, 14, 73],
['ArgumentNode', 1, 1, 18, 21],
nil,   #['ArgsNode', 1, 1, 27, 35],
['ListNode', 1, 1, 24, 32],
['ArgumentNode', 1, 1, 24, 26],
['ArgumentNode', 1, 1, 29, 32],
nil,   #['ScopeNode', 2, 3, 44, 68],
nil,   #['BlockNode', 2, 3, 44, 68],
nil,   #['FCallNode', 2, 3, 44, 60],
nil,   #['ArrayNode', 2, 2, 48, 54],
['CallNode', 2, 2, 45, 47],
['LocalVarNode', 2, 2, 45, 47],
['LocalVarNode', 2, 2, 55, 58],
nil,   #['NewlineNode', 2, 4, 44, 69],
['TrueNode', 3, 3, 64, 67],
nil,   #['NewlineNode', 1, 11, 14, 139],
['SClassNode', 6, 10, 80, 137],
['SelfNode', 6, 6, 89, 92],
['ScopeNode', 7, 9, 98, 131],
['NewlineNode', 7, 10, 98, 133],
['DefnNode', 7, 9, 98, 131],
['ArgumentNode', 7, 7, 102, 106],
nil,   #['ArgsNode', 7, 7, 113, 114],
['ListNode', 7, 7, 109, 112],
['ArgumentNode', 7, 7, 109, 112],
['ScopeNode', 8, 8, 121, 123],
['NewlineNode', 8, 9, 121, 125],
['NilNode', 8, 8, 121, 123],
['ConstNode', 0, 0, 10, 10]
]

test_tree(list, <<END)
class A < B
  def talk( arg, arg2 )
    puts arg.to_s, arg2
    true
  end
  
  class << self
    def talky( arg1)
      nil
    end
  end
end
END

list = [
nil,   #['NewlineNode', 0, 5, 0, 68],
['ModuleNode', 0, 4, 0, 67],
['Colon2Node', 0, 0, 7, 9],
['ScopeNode', 1, 3, 13, 63],
nil,   #['NewlineNode', 3, 4, 64, 65],
['DefnNode', 1, 3, 13, 63],
['ArgumentNode', 1, 1, 17, 20],
['ArgsNode', 1, 1, 23, 31],
['ListNode', 1, 1, 23, 31],
['ArgumentNode', 1, 1, 23, 25],
['ArgumentNode', 1, 1, 28, 31],
nil,   #['ScopeNode', 2, 3, 43, 59],
nil,   #['NewlineNode', 2, 3, 39, 58],
nil,   #['FCallNode', 2, 3, 43, 59],
nil,   #['ArrayNode', 2, 2, 47, 53],
['CallNode', 2, 2, 44, 46],
['LocalVarNode', 2, 2, 44, 46],
['LocalVarNode', 2, 2, 54, 57]
]

test_tree(list, <<END)
module Abc
  def talk( arg, arg2 )
    puts arg.to_s, arg2
  end
end
END

list = [
nil,
['ClassNode', 0, 6, 0, 81],
['Colon2Node', 0, 0, 6, 9],
nil, #['ScopeNode', 1, 5, 20, 77],
nil, #['BlockNode', 1, 5, 20, 77],
['ClassVarDeclNode', 1, 1, 13, 24],
['FixnumNode', 1, 1, 24, 24],
nil,
['DefnNode', 2, 5, 28, 77],
['ArgumentNode', 2, 2, 32, 34],
['ArgsNode', 2, 3, 35, 36],
nil, #['ScopeNode', 3, 4, 45, 70],
nil, #['BlockNode', 3, 4, 45, 70],
['InstAsgnNode', 3, 3, 40, 54],
['StrNode', 3, 3, 49, 54],
nil,
['LocalAsgnNode', 4, 4, 60, 71],
['FixnumNode', 4, 4, 71, 71]
]

test_tree(list, <<END)
class Test
  @@static = 5
  def foo
    @field = "test"
    localvar = 0
  end
end
END
