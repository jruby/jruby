require 'java'
require 'parser.rb'
require 'javacode.rb'
require 'jruby.rb'
require 'statement.rb'
require 'expression.rb'
require 'simple_expression.rb'
require 'helper.rb'

#
# Parse file
#
node = JRuby::Parser::parse <<EOF
if true
  puts(false ? 'Error' : 'Hello World')
else
  puts 'Error'
end
EOF

#
# Create class and methods
#
script_class = JavaCode::JClass.new("RubyScript")

main_method = JavaCode::JMethod.new("main")
main_method.flags << JavaCode::STATIC
main_method.parameter << "String[] args"
main_method << "Ruby runtime = Ruby.getDefaultInstance();\n"
main_method << "load(runtime, runtime.getTopSelf());"

script_class << main_method

load_method = JavaCode::JMethod.new("load")
load_method.flags << JavaCode::STATIC
load_method.parameter << "Ruby runtime, IRubyObject self"

script_class << load_method

$> = load_method
$method = load_method
$class = script_class
$methods = []
$classes = []

#
# generate code
#
$statement.acceptNode(node)

#
# print code
#
$> = $stdout
puts script_class
