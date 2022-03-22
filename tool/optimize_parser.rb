class PostProcessor
  def initialize(source, is_parser = true, out=STDOUT)
    @out = out
    @lines = File.readlines(source)
    @index = -1
    @case_bodies = {}
    @max_case_number = -1
    @sub_type_index = is_parser ? 0 : 1
  end

  # Read/Unread with ability to push back one line for a single lookahead
  def read
    @index += 1
    line = @last ? @last : @lines[@index]
    @last = nil
    line
  end

  def unread(line)
    @index -= 1
    @last = line
  end

  def end_of_actions?(line)
     return line =~ %r{^//\s*ACTIONS_END}
  end

  def read_line
    read
  end

  def translate
    while line = read_line
      if line =~ %r{^/\*@@=}
        read_text_substitutions
      elsif line =~ %r{^//\s*ACTIONS_BEGIN}
        translate_actions
      elsif line =~ %r{^//\s*ACTION_BODIES}
        generate_action_body_methods
      else
        @out.puts line
      end
    end
  end

  # We define substitutions at the top of the file where a constant
  # named SUBS contains a key which represents a subtition and two value
  # where the first value is what is substituted when writing the Parser
  # and the second value is what is substituted when writing Ripper Parser.
  #
  # Any reference to @@name@@ will be replaced with first or second value
  # later on in the grammar file.
  def read_text_substitutions
    code = ''
    while line = read_line
      break if line =~ %r{^=@@\*/}
      code << line
    end
    # Reads in substitions into the constant SUBS
    eval code
  end

  def generate_action_body_methods
    if RIPPER
      @out.puts "static RipperParserState[] states = new RipperParserState[#{@max_case_number+1}];"
    else
      @out.puts "static ParserState[] states = new ParserState[#{@max_case_number+1}];"
    end
    @out.puts "static {";
    @case_bodies.each do |state, code_body| 
      if RIPPER
        generate_ripper_action_body_method(state, code_body) 
      else
        generate_action_body_method(state, code_body) 
      end
    end
    @out.puts "}";
  end

  def generate_ripper_action_body_method(state, code_body)
    @out.puts "states[#{state}] = (RipperParser p, Object yyVal, Object[] yyVals, int yyTop, int yychar) -> {" 
    code_body.each { |line| @out.puts frob_yyVals(line) }
    @out.puts "  return yyVal;"
    @out.puts "};"
  end

  def generate_action_body_method(state, code_body)
    @out.puts "states[#{state}] = (RubyParser p, Object yyVal, ProductionState[] yyVals, int yyTop, int count, int yychar) -> {" 
    code_body.each { |line| @out.puts frob_yyVals(line) }
    @out.puts "  return yyVal;"
    @out.puts "};"
  end

  # @{num} allows us to get direct access to the production state for that
  # production or token.  This is used for specialized reporting in syntax
  # error messaged where we want to highlight a specific region.
  def frob_yyVals(line)
    line
      .gsub(/yyVals\[([^\]]+)\]/, 'yyVals[\1].value')
      .gsub(/@(\d+)/, 'yyVals[yyTop - count + \1]')
  end

  def translate_actions
    count = 1
    while (translate_action)
      count += 1
    end
  end

  # Assumptions:
  # 1. no break; in our code.  A bit weak, but this is highly specialized code.
  # 2. All productions will have a line containing only { (with optional comment)
  # 3. All productions will end with a line containly only } followed by break in ass 1.
  def translate_action
    line = read_line
    return false if end_of_actions?(line) || line !~ /case\s+(\d+):/
    case_number = $1.to_i

    line = read_line
    return false if line !~ /line\s+(\d+)/
    line_number = $1

    # Extra boiler plate '{' that we do not need
    line = read_line
    return false if line !~ /^\s*\{\s*(\/\*.*\*\/)?$/

    @max_case_number = case_number if case_number > @max_case_number

    label = "case#{case_number}_line#{line_number}"

    body = []
    last_line = nil
    while (line = read_line)
      if line =~ /^\s*\}\s*$/ # Extra trailing boiler plate
        next_line = read_line
        if next_line =~ /break;/
          break
        else
          body << line
          unread next_line
        end
      else
        body << line
      end
    end

    @case_bodies[case_number] = body
    true
  end
end

if ARGV[0] =~ /(ripper_|Ripper)/
  RIPPER = true
else
  RIPPER = false
end
$stderr.puts "RIPPER: #{RIPPER}"

PostProcessor.new(ARGV.shift, !RIPPER).translate
