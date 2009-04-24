require "rubygems"
require "rbench"
 
class Array
  alias_method :concat, :<<
end
 
require 'erubis'
module Erubis
  # This adds support for embedding the return value of a block call:
  #   <%= foo do %>...<% end =%>
  #
  # :api: private
  module Basic::Converter
    def convert_input(src, input)
      pat = @pattern
      regexp = pat.nil? || pat == '<% %>' ? DEFAULT_REGEXP : pattern_regexp(pat)
      pos = 0
      is_bol = true     # is beginning of line
      input.scan(regexp) do |indicator, code, tailch, rspace|
        match = Regexp.last_match()
        len  = match.begin(0) - pos
        text = input[pos, len]
        pos  = match.end(0)
        ch   = indicator ? indicator[0] : nil
        lspace = ch == ?= ? nil : detect_spaces_at_bol(text, is_bol)
        is_bol = rspace ? true : false
        add_text(src, text) if text && !text.empty?
        ## * when '<%= %>', do nothing
        ## * when '<% %>' or '<%# %>', delete spaces iff only spaces are around '<% %>'
        if ch == ?=              # <%= %>
          rspace = nil if tailch && !tailch.empty?
          add_text(src, lspace) if lspace
          add_expr(src, code, indicator)
          add_text(src, rspace) if rspace
        elsif ch == ?\#          # <%# %>
          n = code.count("\n") + (rspace ? 1 : 0)
          if @trim && lspace && rspace
            add_stmt(src, "\n" * n)
          else
            add_text(src, lspace) if lspace
            add_stmt(src, "\n" * n)
            add_text(src, rspace) if rspace
          end
        elsif ch == ?%           # <%% %>
          s = "#{lspace}#{@prefix||='<%'}#{code}#{tailch}#{@postfix||='%>'}#{rspace}"
          add_text(src, s)
        else                     # <% %>
          if @trim && lspace && rspace
            if respond_to?(:add_stmt2)
              add_stmt2(src, "#{lspace}#{code}#{rspace}", tailch)
            else
              add_stmt(src, "#{lspace}#{code}#{rspace}")
            end
          else
            add_text(src, lspace) if lspace
            if respond_to?(:add_stmt2)
              add_stmt2(src, code, tailch)
            else
              add_stmt(src, code)
            end
            add_text(src, rspace) if rspace
          end
        end
      end
      #rest = $' || input                        # ruby1.8
      rest = pos == 0 ? input : input[pos..-1]   # ruby1.9
      add_text(src, rest)
    end
    
  end
  
  # Loads a file, runs it through Erubis and parses it as YAML.
  #
  # ===== Parameters
  # file<String>:: The name of the file to load.
  # binding<Binding>::
  #   The binding to use when evaluating the ERB tags. Defaults to the current
  #   binding.
  #
  # :api: private
  def self.load_yaml_file(file, binding = binding)
    YAML::load(Erubis::MEruby.new(IO.read(File.expand_path(file))).result(binding))
  end
end
 
module Erubis
  module BlockAwareEnhancer
    # :api: private
    def add_preamble(src)
      src << "_old_buf, @_erb_buf = @_erb_buf, ''; "
      src << "@_engine = 'erb'; "
    end
 
    # :api: private
    def add_postamble(src)
      src << "\n" unless src[-1] == ?\n      
      src << "_ret = @_erb_buf; @_erb_buf = _old_buf; _ret.to_s;\n"
    end
 
    # :api: private
    def add_text(src, text)
      src << " @_erb_buf.concat('" << escape_text(text) << "'); "
    end
 
    # :api: private
    def add_expr_escaped(src, code)
      src << ' @_erb_buf.concat(' << escaped_expr(code) << ');'
    end
    
    # :api: private
    def add_stmt2(src, code, tailch)
      src << code
      src << ") ).to_s; " if tailch == "="
      src << ';' unless code[-1] == ?\n
    end
    
    # :api: private
    def add_expr_literal(src, code)
      if code =~ /(do|\{)(\s*\|[^|]*\|)?\s*\Z/
        src << ' @_erb_buf.concat( (' << code << "; "
      else
        src << ' @_erb_buf.concat((' << code << ').to_s);'
      end
    end
  end
 
  class BlockAwareEruby < Eruby
    include BlockAwareEnhancer
  end
 
  class PercentEruby < Erubis::Eruby
    include PercentLineEnhancer
    # include StringBufferEnhancer
    include BlockAwareEnhancer
  end  
 
  class StringBufferEruby < Erubis::Eruby
    include StringBufferEnhancer
    include BlockAwareEnhancer
  end
  
  class InterpolationEruby < Erubis::Eruby
    include InterpolationEnhancer
    include BlockAwareEnhancer
  end  
end
 
class Erubis::ArrayEruby < Erubis::BlockAwareEruby
  def add_preamble(src)
    src << "_old_buf, @_erb_buf = @_erb_buf, []; "
    src << "@_engine = 'erb'; "
  end
 
  # :api: private
  def add_postamble(src)
    src << "\n" unless src[-1] == ?\n      
    src << "_ret = @_erb_buf; @_erb_buf = _old_buf; _ret.join;\n"
  end
end
 
class Context
  def initialize
    @hello = "Hello world"
  end
  
  def helper1
    "Hello!"
  end
  
  def helper2
    old, @_erb_buf = @_erb_buf, ""
    yield
    ret = @_erb_buf
    @_erb_buf = old
    "<div>#{ret}  </div>\n"
  end
end
 
text = [__LINE__ + 1, <<-HTML]
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
  "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-type" content="text/html; charset=utf-8">
  <title>ERB</title>
  
</head>
<body>
  Text only!
</body>
</html>
HTML
 
basic = [__LINE__ + 1, <<-HTML]
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
  "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-type" content="text/html; charset=utf-8">
  <title>ERB</title>
  
</head>
<body>
  <%= @hello %>
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
</body>
</html>
HTML
 
blocks = [__LINE__ + 1, <<-HTML]
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
  "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-type" content="text/html; charset=utf-8">
  <title>ERB</title>
  
</head>
<body>
  <%= @hello %>
  <%= helper1 %>
  <%= helper2 do %>
    Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    <%= helper1 %>
    Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <% end =%>
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  <%= helper1 %>
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.  
</body>
</html>
HTML
 
def define_template(name, template, line)
  begin
    code = "def #{name}(_locals={}); #{template.src}; end"
  rescue => e
    puts code
    raise
  end
  Context.module_eval code, __FILE__, line
end
 
KINDS = %w(BlockAware Array Percent StringBuffer)
 
KINDS.each do |kind|
  define_template("text_#{kind}", Erubis.const_get("#{kind}Eruby").new(text[1]), text[0])
  define_template("basic_#{kind}", Erubis.const_get("#{kind}Eruby").new(basic[1]), basic[0])
  define_template("blocks_#{kind}", Erubis.const_get("#{kind}Eruby").new(blocks[1]), blocks[0])
end
 
puts Context.new.blocks_Array
 
(ARGV[0] || 1).to_i.times do
RBench.run(100_000) do
  column :BlockAware
  column :Array
  column :Percent
  column :StringBuffer
    
  report("text") do
    KINDS.each do |k|
      eval "#{k} { Context.new.text_#{k} }"
    end
  end
 
  report("basic") do
    KINDS.each do |k|
      eval "#{k} { Context.new.basic_#{k} }"
    end
  end
 
  report("blocks") do
    KINDS.each do |k|
      eval "#{k} { Context.new.blocks_#{k} }"
    end
  end  
end
end
