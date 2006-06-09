# Emitter expects events obeying the following grammar:
# stream ::= STREAM-START document* STREAM-END
# document ::= DOCUMENT-START node DOCUMENT-END
# node ::= SCALAR | sequence | mapping
# sequence ::= SEQUENCE-START node* SEQUENCE-END
# mapping ::= MAPPING-START (node node)* MAPPING-END

require 'rbyaml/error'
require 'rbyaml/events'

module RbYAML
  class EmitterError < YAMLError
  end

  ScalarAnalysis = Struct.new(:scalar,:empty,:multiline,:allow_flow_plain,:allow_block_plain,:allow_single_quoted,:allow_double_quoted,:allow_block)

  class Emitter
    DEFAULT_TAG_PREFIXES = {
      "!" => "!",
      "tag:yaml.org,2002:" => "!!"
    }

    def initialize(stream, canonical=nil, indent=nil, width=nil,line_break=nil)
      # The stream should have the methods `write` and possibly `flush`.
      @stream = stream

      # Emitter is a state machine with a stack of states to handle nested
      # structures.
      @states = []
      @state = :expect_stream_start

      # Current event and the event queue.
      @events = []
      @event = nil

      # The current indentation level and the stack of previous indents.
      @indents = []
      @indent = nil

      # Flow level.
      @flow_level = 0

      # Contexts.
      @root_context = false
      @sequence_context = false
      @mapping_context = false
      @simple_key_context = false

      # Characteristics of the last emitted character:
      #  - current position.
      #  - is it a whitespace?
      #  - is it an indention character
      #    (indentation space, '-', '?', or ':')?
      @line = 0
      @column = 0
      @whitespace = true
      @indention = true

      # Formatting details.
      @canonical = canonical
      @best_indent = 2
      @best_indent = indent if indent && indent!=0 && 1 < indent && indent < 10

      @best_width = 80
      @best_width = width if width && width != 0 && width > @best_indent*2
      
      @best_line_break = "\n"
      @best_line_break = line_break if ["\r", "\n", "\r\n"].include?(line_break)

      # Tag prefixes.
      @tag_prefixes = nil

      # Prepared anchor and tag.
      @prepared_anchor = nil
      @prepared_tag = nil

      # Scalar analysis and style.
      @analysis = nil
      @style = nil
    end

    def emit(event)
      @events << event
      while !need_more_events
        @event = @events.shift
        send(@state)
        @event = nil
      end
    end

    # In some cases, we wait for a few next events before emitting.

    def need_more_events
      return true if @events.empty?
      event = @events.first
      if DocumentStartEvent === event
        need_events(1)
      elsif SequenceStartEvent === event
        need_events(2)
      elsif MappingStartEvent === event
        need_events(3)
      else
        false
      end
    end

    def need_events(count)
      level = 0
      for event in @events[1..-1]
        if DocumentStartEvent === event || CollectionStartEvent === event
          level += 1
        elsif DocumentEndEvent === event || CollectionEndEvent === event
          level -= 1
        elsif StreamEndEvent === event
          level = -1
        end
        if level < 0
          return false
        end
      end
      @events.length < count+1
    end

    def increase_indent(flow=false, indentless=false)
      @indents << @indent
      if @indent.nil?
        if flow
          @indent = @best_indent
        else
          @indent = 0
        end
      elsif !indentless
        @indent += @best_indent
      end
    end

    # States.

    # Stream handlers.

    def expect_stream_start
      if StreamStartEvent === @event
        write_stream_start
        @state = :expect_first_document_start
      else
        raise EmitterError.new("expected StreamStartEvent, but got #{@event}")
      end
    end

    def expect_nothing
      raise EmitterError.new("expected nothing, but got #{@event}")
    end

    # Document handlers.

    def expect_first_document_start
      expect_document_start(true)
    end

    def expect_document_start(first=false)
      if DocumentStartEvent === @event
        if @event.version
          version_text = prepare_version(@event.version)
          write_version_directive(version_text)
        end
        @tag_prefixes = Emitter::DEFAULT_TAG_PREFIXES.dup
        if @event.tags
          handles = @event.tags.keys
          handles.sort!
          for handle in handles
            prefix = @event.tags[handle]
            @tag_prefixes[prefix] = handle
            handle_text = prepare_tag_handle(handle)
            prefix_text = prepare_tag_prefix(prefix)
            write_tag_directive(handle_text, prefix_text)
          end
        end
        implicit = first && !@event.explicit && !@canonical && !@event.version && !@event.tags && !check_empty_document
        if !implicit
          write_indent
          write_indicator("--- ",true,true)
          if @canonical
            write_indent
          end
        end
        @state = :expect_document_root
      elsif StreamEndEvent === @event
        write_stream_end
        @state = :expect_nothing
      else
        raise EmitterError.new("expected DocumentStartEvent, but got #{@event}")
      end
    end

    def expect_document_end
      if DocumentEndEvent === @event
        write_indent
        if @event.explicit
          write_indicator("...", true)
          write_indent
        end
        flush_stream
        @state = :expect_document_start
      else
        raise EmitterError.new("expected DocumentEndEvent, but got #{@event}")
      end
    end

    def expect_document_root
      @states << :expect_document_end
      expect_node(true)
    end

    # Node handlers.

    def expect_node(root=false, sequence=false, mapping=false, simple_key=false)
      @root_context = root
      @sequence_context = sequence
      @mapping_context = mapping
      @simple_key_context = simple_key
      if AliasEvent === @event
        expect_alias
      elsif ScalarEvent === @event || CollectionStartEvent === @event
        process_anchor("&")
        process_tag
        if ScalarEvent === @event
          expect_scalar
        elsif SequenceStartEvent === @event
          if @flow_level!=0 || @canonical || @event.flow_style || check_empty_sequence
            expect_flow_sequence
          else
            expect_block_sequence
          end
        elsif MappingStartEvent === @event
          if @flow_level!=0 || @canonical || @event.flow_style || check_empty_mapping
            expect_flow_mapping
          else
            expect_block_mapping
          end
        end
      else
        raise EmitterError.new("expected NodeEvent, but got #{@event}")
      end
    end

    def expect_alias
      raise EmitterError.new("anchor is not specified for alias") if @event.anchor.nil?
      process_anchor("*")
      @state = @states.pop
    end

    def expect_scalar
      increase_indent(true)
      process_scalar
      @indent = @indents.pop
      @state = @states.pop
    end

    # Flow sequence handlers.

    def expect_flow_sequence
      write_indicator("[", true, true)
      @flow_level += 1
      increase_indent(true)
      @state = :expect_first_flow_sequence_item
    end

    def expect_first_flow_sequence_item
      if SequenceEndEvent === @event
        @indent = @indents.pop
        @flow_level -= 1
        write_indicator("]", false)
        @state = @states.pop
      else
        write_indent if @canonical || @column > @best_width
        @states << :expect_flow_sequence_item
        expect_node(false,true)
      end
    end
    
    def expect_flow_sequence_item
      if SequenceEndEvent === @event
        @indent =  @indents.pop
        @flow_level -= 1
        if @canonical
          write_indicator(",",false)
          write_indent
        end
        write_indicator("]",false)
        @state = @states.pop
      else
        write_indicator(",", false)
        write_indent if @canonical or @column > @best_width
        @states << :expect_flow_sequence_item
        expect_node(false,true)
      end
    end
    
    # Flow mapping handlers.

    def expect_flow_mapping
      write_indicator("{", true, true)
      @flow_level += 1
      increase_indent(true)
      @state = :expect_first_flow_mapping_key
    end

    def expect_first_flow_mapping_key
      if MappingEndEvent === @event
        @indent = @indents.pop
        @flow_level -= 1
        write_indicator("}", false)
        @state = @states.pop
      else
        write_indent if @canonical || @column > @best_width
        if !@canonical && check_simple_key
          @states << :expect_flow_mapping_simple_value
          expect_node(false,false,true,true)
        else
          write_indicator("?", true)
          @states << :expect_flow_mapping_value
          expect_node(false,false,true)
        end
      end
    end

    def expect_flow_mapping_key
      if MappingEndEvent === @event
        @indent = @indents.pop
        @flow_level -= 1
        if @canonical
          write_indicator(",", false)
          write_indent
        end
        write_indicator("}", false)
        @state = @states.pop
      else
        write_indicator(",", false)
        write_indent if @canonical || @column > @best_width
        if !@canonical && check_simple_key
          @states << :expect_flow_mapping_simple_value
          expect_node(false,false,true,true)
        else
          write_indicator("?", true)
          @states << :expect_flow_mapping_value
          expect_node(false,false,true)
        end
      end
    end
    
    def expect_flow_mapping_simple_value
      write_indicator(": ", false,true)
      @states << :expect_flow_mapping_key
      expect_node(false,false,true)
    end

    def expect_flow_mapping_value
      write_indent if @canonical || @column > @best_width
      write_indicator(": ", false,true)
      @states << :expect_flow_mapping_key
      expect_node(false,false,true)
    end
    
    # Block sequence handlers.
    
    def expect_block_sequence
      indentless = @mapping_context && !@indention
      increase_indent(false,indentless)
      @state = :expect_first_block_sequence_item
    end

    def expect_first_block_sequence_item
      expect_block_sequence_item(true)
    end
    
    def expect_block_sequence_item(first=false)
      if !first && SequenceEndEvent === @event
        @indent = @indents.pop
        @state = @states.pop
      else
        write_indent
        write_indicator("-", true, false, true)
        @states << :expect_block_sequence_item
        expect_node(false,true)
      end
    end

    # Block mapping handlers.
    
    def expect_block_mapping
      increase_indent(false)
      @state = :expect_first_block_mapping_key
    end

    def expect_first_block_mapping_key
      expect_block_mapping_key(true)
    end
    
    def expect_block_mapping_key(first=false)
      if !first && MappingEndEvent === @event
        @indent = @indents.pop
        @state = @states.pop
      else
        write_indent
        if check_simple_key
          @states << :expect_block_mapping_simple_value
          expect_node(false,false,true,true)
        else
          write_indicator("?", true, false, true)
          @states << :expect_block_mapping_value
          expect_node(false,false,true)
        end
      end
    end
    
    def expect_block_mapping_simple_value
      write_indicator(": ", false,true)
      @states << :expect_block_mapping_key
      expect_node(false,false,true)
    end

    def expect_block_mapping_value
      write_indent
      write_indicator(": ",true,true,true)
      @states << :expect_block_mapping_key
      expect_node(false,false,true)
    end

    # Checkers.

    def check_empty_sequence
      @event.__is_sequence_start && !@events.empty? && @events.first.__is_sequence_end
    end
    
    def check_empty_mapping
      @event.__is_mapping_start && !@events.empty? && @events.first.__is_mapping_end
    end

    def check_empty_document
      return false if !@event.__is_document_start || @events.empty?
      event = @events.first
      event.__is_scalar && event.anchor.nil? && event.tag.nil? && event.implicit && event.value == ""
    end

    def check_simple_key
      length = 0
      if @event.__is_node && !@event.anchor.nil?
        @prepared_anchor = prepare_anchor(@event.anchor) if @prepared_anchor.nil?
        length += @prepared_anchor.length
      end
      if (@event.__is_scalar || @event.__is_collection_start) && !@event.tag.nil?
        @prepared_tag = prepare_tag(@event.tag) if @prepared_tag.nil?
        length += @prepared_tag.length
      end
      if @event.__is_scalar
        @analysis = analyze_scalar(@event.value) if @analysis.nil?
        length += @analysis.scalar.length
      end

      (length < 128 && (@event.__is_alias || (@event.__is_scalar && !@analysis.empty && !@analysis.multiline) || 
                        check_empty_sequence || check_empty_mapping))
    end

    
    # Anchor, Tag, and Scalar processors.
    
    def process_anchor(indicator)
      if @event.anchor.nil?
        @prepared_anchor = nil
        return nil
      end
      @prepared_anchor = prepare_anchor(@event.anchor) if @prepared_anchor.nil?
      write_indicator(indicator+@prepared_anchor, true) if @prepared_anchor && !@prepared_anchor.empty?
      @prepared_anchor = nil
    end
    
    def process_tag
      tag = @event.tag
      if ScalarEvent === @event
        @style = choose_scalar_style if @style.nil?
        if ((!@canonical || tag.nil?) && ((@style == "" && @event.implicit[0]) || (@style != "" && @event.implicit[1])))
          @prepared_tag = nil
          return
        end
        if @event.implicit[0] && tag.nil?
          tag = "!"
          @prepared_tag = nil
        end
      else
        if (!@canonical || tag.nil?) && @event.implicit
          @prepared_tag = nil
          return
        end
      end
      raise EmitterError.new("tag is not specified") if tag.nil?
      @prepared_tag = prepare_tag(tag) if @prepared_tag.nil?
      write_indicator(@prepared_tag, true) if @prepared_tag && !@prepared_tag.empty?
      @prepared_tag = nil
    end
    
    def choose_scalar_style
      @analysis = analyze_scalar(@event.value) if @analysis.nil?
      return '"' if @event.style == '"' || @canonical            
      if !@event.style && @event.implicit[0]
        if !(@simple_key_context && (@analysis.empty || @analysis.multiline)) && ((@flow_level!=0 && @analysis.allow_flow_plain) || (@flow_level == 0 && @analysis.allow_block_plain))
          return ""
        end
      end
      if !@event.style && @event.implicit && (!(@simple_key_context && (@analysis.empty || @analysis.multiline)) && 
                                              (@flow_level!=0 && @analysis.allow_flow_plain || (@flow_level==0 && @analysis.allow_block_plain)))
        return ""
      end
      return @event.style if @event.style && /^[|>]$/ =~ @event.style && @flow_level==0 && @analysis.allow_block
      return "'" if (!@event.style || @event.style == "'") && (@analysis.allow_single_quoted && !(@simple_key_context && @analysis.multiline))
      return '"'
    end
    
    def process_scalar
      @analysis = analyze_scalar(@event.value) if @analysis.nil?
      @style = choose_scalar_style if @style.nil?
      split = !@simple_key_context
      if @style == '"'
        write_double_quoted(@analysis.scalar, split)
      elsif @style == "'"
        write_single_quoted(@analysis.scalar, split)
      elsif @style == ">"
        write_folded(@analysis.scalar)
      elsif @style == "|"
        write_literal(@analysis.scalar)
      else
        write_plain(@analysis.scalar, split)
      end
      @analysis = nil
      @style = nil
    end

    # Analyzers.

    def prepare_version(version)
      major, minor = version
      raise EmitterError.new("unsupported YAML version: #{major}.#{minor}") if major != 1
      "#{major}.#{minor}"
    end
    
    def prepare_tag_handle(handle)
      raise EmitterError.new("tag handle must not be empty") if handle.nil? || handle.empty?
      raise EmitterError("tag handle must start and end with '!': #{handle}") if handle[0] != ?! || handle[-1] != ?!
      raise EmitterError.new("invalid character #{$&} in the tag handle: #{handle}") if /[^-\w]/ =~ handle[1...-1]
      handle
    end

    def prepare_tag_prefix(prefix)
      raise EmitterError.new("tag prefix must not be empty") if prefix.nil? || prefix.empty?
      chunks = []
      start = ending = 0
      ending = 1 if prefix[0] == ?!
      ending += 1 while ending < prefix.length
      chunks << prefix[start...ending] if start < ending
      chunks.to_s
    end

    def prepare_tag(tag)
      raise EmitterError.new("tag must not be empty") if tag.nil? || tag.empty?
      return tag if tag == "!"
      handle = nil
      suffix = tag
      for prefix in @tag_prefixes.keys
        if Regexp.new("^"+Regexp.escape(prefix)) =~ tag && (prefix == "!" || prefix.length < tag.length)
          handle = @tag_prefixes[prefix]
          suffix = tag[prefix.length..-1]
        end
      end
      chunks = []
      start = ending = 0
      ending += 1 while ending < suffix.length
      chunks << suffix[start...ending] if start < ending
      suffix_text = chunks.to_s
      if handle
        "#{handle}#{suffix_text}"
      else
        "!<#{suffix_text}>"
      end
    end

    def prepare_anchor(anchor)
      raise EmitterError.new("anchor must not be empty") if anchor.nil? || anchor.empty?
      raise EmitterError.new("invalid character #{$&} in the anchor: #{anchor}") if /[^-\w]/ =~ anchor
      anchor
    end

    def analyze_scalar(scalar)
      # Empty scalar is a special case.
      return ScalarAnalysis.new(scalar,true,false,false,true,true,true,false) if scalar.nil? || scalar.empty?
      # Indicators and special characters.
      block_indicators = false
      flow_indicators = false
      line_breaks = false
      special_characters = false

      # Whitespaces.
      inline_spaces = false          # non-space space+ non-space
      inline_breaks = false          # non-space break+ non-space
      leading_spaces = false         # ^ space+ (non-space | $)
      leading_breaks = false         # ^ break+ (non-space | $)
      trailing_spaces = false        # (^ | non-space) space+ $
      trailing_breaks = false        # (^ | non-space) break+ $
      inline_breaks_spaces = false   # non-space break+ space+ non-space
      mixed_breaks_spaces = false    # anything else

      # Check document indicators.
      if /^(---|\.\.\.)/ =~ scalar
        block_indicators = true
        flow_indicators = true
      end

      # First character or preceded by a whitespace.
      preceeded_by_space = true

      # Last character or followed by a whitespace.
      followed_by_space = scalar.length == 1 || "\0 \t\r\n\x85".include?(scalar[1])

      # The current series of whitespaces contain plain spaces.
      spaces = false

      # The current series of whitespaces contain line breaks.
      breaks = false

      # The current series of whitespaces contain a space followed by a
      # break.
      mixed = false

      # The current series of whitespaces start at the beginning of the
      # scalar.
      leading = false
      
      index = 0
      while index < scalar.length
        ch = scalar[index]
        
        # Check for indicators.
        
        if index == 0
          # Leading indicators are special characters.
          if "#,[]{}#&*!|>'\"%@`".include?(ch) 
            flow_indicators = true
            block_indicators = true
          end
          if "?:".include?(ch)
            flow_indicators = true
            if followed_by_space
              block_indicators = true
            end
          end
          if ch == ?- && followed_by_space
            flow_indicators = true
            block_indicators = true
          end
        else
          # Some indicators cannot appear within a scalar as well.
          flow_indicators = true if ",?[]{}".include?(ch)
          if ch == ?:
            flow_indicators = true
            block_indicators = true if followed_by_space
          end
          if ch == ?# && preceeded_by_space
              flow_indicators = true
            block_indicators = true
          end
        end
        # Check for line breaks, special, and unicode characters.
        line_breaks = true if "\n\x85".include?(ch)
        if !(ch == ?\n || (?\x20 <= ch && ch <= ?\x7E))
          special_characters = true
        end
        # Spaces, line breaks, and how they are mixed. State machine.
        
        # Start or continue series of whitespaces.
        if " \n\x85".include?(ch)
          if spaces && breaks
            mixed = true if ch != 32      # break+ (space+ break+)    => mixed
          elsif spaces
            if ch != 32      # (space+ break+)   => mixed
              breaks = true
              mixed = true
            end
          elsif breaks
            spaces = true if ch == 32      # break+ space+
          else
            leading = (index == 0)
            if ch == 32      # space+
              spaces = true
            else               # break+
              breaks = true
            end
          end
          # Series of whitespaces ended with a non-space.
        elsif spaces || breaks
          if leading
            if spaces && breaks
              mixed_breaks_spaces = true
            elsif spaces
              leading_spaces = true
            elsif breaks
              leading_breaks = true
            end
          else
            if mixed
              mixed_breaks_spaces = true
            elsif spaces && breaks
              inline_breaks_spaces = true
            elsif spaces
              inline_spaces = true
            elsif breaks
              inline_breaks = true
            end
          end
          spaces = breaks = mixed = leading = false
        end
        
        # Series of whitespaces reach the end.
        if (spaces || breaks) && (index == scalar.length-1)
          if spaces && breaks
            mixed_breaks_spaces = true
          elsif spaces
            trailing_spaces = true
            leading_spaces = true if leading
          elsif breaks
            trailing_breaks = true
            leading_breaks = true if leading
          end    
          spaces = breaks = mixed = leading = false
        end
        # Prepare for the next character.
        index += 1
        preceeded_by_space = "\0 \t\r\n\x85".include?(ch)
        followed_by_space = index+1 >= scalar.length || "\0 \t\r\n\x85".include?(scalar[index+1])
      end
      # Let's decide what styles are allowed.
      allow_flow_plain = true
      allow_block_plain = true
      allow_single_quoted = true
      allow_double_quoted = true
      allow_block = true
      # Leading and trailing whitespace are bad for plain scalars. We also
      # do not want to mess with leading whitespaces for block scalars.
      allow_flow_plain = allow_block_plain = allow_block = false if leading_spaces || leading_breaks || trailing_spaces

      # Trailing breaks are fine for block scalars, but unacceptable for
      # plain scalars.
      allow_flow_plain = allow_block_plain = false if trailing_breaks

      # The combination of (space+ break+) is only acceptable for block
      # scalars.
      allow_flow_plain = allow_block_plain = allow_single_quoted = false if inline_breaks_spaces

      # Mixed spaces and breaks, as well as special character are only
      # allowed for double quoted scalars.
      allow_flow_plain = allow_block_plain = allow_single_quoted = allow_block = false if mixed_breaks_spaces || special_characters

      # We don't emit multiline plain scalars.
      allow_flow_plain = allow_block_plain = false if line_breaks

      # Flow indicators are forbidden for flow plain scalars.
      allow_flow_plain = false if flow_indicators

      # Block indicators are forbidden for block plain scalars.
      allow_block_plain = false if block_indicators

      ScalarAnalysis.new(scalar,false,line_breaks,allow_flow_plain,allow_block_plain,allow_single_quoted,allow_double_quoted,allow_block)
    end

    # Writers.
    
    def flush_stream
      @stream.flush if @stream.respond_to?(:flush)
    end
    
    def write_stream_start
    end
    
    def write_stream_end
      flush_stream
    end
    
    def write_indicator(indicator, need_whitespace,whitespace=false,indention=false)
      if @whitespace || !need_whitespace
        data = indicator
      else
        data = " "+indicator
      end
      
      @whitespace = whitespace
      @indention = @indention && indention
      @column += data.length
      @stream.write(data)
    end

    def write_indent
      indent = @indent || 0
      write_line_break if !@indention || @column > indent || (@column == indent && !@whitespace)
      if @column < indent
        @whitespace = true
        data = " "*(indent-@column)
        @column = indent
        @stream.write(data)
      end
    end

    def write_line_break(data=nil)
      data = @best_line_break if data.nil?
      @whitespace = true
      @indention = true
      @line += 1
      @column = 0
      @stream.write(data)
    end


    def write_version_directive(version_text)
      data = "%YAML #{version_text}"
      @stream.write(data)
      write_line_break
    end
    
    def write_tag_directive(handle_text, prefix_text)
      data = "%TAG #{handle_text} #{prefix_text}"
      @stream.write(data)
      write_line_break
    end
    
    # Scalar streams.

    def write_single_quoted(text, split=true)
      write_indicator("'",true)
      spaces = false
      breaks = false
      start = ending = 0
      while ending <= text.length
        ch = nil
        ch = text[ending] if ending < text.length
        if spaces
          if ch.nil? || ch != 32
            if start+1 == ending && @column > @best_width && split && start != 0 && ending != text.length
              write_indent
            else
              data = text[start...ending]
              @column += data.length
              @stream.write(data)
            end
            start = ending
          end
        elsif breaks
          if ch.nil? or !"\n\x85".include?(ch)
            (text[start...ending]).each_byte { |br|
              if br == ?\n
                write_line_break
              else
                write_line_break(br)
              end
            }
            write_indent
            start = ending
          end
        else
          if ch.nil? || "' \n\x85".include?(ch)
            if start < ending
              data = text[start...ending]
              @column += data.length
              @stream.write(data)
              start = ending
            end
            if ch == ?'
              data = "''"
              @column += 2
              @stream.write(data)
              start = ending + 1
            end
          end
        end
        
        if !ch.nil?
          spaces = ch == 32
          breaks = "\n\x85".include?(ch)
        end
        
        ending += 1
      end
      write_indicator("'", false)
    end

    ESCAPE_REPLACEMENTS = {
      ?\0   =>   "0",
      ?\x07 =>   "a",
      ?\x08 =>   "b",
      ?\x09 =>   "t",
      ?\x0A =>   "n",
      ?\x0B =>   "v",
      ?\x0C =>   "f",
      ?\x0D =>   "r",
      ?\x1B =>   "e",
      ?"    =>   "\"",
      ?\\   =>   "\\",
      ?\x85 =>   "N",
      ?\xA0 =>   "_"
    }

    def write_double_quoted(text, split=true)
      write_indicator('"', true)
      start = ending = 0
      while ending <= text.length
        ch = nil
        ch = text[ending] if ending < text.length
        if ch.nil? || "\"\\\x85".include?(ch) || !(?\x20 <= ch && ch <= ?\x7E)
          if start < ending
            data = text[start...ending]
            @column += data.length
            @stream.write(data)
            start = ending
          end
          if !ch.nil?
            if ESCAPE_REPLACEMENTS.include?(ch)
              data = "\\"+ESCAPE_REPLACEMENTS[ch]
            elsif ch <= ?\xFF
              data = "\\x%02X" % ch
            end
            @column += data.length
            @stream.write(data)
            start = ending+1
          end
        end
        if (0 < ending && ending < text.length-1) && (ch == 32 || start >= ending) && @column+(ending-start) > @best_width && split
          data = text[start...ending]+"\\"
          start = ending if start < ending
          @column += data.length
          @stream.write(data)
          write_indent
          @whitespace = false
          @indention = false
          if text[start] == 32
            data = "\\"
            @column += data.length
            @stream.write(data)
          end
        end
        ending += 1
      end
      write_indicator('"', false)
    end

    def determine_chomp(text)
      tail = text[-2..-1]
      tail = " "+tail while tail.length < 2
      "\n\x85".include?(tail[-1])? ("\n\x85".include?(tail[-2])? "+" : "" ) : "-"
    end

    def write_folded(text)
      chomp = determine_chomp(text)
      write_indicator(">"+chomp, true)
      write_indent
      leading_space = false
      spaces = false
      breaks = false
      start = ending = 0
      while ending <= text.length
        ch = nil
        ch = text[ending] if ending < text.length
        if breaks
          if ch.nil? || !"\n\x85".include?(ch)
            write_line_break if !leading_space && !ch.nil? && ch != 32 && text[start] == ?\n                        
            leading_space = ch == 32
            (text[start...ending]).each_byte { |br|
              if br == ?\n
                write_line_break
              else
                write_line_break(br)
              end
            }
            write_indent if !ch.nil?
            start = ending
          end
        elsif spaces
          if ch != 32
            if start+1 == ending && @column > @best_width
              write_indent
            else
              data = text[start...ending]
              @column += data.length
              @stream.write(data)
            end
            start = ending
          end
        else
          if ch.nil? || " \n\x85".include?(ch)
            data = text[start...ending]
            @stream.write(data)
            write_line_break if ch.nil?
            start = ending
          end
        end
        if !ch.nil?
          breaks = "\n\x85".include?(ch)
          spaces = ch == 32
        end
        ending += 1
      end
    end

    def write_literal(text)
      chomp = determine_chomp(text)
      write_indicator("|"+chomp, true)
      write_indent
      breaks = false
      start = ending = 0
      while ending <= text.length
        ch = nil
        ch = text[ending] if ending < text.length
        if breaks
          if ch.nil? || !"\n\x85".include?(ch)
            (text[start...ending]).each_byte { |br|
              if br == ?\n
                write_line_break
              else
                write_line_break(br)
              end
            }
            write_indent if !ch.nil?
            start = ending
          end
        else
          if ch.nil? || "\n\x85".include?(ch)
            data = text[start...ending]
            @stream.write(data)
            write_line_break if ch.nil?
            start = ending
          end
        end
        breaks = "\n\x85".include?(ch) if !ch.nil?
        ending += 1
      end
    end

    def write_plain(text, split=true)
      return nil if text.nil? || text.empty?
      if !@whitespace
        data = " "
        @column += data.length
        @stream.write(data)
      end
      @writespace = false
      @indention = false
      spaces = false
      breaks = false
      start = ending = 0
      while ending <= text.length
        ch = nil
        ch = text[ending] if ending < text.length
        if spaces
          if ch != 32
            if start+1 == ending && @column > @best_width && split
              write_indent
              @writespace = false
              @indention = false
            else
              data = text[start...ending]
              @column += data.length
              @stream.write(data)
            end
            start = ending
          end
        elsif breaks
          if !"\n\x85".include?(ch)
            write_line_break if text[start] == ?\n
            (text[start...ending]).each_byte { |br|
              if br == ?\n
                write_line_break
              else
                write_line_break(br)
              end
            }
            write_indent
            @whitespace = false
            @indention = false
            start = ending
          end
        else
          if ch.nil? || " \n\x85".include?(ch)
            data = text[start...ending]
            @column += data.length
            @stream.write(data)
            start = ending
          end
        end
        if !ch.nil?
          spaces = ch == 32
          breaks = "\n\x85".include?(ch)
        end
        ending += 1
      end
    end
  end
end
