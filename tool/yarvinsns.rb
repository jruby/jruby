
# Run this file to regenerate all files automatically created
# from instruction information.
#
# The parameters provided points to Ruby trunk defs files
#
# ex: 
# ruby yarvinsns.rb ~/src/ruby-trunk/insns.def ~/src/ruby-trunk/opt_insn_unif.def ~/src/ruby-trunk/opt_operand.def

insnsFile = ARGV[0]
uniFile = ARGV[1]
operandFile = ARGV[2]

defs = []
in_def = false
cur_def = ""
open(insnsFile) do |f|
  f.each_line do |l|
    if !in_def && /^DEFINE_INSN$/ =~ l
      in_def = true
    else
      if in_def && /^\{$/ =~ l
        in_def = false
        defs << cur_def
        cur_def = ""
      end
      if in_def
        cur_def << l
      end
    end
  end
end

$typeMappings = {
  'ISEQ' => 'YARVMachine.InstructionSequence',
  'dindex_t' => 'long',
  'lindex_t' => 'long',
  'num_t' => 'long',
  'OFFSET' => 'long',
  'CDHASH' => 'CDHASH???',
  'IC' => 'IC???',
  'GENTRY' => 'GENTRY???',
  'ID' => 'ID???',
  'VALUE' => 'IRubyObject',
  '...' => :any,
}

class Value
  attr_accessor :type, :name
  def initialize(type, name='any')
    @type, @name = $typeMappings[type], name
  end
end

class Instruction
  attr_accessor :name, :ops, :pops, :rets
end


instructions = []

def get_instruction(str)
  i = Instruction.new
  sarr = str.split(/\n/)
  i.name = sarr[0]
  i.ops = sarr[1][/\(.*?\)/][1..-2].split(/,/).map {|v| Value.new *v.strip.split(/ +/)}
  i.pops = sarr[2][/\(.*?\)/][1..-2].split(/,/).map {|v| Value.new *v.strip.split(/ +/)}
  i.rets = sarr[3][/\(.*?\)/][1..-2].split(/,/).map {|v| Value.new *v.strip.split(/ +/)}
  i
end

for idef in defs
  instructions << get_instruction(idef)
end

def reformat(v)
  if "*" == v
    '_wc_'
  elsif /^int2fix\((.*)\)$/ =~ v
    "int2fix_0_#{$1}_c_"
  else
    v
  end
end

open(operandFile) do |f|
  f.each do |l|
    if /^#|__END__|^$/ =~ l.strip
      next
    end
    i = Instruction.new
    nm = l[/^[^ ]+/]
    rest = l[nm.length+1..-1].strip
    i.name = nm + "_op_" + rest.split(/, /).map {|n| reformat(n.downcase) }.join('_')
    i.ops = []
    i.pops = []
    i.rets = []
    instructions << i
  end
end

open(uniFile) do |f|
  f.each do |l|
    if /^#|__END__|^$/ =~ l.strip
      next
    end
    i = Instruction.new
    i.name = "unified_#{l.strip.split(/ +/).join('_')}"
    i.ops = []
    i.pops = []
    i.rets = []
    instructions << i
  end
end

INSTRUCTIONS = instructions

b = binding

require 'erb'

Dir["**/*.template"].each do |file|
  $stderr.puts "Processing #{file}"
  f = ERB.new(File.read(file))
  File.open(file[0..-10],"w") do |of|
    of.write(f.result(b))
  end
end
