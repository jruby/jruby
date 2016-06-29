class PostProcessor
  def initialize(source, out=STDOUT)
    @out = out
    @lines = File.readlines(source)
    @index = -1
    @case_bodies = {}
    @max_case_number = -1
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

  def translate
    while (line = read)
      if line =~ %r{^//\s*ACTIONS_BEGIN}
        translate_actions
      elsif line =~ %r{^//\s*ACTION_BODIES}
        generate_action_body_methods
      else
        @out.puts line
      end
    end
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
    @out.puts "states[#{state}] = new RipperParserState() {"
    @out.puts "  @Override public Object execute(RipperParser p, Object yyVal, Object[] yyVals, int yyTop) {"
    code_body.each { |line| @out.puts line }
    @out.puts "    return yyVal;"
    @out.puts "  }"
    @out.puts "};"
  end

  def generate_action_body_method(state, code_body)
    @out.puts "states[#{state}] = new ParserState() {"
    @out.puts "  @Override public Object execute(ParserSupport support, RubyLexer lexer, Object yyVal, Object[] yyVals, int yyTop) {"
    code_body.each { |line| @out.puts line }
    @out.puts "    return yyVal;"
    @out.puts "  }"
    @out.puts "};"
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
    line = read
    return false if end_of_actions?(line) || line !~ /case\s+(\d+):/
    case_number = $1.to_i

    line = read
    return false if line !~ /line\s+(\d+)/
    line_number = $1

    # Extra boiler plate '{' that we do not need
    line = read
    return false if line !~ /^\s*\{\s*(\/\*.*\*\/)?$/

    @max_case_number = case_number if case_number > @max_case_number

    label = "case#{case_number}_line#{line_number}"

    body = []
    last_line = nil
    while (line = read)
      if line =~ /^\s*\}\s*$/ # Extra trailing boiler plate
        next_line = read
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

if ARGV[0] =~ /Ripper/
  RIPPER = true
else
  RIPPER = false
end

PostProcessor.new(ARGV.shift).translate
