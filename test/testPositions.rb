require 'test/minirunit'
test_check "Test Source Positions:"

require 'java'
require 'jruby'

def short_name(long_name)
  long_name.sub(/.*\./, '')
end

def node_string(node)
  return "#{short_name(node.java_class.name)}" unless node.position
  p = node.position
  "#{short_name(node.java_class.name)},#{p.startLine},#{p.endLine},#{p.startOffset},#{p.endOffset}"
end

def test_pos_ok(a, b, entry, node, test_name)
  test_ok(a == b, "Expected [#{entry.join(',')}] but got [#{node_string(node)}] for #{test_name}")
end

def compare_node(node, list, test_name)
    entry = list.delete_at(0)
    
    unless entry.nil? 
      position = node.position
    
      test_pos_ok(entry[0], short_name(node.java_class.name), entry, node, test_name)
      test_pos_ok(entry[1], position.startLine, entry, node, test_name)
      test_pos_ok(entry[2], position.endLine, entry, node, test_name)
      test_pos_ok(entry[3], position.startOffset, entry, node, test_name)
      test_pos_ok(entry[4], position.endOffset, entry, node, test_name)
    end
	
    node.childNodes.each {|child| compare_node(child, list, test_name) }
end

def test_tree(expected_list, script_content, test_name=nil, verbose=false)
   root = JRuby::parse(script_content, "", true)
   print_tree(root) if verbose
   compare_node(root, expected_list, test_name)
end

def print_tree(node, indent="")
	puts indent + node_string(node)

    node.childNodes.each {|child| print_tree(child, indent + "  ") }
end

########################################################################
#### Call/method tests
########################################################################

list = [
nil,
nil, #['NewlineNode',1,2,2,6],
  ['VCallNode',1,1,1,5]
]
test_tree(list, <<'END', "operation [paren-less no args]")

puts
END

list = [
nil,
nil, #['NewlineNode',0,1,0,4],
['VCallNode',0,0,0,4],
]
test_tree(list, <<'END', "operation [paren-less no args]")
puts
END

list = [
nil,
nil, #['NewlineNode',0,1,0,6],
['FCallNode',0,0,0,6],
  ['ArrayNode',0,0,5,6],
    ['FixnumNode',0,0,5,6],
]
test_tree(list, <<'END', "operation [paren-less single arg]")
puts 3
END

list = [
nil,
nil, #['NewlineNode',0,1,0,9],
['FCallNode',0,0,0,9],
  ['ArrayNode',0,0,5,9],
    ['FixnumNode',0,0,5,6],
    ['FixnumNode',0,0,8,9],
]
test_tree(list, <<'END', "operation [paren-less n args]")
puts 3, 5
END

list = [
nil,
nil, #['NewlineNode',0,1,0,5],
['FCallNode',0,0,0,6],
  ['ArrayNode',0,0,4,6]
]
test_tree(list, <<'END', "operation [parens no args]")
puts()
END

list = [
nil,
nil, #['NewlineNode',0,1,0,9],
['FCallNode',0,0,0,7],
  ['ArrayNode',0,0,4,7],
    ['FixnumNode',0,0,5,6],
]
test_tree(list, <<'END', "operation [parens single arg]")
puts(3)
END

list = [
nil,
nil, #['NewlineNode',0,1,0,10],
['FCallNode',0,0,0,10],
  ['ArrayNode',0,0,4,10],
    ['FixnumNode',0,0,5,6],
    ['FixnumNode',0,0,8,9],
]
test_tree(list, <<'END', "operation [parens n args]")
puts(3, 5)
END

list = [
nil,
nil, #['NewlineNode',0,1,0,9],
['CallNode',0,0,0,9],
  ['ConstNode',0,0,0,5],
]
test_tree(list, <<'END', "primary.operation [paren-less no args]")
Array.new
END

list = [
nil,
nil, #['NewlineNode',0,1,0,9],
['CallNode',0,0,0,11],
  ['ConstNode',0,0,0,5],
  ['ArrayNode',0,0,10,11],
    ['FixnumNode',0,0,10,11],
]
test_tree(list, <<'END', "primary.operation [paren-less single arg]")
Array.new 3
END

list = [
nil,
nil, #['NewlineNode',0,1,0,9],
['CallNode',0,0,0,14],
  ['ConstNode',0,0,0,5],
  ['ArrayNode',0,0,10,14],
    ['FixnumNode',0,0,10,11],
    ['FixnumNode',0,0,13,14]
]
test_tree(list, <<'END', "primary.operation [paren-less n args]")
Array.new 3, 5
END

list = [
nil,
nil, #['NewlineNode',0,1,0,11],
['CallNode',0,0,0,11],
  ['ConstNode',0,0,0,5],
  ['ArrayNode',0,0,9,11],
]
test_tree(list, <<'END', "primary.operation [parens no args]")
Array.new()
END

list = [
nil,
nil, #['NewlineNode',0,1,0,14],
['CallNode',0,0,0,14],
  ['ConstNode',0,0,0,5],
  ['ArrayNode',0,0,9,14],
    ['StrNode',0,0,10,13],
]
test_tree(list, <<'END', "primary.operation [parens one arg]")
Array.new("a")
END

list = [
nil,
nil, #['NewlineNode',0,1,0,14],
['CallNode',0,0,0,17],
  ['ConstNode',0,0,0,5],
  ['ArrayNode',0,0,9,17],
    ['StrNode',0,0,10,13],
    ['FixnumNode',0,0,15,16]
]
test_tree(list, <<'END', "primary.operation [parens one arg]")
Array.new("a", 3)
END

list = [
nil,
nil, # ['NewlineNode',0,1,0,25]
['CallNode',0,0,0,24],
  ['ConstNode',0,0,0,6],
  ['ArrayNode',0,0,11,24],
    ['CallNode',0,0,11,24],
      ['StrNode',0,0,11,16],
      ['ArrayNode',0,0,19,24],
        ['StrNode',0,0,19,24]
]
test_tree(list, <<'END', "postfix around infix method call [no-parens 1-args]")
String.new "aaa" + "bbb"
END

list = [
nil,
nil, #['NewlineNode',0,1,0,18]
  ['CallNode',0,0,0,17],
    ['ConstNode',0,0,0,6],
    ['ArrayNode',0,0,11,17],
      ['CallNode',0,0,11,17],
        ['FixnumNode',0,0,11,12]
]
test_tree(list, <<'END', "postfix around postfix [1 arg around no arg]")
String.new 5.to_s
END

list = [
nil,
nil, # ['NewlineNode',0,1,0,27],
  ['CallNode',0,0,0,26],
    ['ConstNode',0,0,0,5],
    ['ArrayNode',0,0,10,26],
      ['CallNode',0,0,10,26],
        ['ConstNode',0,0,10,16],
        ['ArrayNode',0,0,20,26],
          ['FixnumNode',0,0,21,25]
]
test_tree(list, <<'END', "postfix around postfix [1 arg parens around no arg]")
Array.new String.new(1234)
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['OrNode',0,0,0,6],
    ['FixnumNode',0,0,0,1],
    ['FixnumNode',0,0,5,6]
]
test_tree(list, <<'END', "|| operator")
1 || 2
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['AndNode',0,0,0,6],
    ['FixnumNode',0,0,0,1],
    ['FixnumNode',0,0,5,6]
]
test_tree(list, <<'END', "&& operator")
1 && 2
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['CallNode',0,0,0,6],
    ['FixnumNode',0,0,0,1],
    ['ArrayNode',0,0,5,6],
      ['FixnumNode',0,0,5,6]
]
test_tree(list, <<'END', "== operator")
1 == 2
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['NotNode',0,0,0,6],
    ['CallNode',0,0,0,6],
      ['FixnumNode',0,0,0,1],
      ['ArrayNode',0,0,5,6],
        ['FixnumNode',0,0,5,6]
]
test_tree(list, <<'END', "!= operator")
1 != 2
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['NotNode',0,0,0,2],
    ['FixnumNode',0,0,1,2]
]
test_tree(list, <<'END', "! operator")
!1
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['ZArrayNode',0,0,0,2]
]
test_tree(list, <<'END', "[] ZArray")
[]
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['ArrayNode',0,0,0,12],
    ['FixnumNode',0,0,1,3],
    ['StrNode',0,0,5,11]
]
test_tree(list, <<'END', "[] with multiple args")
[55, "test"]
END

list = [
nil, #['NewlineNode',0,1,1,13],
nil,
  ['ZArrayNode',0,0,0,4]
]
test_tree(list, <<'END', "%w() ZArray")
%w{}
END

list = [
nil,
nil, #['NewlineNode',0,1,1,13],
  ['ArrayNode',0,0,0,11],
    ['StrNode',0,0,3,5],
    ['StrNode',0,0,6,10]
]

test_tree(list, <<'END', "%w{} with multiple args")
%w{55 test}
END


list = [
nil,
nil, #['NewlineNode',0,1,0,6],
  ['FCallNode',0,0,0,6],
    ['IterNode',0,0,4,6]
]
test_tree(list, <<'END', "operation brace_block [paren-less no-args block")
foo {}
END

list = [
nil,
nil, #['NewlineNode',0,1,0,6],
  ['FCallNode',0,1,0,10],
    ['IterNode',0,1,4,10]
]

test_tree(list, <<'END', "operation brace_block [paren-less no-args block")
foo do
end
END

list = [
nil,
nil, #['NewlineNode',0,1,6,9],
  ['FCallNode',0,0,0,8],
    ['ArrayNode',0,0,3,5],
    ['IterNode',0,0,6,8]
]

test_tree(list, <<'END', "operation brace_block [parens no-args block")
foo() {}
END

list = [
nil,
nil, #['NewlineNode',0,1,6,9],
  ['FCallNode',0,1,0,12],
    ['ArrayNode',0,0,3,5],
    ['IterNode',0,1,6,12]
]

test_tree(list, <<'END', "operation brace_block [parens no-args block")
foo() do
end
END

list = [
nil,
nil, #['NewlineNode',0,2,6,17],
  ['FCallNode',0,1,0,16],
    ['ArrayNode',0,0,3,5],
    ['IterNode',0,1,6,16],
      ['DAsgnNode',0,0,10,11]
]

test_tree(list, <<'END', "operation brace_block [parens no-args block arg")
foo() do |a|
end
END

list = [
nil,
nil, #['NewlineNode',0,2,6,19],
  ['FCallNode',0,1,0,18],
    ['ArrayNode',0,0,3,5],
    ['IterNode',0,1,6,18],
      ['MultipleAsgnNode',0,0,9,14],
        ['ArrayNode',0,0,10,13],
          ['DAsgnNode',0,0,10,11],
          ['DAsgnNode',0,0,12,13]
]

test_tree(list, <<'END', "operation brace_block [parens no-args block args")
foo() do |a,b|
end
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['FCallNode',0,1,0,22],
    ['ArrayNode',0,0,3,9],
      ['VCallNode',0,0,4,5],
      ['VCallNode',0,0,7,8],
    ['IterNode',0,1,10,22],
      ['MultipleAsgnNode',0,0,13,18],
        ['ArrayNode',0,0,14,17],
          ['DAsgnNode',0,0,14,15],
          ['DAsgnNode',0,0,16,17]
]

test_tree(list, <<'END', "operation brace_block [parens args block args")
foo(c, d) do |a,b|
end
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['YieldNode',0,0,0,5],
]

test_tree(list, <<'END', "yield")
yield
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['YieldNode',0,0,0,7],
]

test_tree(list, <<'END', "yield()")
yield()
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['YieldNode',0,0,0,8],
    ['FixnumNode',0,0,6,7]
]

test_tree(list, <<'END', "yield(1)")
yield(1)
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['ReturnNode',0,0,0,6],
]

test_tree(list, <<'END', "return")
return
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['ReturnNode',0,0,0,8],
    ['FixnumNode',0,0,7,8]
]

test_tree(list, <<'END', "return 1")
return 1
END

# FIXME: null values passed up cannot provide pos info while additional tokens may get read
#list = [
#nil, #['NewlineNode',0,2,10,23],
#  ['ReturnNode',0,0,0,8]
#]
#
#test_tree(list, <<'END', "return()", true)
#return()
#END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['ReturnNode',0,0,0,9],
    nil, #['NewlineNode',0,2,10,23],
      ['FixnumNode',0,0,7,8]
]

test_tree(list, <<'END', "return(1)")
return(1)
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['NextNode',0,0,0,4],
]

test_tree(list, <<'END', "next")
next
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['NextNode',0,0,0,6],
]

# FIXME: null values passed up cannot provide pos info while additional tokens may get read
#test_tree(list, <<'END', "next()", true)
#next()
#END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['NextNode',0,0,0,6],
    ['FixnumNode',0,0,5,6]
]

test_tree(list, <<'END', "next 5")
next 5
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['NextNode',0,0,0,7],
    nil, #['NewlineNode',0,2,10,23],
    ['FixnumNode',0,0,5,6]
]

test_tree(list, <<'END', "next(5)")
next(5)
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['BreakNode',0,0,0,5],
]

test_tree(list, <<'END', "break")
break
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['BreakNode',0,0,0,7],
    ['FixnumNode',0,0,6,7]
]

test_tree(list, <<'END', "break 5")
break 5
END

list = [
nil,
nil, #['NewlineNode',0,2,10,23],
  ['RedoNode',0,0,0,4],
]

test_tree(list, <<'END', "redo")
redo
END

########################################################################
#### string tests
########################################################################

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,8]
]

test_tree(list, <<'END', "' string")
'simple'
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,10]
]

test_tree(list, <<'END', "%q{ string")
%q{simple}
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,10]
]

test_tree(list, <<'END', "%q[ (something other than {) string")
%q[s{mple]
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,10]
]

test_tree(list, <<'END', "%q[ (something other than { 2) string")
%q<simple>
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,8]
]

test_tree(list, <<'END', "\" string")
"simple"
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,10]
]

test_tree(list, <<'END', "%Q{ string")
%Q{simple}
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,0,0,9]
]

test_tree(list, <<'END', "%{ string")
%{simple}
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['DStrNode',0,0,0,17],
    ['StrNode',0,0,0,9],
    ['EvStrNode',0,0,9,16],
      nil, #['NewlineNode',0,0,11,16],
        ['VCallNode',0,0,11,15]
]

test_tree(list, <<'END', "%{ string w expression")
%{simple #{to_s}}
END

# FIXME: We optimize the source structure out of this by coallescing strnodes
list = [
nil
]

test_tree(list, <<'END', "double nested string")
%{simple #{%{test}}}
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['DStrNode',0,0,0,24],
    ['StrNode',0,0,0,9],
    ['EvStrNode',0,0,9,23],
      nil, #['NewlineNode',0,0,11,23],
        ['FCallNode',0,0,11,22],
          ['ArrayNode',0,0,15,22],
            ['StrNode',0,0,15,22]
]

test_tree(list, <<'END', "double nested string")
%{simple #{foo %{test}}}
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['XStrNode',0,0,0,8]
]

test_tree(list, <<'END', "simple string")
`simple`
END

list = [
nil,
nil, #['NewlineNode',0,1,0,17],
  ['DXStrNode',0,0,0,16],
    ['StrNode',0,0,0,8],
    ['EvStrNode',0,0,8,15],
      nil, #['NewlineNode',0,0,10,16],
        ['VCallNode',0,0,10,14]
]

test_tree(list, <<'END', "simple string")
`simple #{to_s}`
END

list = [
nil,
nil, #['NewlineNode',0,1,0,18],
  ['DStrNode',0,0,0,16],
    ['StrNode',0,0,0,8],
    ['EvStrNode',0,0,8,15],
      nil, #['NewlineNode',0,0,10,16],
        ['VCallNode',0,0,10,14]
]

test_tree(list, <<'END', "eval in string")
"simple #{to_s}"
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,2,0,13]
]

test_tree(list, <<'END', "heredoc")
<<HEH
bar
HEH
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,2,0,15]
]

test_tree(list, <<'END', "heredoc quoted")
<<'HEH'
bar
HEH
END

list = [
nil,
nil, #['NewlineNode',0,1,0,8],
  ['StrNode',0,2,0,23]
]

# ENEBO: Should heredocs be different than regular strnodes since I include
#   the delineaters around the string
test_tree(list, <<'END', " dashed heredoc")
<<-HEH
bar
         HEH
END

list = [
nil,
nil, #['NewlineNode',0,3,0,22],
  ['DStrNode',0,2,0,21],
    ['StrNode',0,1,0,10],  # ENEBO: What should this really be? # MIRKO: This would be much easier for the rewriter
    ['EvStrNode',1,1,10,17],
      nil, #['NewlineNode',1,1,12,18],
        ['VCallNode',1,1,12,16],
    ['StrNode',1,2,17,21]
]

test_tree(list, <<'END', "heredoc with nested expression")
<<HEH
bar #{to_s}
HEH
END

########################################################################
#### definition tests
########################################################################

list = [
nil,
nil, #['NewlineNode', 0, 2, 0, 12],
['DefnNode', 0, 1, 0, 11],
  ['ArgumentNode', 0, 0, 4, 7],
  nil,#['ArgsNode', 0, 1, 7, 7],  
]

test_tree(list, <<'EOF', "Simple def")
def foo
end
EOF

list = [
nil,
nil, #['NewlineNode',0,2,0,18],
['DefnNode',0,1,0,16],
  ['ArgumentNode',0,0,4,7],
  ['ArgsNode',0,0,7,12],
    ['ListNode',0,0,8,11],
      ['ArgumentNode',0,0,8,11],
]

test_tree(list, <<'EOF', "Simple def with single arg list")
def foo(bar)
end
EOF

list = [
nil,
nil, #['NewlineNode',0,2,0,18],
  ['DefnNode',0,1,0,23],
    ['ArgumentNode',0,0,4,7],
    ['ArgsNode',0,0,7,19],
      ['ListNode',0,0,8,11],
        ['ArgumentNode',0,0,8,11],
      ['BlockArgNode',0,0,13,18],
]

test_tree(list, <<'EOF', "def foo(bar, &bloc) ... end")
def foo(bar, &bloc)
end
EOF

# Enebo: *rest args do not show up directly in AST?
list = [
nil,
nil, #['NewlineNode',0,2,0,18],
  ['DefnNode',0,1,0,23],
    ['ArgumentNode',0,0,4,7],
    ['ArgsNode',0,0,7,19],
      ['ListNode',0,0,8,11],
        ['ArgumentNode',0,0,8,11],
]

test_tree(list, <<'EOF', "def foo(bar, *rest) ... end")
def foo(bar, *rest)
end
EOF

list = [
nil,
nil, #['NewlineNode',0,5,0,70],
  ['ModuleNode',0,4,0,68],
    ['Colon2Node',0,0,7,10],
      nil, #['NewlineNode',3,4,64,66],
        ['DefnNode',1,3,13,64],
          ['ArgumentNode',1,1,17,21],
          ['ArgsNode',1,1,21,34],
            ['ListNode',1,1,23,32],
              ['ArgumentNode',1,1,23,26],
              ['ArgumentNode',1,1,28,32],
            nil, #['NewlineNode',2,3,39,60],
              ['FCallNode',2,2,39,58],
                ['ArrayNode',2,2,44,58],
                  ['CallNode',2,2,44,52],
                    ['LocalVarNode',2,2,44,47],
                  ['LocalVarNode',2,2,54,58]
]

test_tree(list, <<'END', "module with simple parened method")
module Abc
  def talk( arg, arg2 )
    puts arg.to_s, arg2
  end
end
END

list = [
nil,
nil, #['NewlineNode',0,5,0,56],
  ['ClassNode',0,4,0,54],
    ['Colon2Node',0,0,6,7],
      nil, #['NewlineNode',1,4,14,52],
        ['DefnNode',1,3,14,50],
          ['ArgumentNode',1,1,18,22],
          ['ArgsNode',1,1,22,35],
            ['ListNode',1,1,24,33],
              ['ArgumentNode',1,1,24,27],
              ['ArgumentNode',1,1,29,33],
            nil, #['NewlineNode',2,3,40,46],
              ['TrueNode',2,2,40,44],
    ['ConstNode',0,0,10,11]
]

test_tree(list, <<'END', "simple class inheritance")
class A < B
  def talk( arg, arg2 )
    true
  end
end
END
  
list = [
nil,
nil, #['NewlineNode',0,4,0,43],
  ['SClassNode',0,3,0,42],
    ['SelfNode',0,0,9,13],
      nil, #['NewlineNode',1,3,16,39],
        ['DefnNode',1,2,16,38],
          ['ArgumentNode',1,1,20,25],
          ['ArgsNode',1,1,25,32],
            ['ListNode',1,1,27,31],
              ['ArgumentNode',1,1,27,31],
]

test_tree(list, <<'END', "singleton class")
class << self
  def talky( arg1)
  end
end
END

########################################################################
#### symbol/variable tests
########################################################################

list = [
nil,
nil, #['NewlineNode', 0, 1, 0, 5],
['SymbolNode', 0, 0, 0, 4]
]
test_tree(list, <<'EOF', "Simple symbol")
:foo
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,6],
  ['DSymbolNode',0,0,0,6],
     ['StrNode',0,0,1,6]
]
test_tree(list, <<'EOF', "String-wrapped symbol")
:"foo"
EOF

list = [
nil,
nil, #['NewlineNode,0,1,0,16]
['Colon2Node',0,0,0,14],
  ['ConstNode',0,0,0,6]
]
test_tree(list, <<'EOF', "colon2 node")
Object::Object
EOF

list = [
nil,
nil, #['NewlineNode,0,1,0,9]
['Colon3Node',0,0,0,8]
]
test_tree(list, <<'EOF', "colon3 node")
::Object
EOF

#### Assignment

list = [
nil,
nil, #['NewlineNode,0,1,0,9]
  ['LocalAsgnNode',0,0,0,5],
    ['FixnumNode',0,0,4,5]
]
test_tree(list, <<'EOF', "simple assignment")
a = 1
EOF

list = [
nil,
nil, #['NewlineNode,0,1,0,9]
  ['LocalAsgnNode',0,0,0,5],
    ['FixnumNode',0,0,4,5]
]
# JRUBY-201 needs to be fixed before continuing here
#test_tree(list, <<'EOF', "simple colon3 assignment", true)
#::Foo = 1
#EOF

list = [
nil,
nil, #['NewlineNode,0,1,0,9]
  ['LocalAsgnNode',0,0,0,9],
    ['FCallNode',0,0,4,9],
      ['ArrayNode',0,0,7,9]
]
test_tree(list, <<'EOF', "simple assignment from method")
a = foo()
EOF

# Enebo: Splat is replacement for array or args cat node so this is ok.
list = [
nil,
nil, #['NewlineNode,0,1,0,9]
  ['LocalAsgnNode',0,0,0,13],
    ['FCallNode',0,0,4,13],
      ['SplatNode',0,0,7,13],
        ['VCallNode',0,0,9,12]
]
test_tree(list, <<'EOF', "simple assignment from method")
a = foo(*bar)
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9],
  ['DefnNode',0,0,0,25],
    ['ArgumentNode',0,0,4,5],
    ['ArgsNode',0,0,5,11],
    ['ArgumentNode',0,0,6,10],
      nil, #['NewlineNode',0,0,12,25],
        ['FCallNode',0,0,12,21],
          ['SplatNode',0,0,15,21],
            ['LocalVarNode',0,0,17,20]
]
test_tree(list, <<'EOF', "simple assignment from method")
def a(*bar) foo(*bar) end
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['LocalAsgnNode',0,0,0,22],
    ['RescueNode',0,0,10,22],
      ['RescueBodyNode',0,0,10,22],
        ['FCallNode',0,0,17,22],
          ['ArrayNode',0,0,20,22],
      ['FCallNode',0,0,4,9],
        ['ArrayNode',0,0,7,9]
]
test_tree(list, <<'EOF', "simple assignment from method")
a = foo() rescue foo()
EOF

# Fixme:  Same as next() and return() issue
list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['BeginNode',0,4,0,34],
    ['RescueNode',2,4,12,34],
      ['RescueBodyNode',2,4,12,34],
        ['ArrayNode',2,2,19,24],
          ['ConstNode',2,2,19,24],
        ['NewlineNode',3,4,27,31],
          ['VCallNode',3,3,27,30],
      ['NewlineNode',1,2,8,12],
        ['VCallNode',1,1,8,11]
]
#test_tree(list, <<'EOF', "simple assignment from method", true)
#begin
#  foo
#rescue Error
#  bar
#end
#EOF

list = [
nil,
 nil, #['NewlineNode',0,3,4,21],
  ['CaseNode',0,2,0,20],
    ['VCallNode',0,0,5,9],
    ['WhenNode',1,2,10,20],
      ['ArrayNode',1,1,15,16],
        ['FixnumNode',1,1,15,16],
]
#test_tree(list, <<'EOF', "when", true)
#case year
#when 1
#end
#EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['IfNode',0,0,0,6],
    ['FixnumNode',0,0,5,6],
    ['FixnumNode',0,0,0,1]
]
test_tree(list, <<'EOF', "1 if 2")
1 if 2
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['IfNode',0,0,0,10],
    ['FixnumNode',0,0,9,10],
    ['FixnumNode',0,0,0,1]
]
test_tree(list, <<'EOF', "1 unless 2")
1 unless 2
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['IfNode',0,1,0,9],
    ['FixnumNode',0,0,3,4]
]
test_tree(list, <<'EOF', "if 1 end")
if 1 
end
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['IfNode',0,1,0,13],
    ['FixnumNode',0,0,7,8]
]
test_tree(list, <<'EOF', "unless 1 end")
unless 1 
end
EOF

list = [
nil,
nil, #['NewlineNode',0,1,0,9]
  ['ForNode',0,1,0,14],
    ['LocalAsgnNode',0,0,4,5],
    ['VCallNode',0,0,9,10]
]
test_tree(list, <<'EOF', "for i in j end")
for i in j
end
EOF

#test calls:
list = [
nil,
nil,
['CallNode', 0, 0, 0, 10],
nil
]
test_tree(list, <<END)
String.new
END

list = [
nil,
nil,
['CallNode', 0, 0, 0, 12],
nil
]
test_tree(list, <<END)
String.new()
END

list = [
nil,
nil,
['CallNode', 0, 0, 0, 17],
nil
]
test_tree(list, <<END)
String.new("aaa")
END

list = [
nil,
nil,
['CallNode', 0, 0, 0, 24],
nil
]
test_tree(list, <<END)
String.new "aaa" + "bbb"
END

list = [
nil,
nil,
['CallNode', 0, 0, 0, 17],
nil
]
test_tree(list, <<END)
String.new 5.to_s
END

#test arrays

list = [
nil,
nil,
['CallNode', 0, 0, 0, 30],
['ConstNode', 0, 0, 0, 5],
['ArrayNode', 0, 0, 10, 30]
]
test_tree(list, <<END)
Array.new "aa", 11000, 20, 340
END

list = [
nil,
nil,
['CallNode', 0, 0, 0, 26],
]
test_tree(list, <<END)
Array.new String.new(1234)
END

list = [
nil,
nil,
['ArrayNode', 0, 0, 0, 12],
]
test_tree(list, <<END)
[55, "test"]
END

list = [
nil,
nil,
nil,
['ConstNode', 0, 0, 0, 5],
['ArrayNode', 0, 0, 9, 14],
]
test_tree(list, <<END)
Array.new("a")
END

list = [
nil,
nil,
nil,
nil,
nil,
['DAsgnNode', 0, 0, 12, 19],
nil,
nil,
]
test_tree(list, <<END, "dasgn node")
[].each do |element|
end
END

list = [
nil,
nil,
nil,
nil,
nil,
nil,
nil,
['DAsgnNode', 0, 0, 12, 20],
['DAsgnNode', 0, 0, 22, 30],
nil,
nil,
]
test_tree(list, <<END, "multiple dasgn nodes")
[].each do |element1, element2|
end
END

list = [
nil,
nil,
['MultipleAsgnNode', 0, 0, 0, 13],
['ArrayNode', 0, 0, 0, 4],
['LocalAsgnNode', 0, 0, 0, 1],
['LocalAsgnNode', 0, 0, 3, 4],
['ArrayNode', 0, 0, 7, 13],
['ZArrayNode', 0, 0, 7, 9],
['HashNode', 0, 0, 11, 13],
]
test_tree(list, <<END, "multipleasgn node")
a, b = [], {}
END

list = [
nil,
nil,
nil,
['ArrayNode', 0, 0, 0, 9],
['FixnumNode', 0, 0, 1, 2],
['FixnumNode', 0, 0, 4, 5],
['FixnumNode', 0, 0, 7, 8],
['IterNode', 0, 2, 15, 34],
['DAsgnNode', 0, 0, 19, 20],
nil,
['FCallNode', 1, 1, 24, 30],
['ArrayNode', 1, 1, 29, 30],
['DVarNode', 1, 1, 29, 30],
]
test_tree(list, <<END, "iter node")
[1, 2, 3].each do |i|
  puts i
end
END

list = [
nil,
nil, #['NewlineNode', 0, 1, 0, 7],
['LocalAsgnNode', 0, 0, 0, 6],
['CallNode', 0, 0, 0, 6],
['LocalVarNode', 0, 0, 0, 1],
['ArrayNode', 0, 0, 5, 6],
['FixnumNode', 0, 0, 5, 6]
]
test_tree(list, <<END, "+= assignment")
a += 5
END

list = [
nil,
nil, #['NewlineNode', 0, 6, 0, 70],
['ClassNode', 0, 6, 0, 70],
['Colon2Node', 0, 0, 6, 10],
nil, #['NewlineNode', 1, 6, 13, 67],
['DefnNode', 1, 5, 13, 66],
['ArgumentNode', 1, 1, 17, 21],
['ArgsNode', 2, 2, 21, 21],
nil, #['NewlineNode', 2, 5, 26, 61],
['WhileNode', 2, 4, 26, 60],
['TrueNode', 2, 2, 32, 36],
nil, #['NewlineNode', 3, 4, 43, 53],
['FCallNode', 3, 3, 43, 52],
['ArrayNode', 3, 3, 48, 52],
['TrueNode', 3, 3, 48, 52]
]
test_tree(list, <<END)
class Test
  def test
    while true
      puts true
    end
  end
end
END

list = [
nil,
nil, #['NewlineNode', 0, 4, 0, 78],
['ClassNode', 0, 4, 0, 78],
['Colon2Node', 0, 0, 6, 14],
nil, #['NewlineNode', 1, 4, 17, 75],
['DefnNode', 1, 3, 17, 73],
['ArgumentNode', 1, 1, 21, 27],
['ArgsNode', 1, 1, 28, 32],
['ListNode', 1, 1, 28, 32],
['ArgumentNode', 1, 1, 28, 29],
['ArgumentNode', 1, 1, 31, 32],
nil, #['NewlineNode', 2, 3, 38, 68],
['LocalAsgnNode', 2, 2, 38, 66],
['CallNode', 2, 2, 42, 66],
['ConstNode', 2, 2, 42, 50],
['ArrayNode', 2, 2, 62, 66],
['LocalVarNode', 2, 2, 62, 63],
['LocalVarNode', 2, 2, 65, 66]
]
test_tree(list, <<END)
class Triangle
  def printC a, b 
    c = Triangle.hypotenuse a, b 
  end 
end
END

list = [
nil,
nil, #['NewlineNode', 0, 7, 0, 91],
['ClassNode', 0, 7, 0, 90],
['Colon2Node', 0, 0, 6, 10],
['BlockNode', 1, 6, 13, 86],
nil, #['NewlineNode', 1, 4, 13, 49],
['DefsNode', 1, 3, 13, 48],
['ConstNode', 1, 1, 17, 21],
['ArgumentNode', 1, 1, 22, 26],
['ArgsNode', 1, 1, 27, 31],
['ListNode', 1, 1, 27, 31],
['ArgumentNode', 1, 1, 27, 31],
nil, #['NewlineNode', 2, 3, 36, 43],
['ReturnNode', 2, 2, 36, 42],
nil, #['NewlineNode', 1, 7, 13, 87],
['DefsNode', 4, 6, 51, 86],
['SelfNode', 4, 4, 55, 59],
['ArgumentNode', 4, 4, 60, 64],
['ArgsNode', 4, 4, 65, 69],
['ListNode', 4, 4, 65, 69],
['ArgumentNode', 4, 4, 65, 69],
nil, #['NewlineNode', 5, 6, 74, 81],
['ReturnNode', 5, 5, 74, 80]
]
test_tree(list, <<END, "class methods")
class Test
  def Test.test huhu
    return
  end
  def self.test huhu
    return
  end
end
END

list = [
nil,
nil, #['NewlineNode', 0, 4, 0, 67],
['DefnNode', 0, 4, 0, 67],
['ArgumentNode', 0, 0, 4, 20],
['ArgsNode', 1, 1, 20, 20],
['RescueNode', 1, 4, 23, 67],
['RescueBodyNode', 2, 4, 33, 67],
nil, #['NewlineNode', 3, 4, 42, 64],
['FCallNode', 3, 3, 42, 63],
['ArrayNode', 3, 3, 47, 63],
['StrNode', 3, 3, 47, 63],
nil, #['NewlineNode', 1, 2, 23, 33],
['LocalAsgnNode', 1, 1, 23, 32],
['FixnumNode', 1, 1, 31, 32]
]
test_tree(list, <<END)
def value_assertions
  value = 5
rescue
  puts "to the rescue!"
end
END

list = [
nil,
nil, #['NewlineNode', 0, 6, 0, 63],
['ClassNode', 0, 6, 0, 63],
['Colon2Node', 0, 0, 6, 7],
nil, #['NewlineNode', 1, 6, 11, 60],
['DefnNode', 1, 5, 11, 58],
['ArgumentNode', 1, 1, 15, 16],
['ArgsNode', 2, 2, 17, 17],
['BlockNode', 2, 4, 22, 52],
nil, #['NewlineNode', 2, 3, 22, 31],
['YieldNode', 2, 2, 22, 30],
['FixnumNode', 2, 2, 28, 29],
nil, #['NewlineNode', 2, 4, 22, 43],
['YieldNode', 3, 3, 35, 42],
nil, #['NewlineNode', 2, 5, 22, 53],
['YieldNode', 4, 4, 47, 52]
]
test_tree(list, <<END, "yield")
class X 
  def x 
    yield(5)
    yield()
    yield
  end 
end
END


list = [
nil,
nil, #['NewlineNode', 0, 1, 0, 25],
['CallNode', 0, 0, 0, 24],
['FixnumNode', 0, 0, 0, 1],
['IterNode', 0, 0, 8, 24],
nil, #['NewlineNode', 0, 0, 10, 24],
['FCallNode', 0, 0, 10, 22],
['ArrayNode', 0, 0, 15, 22],
['StrNode', 0, 0, 15, 22],
]
test_tree(list, <<END)
5.times { puts "hello" }
END


list = [
nil,
nil, #['NewlineNode', 0, 4, 0, 44],
['ClassNode', 0, 4, 0, 44],
['Colon2Node', 0, 0, 6, 7],
nil, #['NewlineNode', 1, 4, 10, 41],
['DefnNode', 1, 3, 10, 40],
['ArgumentNode', 1, 1, 14, 24],
['ArgsNode', 2, 2, 24, 24],
nil, #['NewlineNode', 2, 3, 29, 35],
['ZSuperNode', 2, 2, 29, 34]
]
test_tree(list, <<END, "zsuper")
class X
  def initialize
    super
  end
end
END


list = [
nil,
nil, #['NewlineNode', 0, 2, 0, 35],
['DefnNode', 0, 1, 0, 34],
['ArgumentNode', 0, 0, 4, 8],
['ArgsNode', 0, 0, 9, 30],
['ListNode', 0, 0, 9, 16],
['ArgumentNode', 0, 0, 9, 10],
['ArgumentNode', 0, 0, 12, 13],
['ArgumentNode', 0, 0, 15, 16],
['ArgumentNode', 0, 0, 18, 22],
['BlockArgNode', 0, 0, 24, 30],
nil, #['ScopeNode', 1, 1, 33, 34]
]
test_tree(list, <<END, "method with opt and block arg")
def test a, b, c, *opt, &block
end
END

list = [
nil,
nil,
['LocalAsgnNode', 0, 0, 0, 15],
['CallNode', 0, 0, 4, 15],
['FixnumNode', 0, 0, 4, 5],
['ArrayNode', 0, 0, 9, 15],
nil,
['CallNode', 0, 0, 9, 14],
['FixnumNode', 0, 0, 9, 10],
['ArrayNode', 0, 0, 13, 14],
['FixnumNode', 0, 0, 13, 14]
]
test_tree(list, <<END)
v = 2 * (1 + 3)
END

list = [
nil,
['BlockNode', 0, 6, 0, 141],
nil,
['LocalAsgnNode', 0, 0, 0, 11],
['FixnumNode', 0, 0, 7, 11],
nil,
['LocalAsgnNode', 1, 5, 12, 131],
['CaseNode', 1, 5, 19, 131],
['WhenNode', 2, 2, 31, 57],
['ArrayNode', 2, 2, 36, 51],
['CallNode', 2, 2, 36, 51],
['CallNode', 2, 2, 36, 46],
['LocalVarNode', 2, 2, 36, 40],
['ArrayNode', 2, 2, 43, 46],
['FixnumNode', 2, 2, 43, 46],
['ArrayNode', 2, 2, 50, 51],
['FixnumNode', 2, 2, 50, 51],
nil,
['TrueNode', 2, 2, 53, 57],
['WhenNode', 3, 3, 65, 92],
['ArrayNode', 3, 3, 70, 85],
['CallNode', 3, 3, 70, 85],
['CallNode', 3, 3, 70, 80],
['LocalVarNode', 3, 3, 70, 74],
['ArrayNode', 3, 3, 77, 80],
['FixnumNode', 3, 3, 77, 80],
['ArrayNode', 3, 3, 84, 85],
['FixnumNode', 3, 3, 84, 85],
nil,
['FalseNode', 3, 3, 87, 92],
nil,
['CallNode', 4, 4, 105, 120],
['CallNode', 4, 4, 105, 113],
['LocalVarNode', 4, 4, 105, 109],
['ArrayNode', 4, 4, 112, 113],
['FixnumNode', 4, 4, 112, 113],
['ArrayNode', 4, 4, 119, 120],
['FixnumNode', 4, 4, 119, 120],
nil,
['FCallNode', 6, 6, 132, 141],
['ArrayNode', 6, 6, 137, 141],
['LocalVarNode', 6, 6, 137, 141]
]
test_tree(list, <<END)
year = 2007
leap = case
       when year % 400 == 0: true
       when year % 100 == 0: false
       else year % 4   == 0
       end
puts leap
END


list = [
nil,
nil,
['CallNode', 0, 0, 0, 28],
['FixnumNode', 0, 0, 0, 1],
['IterNode', 0, 0, 8, 28],
['DAsgnNode', 0, 0, 11, 12],
['BlockNode', 0, 0, 14, 27],
nil,
['FCallNode', 0, 0, 14, 20],
['ArrayNode', 0, 0, 19, 20],
['DVarNode', 0, 0, 19, 20],
nil,
['BreakNode', 0, 0, 22, 27]
]
test_tree(list, <<END)
3.times { |i| puts i; break}
END

list = [
nil,
nil,
['FCallNode', 0, 0, 0, 29],
['ArrayNode', 0, 0, 5, 8],
['FixnumNode', 0, 0, 6, 7],
['IterNode', 0, 0, 9, 29],
['DAsgnNode', 0, 0, 12, 13],
['BlockNode', 0, 0, 15, 28],
nil,
['FCallNode', 0, 0, 15, 21],
['ArrayNode', 0, 0, 20, 21],
['DVarNode', 0, 0, 20, 21],
nil,
['BreakNode', 0, 0, 23, 28]
]
test_tree(list, <<END)
times(1) { |i| puts i; break}
END

list = [
nil,
nil,
['CallNode', 0, 0, 0, 31],
['FixnumNode', 0, 0, 0, 1],
['ArrayNode', 0, 0, 7, 10],
['FixnumNode', 0, 0, 8, 9],
['IterNode', 0, 0, 11, 31],
['DAsgnNode', 0, 0, 14, 15],
['BlockNode', 0, 0, 17, 30],
nil,
['FCallNode', 0, 0, 17, 23],
['ArrayNode', 0, 0, 22, 23],
['DVarNode', 0, 0, 22, 23],
nil,
['BreakNode', 0, 0, 25, 30]
]
test_tree(list, <<END)
3.times(1) { |i| puts i; break}
END

list = [
nil,
['BlockNode', 0, 2, 0, 13],
nil,
['FCallNode', 0, 0, 0, 4],
['BlockPassNode', 0, 0, 2, 4],
['VCallNode', 0, 0, 3, 4],
nil,
['VCallNode', 2, 2, 6, 13]
]
test_tree(list, <<END)
a &i

nothing
END

list = [
nil,
['BlockNode', 0, 1, 0, 26],
nil,
['OpAsgnOrNode', 0, 0, 0, 12],
['LocalVarNode', 0, 0, 0, 3],
['LocalAsgnNode', 0, 0, 0, 3],
['TrueNode', 0, 0, 8, 12],
nil,
['OpAsgnAndNode', 1, 1, 13, 26],
['LocalVarNode', 1, 1, 13, 16],
['LocalAsgnNode', 1, 1, 13, 16],
['FalseNode', 1, 1, 21, 26]
]
test_tree(list, <<END)
var ||= true
var &&= false
END

list = [
nil,
nil,
['DefnNode', 0, 1, 0, 10],
['ArgumentNode', 0, 0, 4, 6],
['ArgsNode', 1, 1, 6, 6]
]
test_tree(list, <<END)
def []
end
END
