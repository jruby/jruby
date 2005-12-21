require 'test/minirunit'
test_check "Test Class derivation:"

require 'java'
require 'jruby'

def short_name(long_name)
  long_name.sub(/.*\./, '')
end

def compare_node(node, list)
    entry = list.delete_at(0)
    
    unless entry.nil?
      position = node.position
    
      test_equal(entry[0], short_name(node.java_class.name))
      test_equal(entry[1], position.startLine)
      test_equal(entry[2], position.endLine)
      test_equal(entry[3], position.startOffset)
      test_equal(entry[4], position.endOffset)
    end
	
    node.childNodes.each {|child| compare_node(child, list) }
end

def test_tree(expected_list, script_content)
   compare_node(JRuby::parse(script_content, ""), expected_list)
end

list = [
['NewlineNode', 0, 12, 0, 143],
['ClassNode', 0, 11, 0, 142],
['Colon2Node', 0, 0, 6, 7],
['ScopeNode', 1, 10, 14, 138],
['BlockNode', 1, 10, 14, 138],
['NewlineNode', 1, 5, 14, 75],
['DefnNode', 1, 4, 14, 74],
['ArgumentNode', 1, 1, 18, 22],
nil,   #['ArgsNode', 1, 1, 27, 35],
['ListNode', 1, 1, 24, 33],
['ArgumentNode', 1, 1, 24, 27],
['ArgumentNode', 1, 1, 29, 33],
nil,   #['ScopeNode', 2, 3, 44, 68],
nil,   #['BlockNode', 2, 3, 44, 68],
nil,   #['NewlineNode', 2, 3, 44, 60],
nil,   #['FCallNode', 2, 3, 44, 60],
nil,   #['ArrayNode', 2, 2, 48, 54],
['CallNode', 2, 2, 45, 48],
['LocalVarNode', 2, 2, 45, 48],
['LocalVarNode', 2, 2, 55, 59],
nil,   #['NewlineNode', 2, 4, 44, 69],
['TrueNode', 3, 3, 64, 68],
nil,   #['NewlineNode', 1, 11, 14, 139],
['SClassNode', 6, 10, 80, 138],
['SelfNode', 6, 6, 89, 93],
['ScopeNode', 7, 9, 98, 132],
['NewlineNode', 7, 10, 98, 133],
['DefnNode', 7, 9, 98, 132],
['ArgumentNode', 7, 7, 102, 107],
nil,   #['ArgsNode', 7, 7, 113, 114],
['ListNode', 7, 7, 109, 113],
['ArgumentNode', 7, 7, 109, 113],
['ScopeNode', 8, 8, 121, 124],
['NewlineNode', 8, 9, 121, 125],
['NilNode', 8, 8, 121, 124],
['ConstNode', 0, 0, 10, 11]
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
['NewlineNode', 0, 5, 0, 69],
['ModuleNode', 0, 4, 0, 68],
['Colon2Node', 0, 0, 7, 10],
['ScopeNode', 1, 3, 13, 64],
nil,   #['NewlineNode', 3, 4, 64, 65],
['DefnNode', 1, 3, 13, 64],
['ArgumentNode', 1, 1, 17, 21],
['ArgsNode', 1, 1, 23, 32],
['ListNode', 1, 1, 23, 32],
['ArgumentNode', 1, 1, 23, 26],
['ArgumentNode', 1, 1, 28, 32],
nil,   #['ScopeNode', 2, 3, 43, 59],
nil,   #['NewlineNode', 2, 3, 39, 58],
nil,   #['FCallNode', 2, 3, 43, 59],
nil,   #['ArrayNode', 2, 2, 47, 53],
['CallNode', 2, 2, 44, 47],
['LocalVarNode', 2, 2, 44, 47],
['LocalVarNode', 2, 2, 54, 58]
]

test_tree(list, <<END)
module Abc
  def talk( arg, arg2 )
    puts arg.to_s, arg2
  end
end
END
