# From MRI ext/ripper/tools/dsl.rb

# Simple DSL implementation for Ripper code generation
#
# input: /*% ripper: stmts_add!(stmts_new, void_stmt) %*/
# output:
#   VALUE v1, v2;
#   v1 = dispatch0(stmts_new);
#   v2 = dispatch0(void_stmt);
#   $$ = dispatch2(stmts_add, v1, v2);

$dollar = "$$"
alias $$ $dollar

Qnil = 'p.nil()'
$__1__ = '$1'
$__2__ = '$2'
$__3__ = '$3'
$__4__ = '$4'
$__5__ = '$5'
$__6__ = '$6'
$__7__ = '$7'
$__8__ = '$8'
$__9__ = '$9'

class DSL
  def initialize(code, options, type: "IRubyObject", supports_overloads: false)
    @events = {}
    @error = options.include?("error")
    @brace = options.include?("brace")
    if options.include?("final")
      @final = "$$"  # MRI has special need here but we don't
    else
      @final = (options.grep(/\A\$(?:\$|\d+)\z/)[0] || "$$")
    end
    @vars = 0

    # create $1 == "$1", $2 == "$2", ...
    s = (1..20).map {|n| "$#{n}"}
    re = Array.new(s.size, "([^\0]+)")
    /#{re.join("\0")}/ =~ s.join("\0")

    # struct parser_params *p
    p = p = "p"

    @code = ""
    @type = type
    @supports_overloads = supports_overloads

    code.gsub!(/\$:?(\d+)/, '$__\1__')

    @last_value = eval(code)
#    $stderr.puts("LAST VALUE: #{@last_value}")
  end

  attr_reader :events

  undef lambda
  undef hash
  undef class

  INDENT = "                    "

  def generate(indent = INDENT)
    s = ""
    s << "#{@type} #{(1..@vars).map {|v| %Q{v#{v}} }.join(', ')};\n" if @vars > 0
    s << @code
    s << "#{indent}#@final = #{@last_value};"
    s << "\n#{indent}p.error();" if @error
    s = "{#{ s }}" #if @brace
    s
  end

  def new_var
    "v#{ @vars += 1 }"
  end

  def opt_event(event, default, addend)
    add_event(event, [default, addend], true)
  end

  TRANSLATIONS = {
    'Qnil' => 'p.nil()',
    '0' => 'null',
    'ERR_MESG()' => 'p.intern(message)',
    'id_assoc' => 'EQ_GT',
    'idOr' => 'OR',
  }

  def id_value(value)
    if value == "idCOLON2"
      '"::"'
    elsif value == "idCall"
      '"call"'
    elsif value == "idUMinus"
      '"-@"'
    elsif value == "idNOT"
      '"!"'
    else
      raise ArgumentError "Unknown id value: #{value}"
    end
  end
  
  def translate_arg(arg)
    arg = arg.to_s
    replacement = TRANSLATIONS[arg]

    return replacement if replacement

    if arg =~ /^escape_Qundef\((.*)\)/
      "p.escape(#{$1})"
    elsif arg =~ /^assignable\(p, ([^\)]+)\)/
      "p.assignable(#{translate_arg(two)})"
    elsif arg =~ /^rb_assoc_new\(([^,]+), ([^\)]+)\)/
      two = $2
      two = "null" if two == "0"
      "p.new_assoc(#{translate_arg($1)}, #{translate_arg($2)})"
    elsif arg =~ /^rb_ary_new3\(1, ([^\)]+)\)/
      "p.new_array(#{translate_arg($1)})"
    elsif arg =~ /^rb_ary_push\($1, ([^\)]+)\)/
      "$1.push(p.getContext(), #{translate_arg($1)});"
    elsif arg =~ /^rb_ary_concat\($1, ([^\)]+)\)/
      "$1.push(p.getContext(), #{translate_arg($1)});"
    elsif arg =~ /^rb_ary_new_from_args\(1, ([^\)]+)\)/
      "p.new_array(#{translate_arg($1)})"
    elsif arg =~ /^rb_ary_new_from_args\(2, ([^,]+), ([^\)]+)\)/
      "p.new_array(#{translate_arg($1)},  #{translate_arg($2)})"
    elsif arg =~ /^get_value\(([^\)]+)\)/
      "p.get_value(#{translate_arg($1)})"
    elsif arg =~ /^var_field\(p, ([^\)]+)\)/
      "p.dispatch($1)"
    elsif arg =~ /^ID2VAL\((.*)\)/
      "p.intern(#{id_value($1)})"
    elsif arg =~ /^AREF\((.*), (.*)\)/
      # This is super fragile and assumes only AREF is for singleton def{s,n}
      # singleton(0), dot_or_colon(1), name(2)
      if $2 == "0"
        "$<IRubyObject>1.singleton"
      elsif $2 == "1"
        "p.intern($1.dotOrColon)"
      elsif $2 == "2"
        "$1.name"
      end
    else
      arg
    end
  end

  def add_event(event, args, qundef_check = false, indent = INDENT)
#    $stderr.puts "ARGS #{args.inspect}"
    event = event.to_s.sub(/!\z/, "")
    @events[event] = args.size
    vars = args.map do |arg|
      new_var.tap do |v|
#    $stderr.puts "ARG: #{arg}, TRANS: #{translate_arg(arg)}"
        @code << "#{indent}#{v} = #{translate_arg(arg)};\n"
      end
    end
    v = new_var
    d = dispatch(event, vars)
    d = "#{vars.last} == null ? #{vars.first} : #{d}" if qundef_check
    @code << "#{indent}#{v} = #{d};\n"
    v
  end

  def dispatch(event, vars)
    event = %Q{"on_#{event}"}
    
    if @supports_overloads
      "p.dispatch(#{ [event, *vars].join(", ") })"
    else
      "p.dispatch(#{ [event, *vars].join(", ") })"
    end
  end

  def method_missing(event, *args)
    event = event.to_s
#    $stderr.puts "AAAARGS: #{args.join(',')}"

    replacement = TRANSLATIONS[event]

    return replacement if replacement

    if event =~ /!\z/
#      $stderr.puts("EVENT: #{event} LEN: #{args.size}, ARGS: #{args.join(',')}")
      add_event(event, args)
    elsif args.empty? and /\Aid[A-Z_]/ =~ event
      event
    elsif args.length > 0 && args[0] == "p"
      "p.#{event}(#{args[1..-1].map { |e|  e == 0 ? "null" : e }.join(', ')})"
    elsif event == 'RNODE'
      '$1'
    elsif event == 'STATIC_ID2SYM'
      "p.symbolID(#{translate_arg(args[0])})"
    elsif event == 'get_value'
      "p.get_value(#{translate_arg(args[0])})"
    elsif event == 'backref_error'
      "p.backref_err(#{translate_arg(args[1])}, #{translate_arg(args[2])})"
    elsif event == 'rb_ary_new3'
      "p.new_array(#{translate_arg(args[1])})"
    elsif event == 'rb_ary_push' || event == 'rb_ary_concat'
      "#{args[0]}.push(p.getContext(), #{translate_arg(args[1])});"
    elsif event == 'rb_assoc_new'
      two = args[1]
      two = "null" if two == "0"
      "p.new_assoc(#{translate_arg(args[0])}, #{translate_arg(args[1])})"
    elsif event == 'escape_Qundef'
      "p.escape(#{args[0]})"
    elsif event == 'assignable'
      "p.assignable(#{translate_arg(args[1])})"
    elsif event == 'rb_ary_new_from_args'
      if args[0] == 1
        "p.new_array(#{translate_arg(args[1])})"
      elsif args[0] == 2
        "p.new_array(#{translate_arg(args[1])},  #{translate_arg(args[2])})"
      else
        "ERRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR NEW_FROM_ARGS"
      end
    elsif event == 'var_field'
      "p.dispatch(args[0])"
    elsif event == 'ID2VAL'
      "p.intern(#{id_value(args[0])})"
    elsif event == 'AREF'
      "((RubyArray) $1.value).eltOk(#{args[1]})"
    else
      "#{event}(#{args.join(', ')})"
    end
  end

  def self.const_missing(name)
    name
  end
end

