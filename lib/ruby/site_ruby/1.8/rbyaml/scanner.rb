# Scanner produces tokens of the following types:
# STREAM-START
# STREAM-END
# DIRECTIVE(name, value)
# DOCUMENT-START
# DOCUMENT-END
# BLOCK-SEQUENCE-START
# BLOCK-MAPPING-START
# BLOCK-END
# FLOW-SEQUENCE-START
# FLOW-MAPPING-START
# FLOW-SEQUENCE-END
# FLOW-MAPPING-END
# BLOCK-ENTRY
# FLOW-ENTRY
# KEY
# VALUE
# ALIAS(value)
# ANCHOR(value)
# TAG(value)
# SCALAR(value, plain)
#
# Read comments in the Scanner code for more details.
#

require 'rbyaml/util'
require 'rbyaml/error'
require 'rbyaml/tokens'

module RbYAML
  class ScannerError < MarkedYAMLError
  end
  class ReaderError < YAMLError
    def initialize(name, position, character, encoding, reason)
      @name = name
      @position = position
      @character = character
      @encoding = encoding
      @reason = reason
    end

    def to_s
      if @character.__is_str
        "'#{@encoding}' codec can't decode byte #x%02x: #{@reason}\n  in \"#{@name}\", position #{@position}" % @character.to_i
      else
        "unacceptable character #x%04x: #{@reason}\n  in \"#{@name}\", position #{@position}" % @character.to_i
      end
    end
  end

  SimpleKey = Struct.new(:token_number, :required, :index, :line, :column, :mark)

  class Scanner
    attr_reader :column, :stream, :stream_pointer, :eof, :buffer, :pointer, :index, :line
    def initialize(stream)
      # Had we reached the end of the stream?
      @done = false

      # The number of unclosed '{' and '['. `flow_level == 0` means block
      # context.
      @flow_level = 0
      @flow_zero = true

      # List of processed tokens that are not yet emitted.
      @tokens = []

      # Add the STREAM-START token.
      fetch_stream_start

      # Number of tokens that were emitted through the `get_token` method.
      @tokens_taken = 0

      # The current indentation level.
      @indent = -1

      # Past indentation levels.
      @indents = []

      # Variables related to simple keys treatment.

      # A simple key is a key that is not denoted by the '?' indicator.
      # Example of simple keys:
      #   ---
      #   block simple key: value
      #   ? not a simple key:
      #   : { flow simple key: value }
      # We emit the KEY token before all keys, so when we find a potential
      # simple key, we try to locate the corresponding ':' indicator.
      # Simple keys should be limited to a single line and 1024 characters.

      # Can a simple key start at the current position? A simple key may
      # start:
      # - at the beginning of the line, not counting indentation spaces
      #       (in block context),
      # - after '{', '[', ',' (in the flow context),
      # - after '?', ':', '-' (in the block context).
      # In the block context, this flag also signifies if a block collection
      # may start at the current position.
      @allow_simple_key = true

      # Keep track of possible simple keys. This is a dictionary. The key
      # is `flow_level`; there can be no more that one possible simple key
      # for each level. The value is a SimpleKey record:
      #   (token_number, required, index, line, column, mark)
      # A simple key may start with ALIAS, ANCHOR, TAG, SCALAR(flow),
      # '[', or '{' tokens.
      @possible_simple_keys = {}

      @stream = nil
      @stream_pointer = 0
      @eof = true
      @buffer = ""
      @buffer_length = 0
      @pointer = 0
      @pointer1 = 1
      @column = 0
      if stream.__is_str
        @name = "<string>"
        @raw_buffer = stream
      else
        @stream = stream
        @name = stream.respond_to?(:path) ? stream.path : stream.inspect
        @eof = false
        @raw_buffer = ""
      end
    end

    def peek(index=0)
      peekn(index)
    end

    def peek0
      update(1) unless @pointer1 < @buffer_length
      @buffer[@pointer]
    end

    def peek1
      update(2) unless @pointer1+1 < @buffer_length
      @buffer[@pointer1]
    end

    def peek2
      update(3) unless @pointer1+2 < @buffer_length
      @buffer[@pointer1+1]
    end

    def peek3
      update(4) unless @pointer1+3 < @buffer_length
      @buffer[@pointer1+2]
    end

    def peekn(index=0)
      pix = @pointer1+index
      unless pix < @buffer_length
        update(index+1)
        pix = @pointer1+index
      end
      @buffer[pix-1]
    end
    
    def prefix(length=1)
      update(length) unless @pointer+length < @buffer_length
      @buffer[@pointer...@pointer+length]
    end

    def prefix2()
      update(2) unless @pointer1+1 < @buffer_length
      @buffer[@pointer..@pointer1]
    end

    def forward(length=1)
      case length
        when 0: forward0
        when 1: forward1
        when 2: forward2
        when 3: forward3
        when 4: forward4
        when 5: forward5
        when 6: forward6
        else forwardn(length)
      end
    end

    def forward0
      update(1) unless @pointer1 < @buffer_length
    end

    LINE_BR = "\n\x85"

    def forward1
      update(2) unless @pointer1+1 < @buffer_length
      buff = @buffer[@pointer...@pointer1+1]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? -index : column+1
      @pointer += 1
      @pointer1 += 1
    end
    
    def forward2
      update(3) unless @pointer1+2 < @buffer_length
      buff = @buffer[@pointer...@pointer1+2]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? 1-index : column+2
      @pointer += 2
      @pointer1 += 2
    end

    def forward3
      update(4) unless @pointer1+3 < @buffer_length
      buff = @buffer[@pointer...@pointer1+3]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? 2-index : column+3
      @pointer += 3
      @pointer1 += 3
    end

    def forward4
      update(5) unless @pointer1+4 < @buffer_length
      buff = @buffer[@pointer...@pointer1+4]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? 3-index : column+4
      @pointer += 4
      @pointer1 += 4
    end

    def forward5
      update(6) unless @pointer1+5 < @buffer_length
      buff = @buffer[@pointer...@pointer1+5]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? 4-index : column+5
      @pointer += 5
      @pointer1 += 5
    end

    def forward6
      update(7) unless @pointer1+6 < @buffer_length
      buff = @buffer[@pointer...@pointer1+6]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? 5-index : column+6
      @pointer += 6
      @pointer1 += 6
    end

    LINE_BR_REG = /[\n\x85]|(?:\r[^\n])/
    def forwardn(length)
      update(length + 1) unless @pointer1+length < @buffer_length
      buff = @buffer[@pointer...@pointer+length]
      index = buff.rindex(LINE_BR_REG)
      @column = index ? (length-index)-1 : column+length
      @pointer += length
      @pointer1 += length
    end
    
    def get_mark
      if @stream.nil?
        Mark.new(@name,@column,@buffer,@pointer)
      else
        Mark.new(@name,@column,nil,nil)
      end
    end
    
    NON_PRINTABLE = /[^\x09\x0A\x0D\x20-\x7E\x85\xA0-\xFF]/
    def check_printable(data)
      if NON_PRINTABLE =~ data
        position = @buffer.length-@pointer+($~.offset(0)[0])
        raise ReaderError.new(@name, position, $&,"unicode","special characters are not allowed"),"special characters are not allowed"
      end
    end


    def update(length)
      return if @raw_buffer.nil?
      @buffer = @buffer[@pointer..-1]
      @pointer = 0
      while @buffer.length < length
        unless @eof
          data = @stream.read(1024)
          if data && !data.empty?
            @buffer << data
            @stream_pointer += data.length
            @raw_buffer = ""
          else
            @eof = true
            @buffer << ?\0
            @raw_buffer = nil
            break
          end
        else
          @buffer << @raw_buffer << ?\0
          @raw_buffer = nil
          break
        end
      end
      @buffer_length = @buffer.length
      if @eof
        check_printable(@buffer[(-length)..-2])
      else
        check_printable(@buffer[(-length)..-1])
      end
      @pointer1 = @pointer+1
    end

    def check_token(*choices)
      # Check if the next token is one of the given types.
      fetch_more_tokens while need_more_tokens
      unless @tokens.empty?
        return true if choices.empty?
        for choice in choices
          return true if choice === @tokens[0]
        end
      end
      return false
    end
    
    def peek_token
      # Return the next token, but do not delete if from the queue.
      fetch_more_tokens while need_more_tokens
      return @tokens[0] unless @tokens.empty?
    end
    
    def get_token
      # Return the next token.
      fetch_more_tokens while need_more_tokens
      unless @tokens.empty?
        @tokens_taken += 1
        @tokens.shift
      end
    end

    def each_token
      fetch_more_tokens while need_more_tokens
      while !@tokens.empty?
        @tokens_taken += 1
        yield @tokens.shift
        fetch_more_tokens while need_more_tokens
      end
    end
    
    def need_more_tokens
      return false if @done
      @tokens.empty? || next_possible_simple_key == @tokens_taken
    end

    ENDING = /^---[\0 \t\r\n\x85]$/
    START = /^\.\.\.[\0 \t\r\n\x85]$/
    NULL_OR_OTHER = "\0 \t\r\n\x85"
#    BEG = /^([^\0 \t\r\n\x85\-?:,\[\]{}#&*!|>'"%@`]|([\-?:][^\0 \t\r\n\x85]))/    #Since current SYCK handles this one wrong, we have to allow backtick right now.
    BEG = /^([^\0 \t\r\n\x85\-?:,\[\]{}#&*!|>'"%@]|([\-?:][^\0 \t\r\n\x85]))/    
    def fetch_more_tokens
      # Eat whitespaces and comments until we reach the next token.
      scan_to_next_token

      # Remove obsolete possible simple keys.
#      stale_possible_simple_keys

      # Compare the current indentation and column. It may add some tokens
      # and decrease the current indentation level.
      unwind_indent(@column)

      # Peek the next character.
      ch = peek0
      colz = @column == 0

      case ch
      when ?\0: return fetch_stream_end
      when ?': return fetch_single
      when ?": return fetch_double
      when ??: if !@flow_zero || NULL_OR_OTHER.include?(peek1): return fetch_key end
      when ?:: if !@flow_zero || NULL_OR_OTHER.include?(peek1): return fetch_value end
      when ?%: if colz: return fetch_stream_end end
      when ?-: if colz && ENDING =~ prefix(4): return fetch_document_start; elsif NULL_OR_OTHER.include?(peek1): return fetch_block_entry end
      when ?.: if colz && START =~ prefix(4): return fetch_document_end end
      when ?[: return fetch_flow_sequence_start
      when ?{: return fetch_flow_mapping_start
      when ?]: return fetch_flow_sequence_end
      when ?}: return fetch_flow_mapping_end
      when ?,: return fetch_flow_entry
      when ?*: return fetch_alias
      when ?&: return fetch_anchor
      when ?!: return fetch_tag
      when ?|: if @flow_zero: return fetch_literal end
      when ?>: if @flow_zero: return fetch_folded end
      end
      return fetch_plain if BEG =~ prefix(2)
      raise ScannerError.new("while scanning for the next token", nil,"found character #{ch.chr}(#{ch}) that cannot start any token",get_mark)
    end

    # Simple keys treatment.
    
    def next_possible_simple_key
      # Return the number of the nearest possible simple key. Actually we
      # don't need to loop through the whole dictionary.
      @possible_simple_keys.each_value {|key| return key.token_number if key.token_number}
      nil
    end
    
    def save_possible_simple_key
      # The next token may start a simple key. We check if it's possible
      # and save its position. This function is called for
      #   ALIAS, ANCHOR, TAG, SCALAR(flow), '[', and '{'.
      # The next token might be a simple key. Let's save it's number and
      # position.
      @possible_simple_keys[@flow_level] = SimpleKey.new(@tokens_taken+@tokens.length, @flow_zero && @indent == @column,-1,-1,column,get_mark) if @allow_simple_key
    end

    # Indentation functions.

    def unwind_indent(col)
      ## In flow context, tokens should respect indentation.
      ## Actually the condition should be `@indent >= column` according to
      ## the spec. But this condition will prohibit intuitively correct
      ## constructions such as
      ## key : {
      ## }
      #if @flow_level and @indent > column
      #    raise ScannerError(nil, nil,
      #            "invalid intendation or unclosed '[' or '{'",
      #            get_mark)

      # In the flow context, indentation is ignored. We make the scanner less
      # restrictive then specification requires.
      return nil if !@flow_zero
      # In block context, we may need to issue the BLOCK-END tokens.
      while @indent > col
        mark = get_mark
        @indent = @indents.pop
        @tokens << BlockEndToken.new(mark, mark)
      end
    end
    
    def add_indent(col)
      # Check if we need to increase indentation.
      if @indent < col
        @indents << @indent
        @indent = col
        return true
      end
      return false
    end

    # Fetchers.

    def fetch_stream_start
      # We always add STREAM-START as the first token and STREAM-END as the
      # last token.
      # Read the token.
      mark = get_mark
      # Add STREAM-START.
      @tokens << StreamStartToken.new(mark, mark, @encoding)
    end


    def fetch_stream_end
      # Set the current intendation to -1.
      unwind_indent(-1)
      # Reset everything (not really needed).
      @allow_simple_key = false
      @possible_simple_keys = {}
      # Read the token.
      mark = get_mark
      # Add STREAM-END.
      @tokens << StreamEndToken.new(mark, mark)
      # The stream is finished.
      @done = true
    end

    def fetch_directive
      # Set the current intendation to -1.
      unwind_indent(-1)
      # Reset simple keys.
      @allow_simple_key = false
      # Scan and add DIRECTIVE.
      @tokens << scan_directive
    end

    def fetch_document_start
      fetch_document_indicator(DocumentStartToken)
    end

    def fetch_document_end
      fetch_document_indicator(DocumentEndToken)
    end

    def fetch_document_indicator(token)
      # Set the current intendation to -1.
      unwind_indent(-1)
      # Reset simple keys. Note that there could not be a block collection
      # after '---'.
      @allow_simple_key = false
      # Add DOCUMENT-START or DOCUMENT-END.
      start_mark = get_mark
      forward3
      end_mark = get_mark
      @tokens << token.new(start_mark, end_mark)
    end

    def fetch_flow_sequence_start
      fetch_flow_collection_start(FlowSequenceStartToken)
    end

    def fetch_flow_mapping_start
      fetch_flow_collection_start(FlowMappingStartToken)
    end

    def fetch_flow_collection_start(token)
      # '[' and '{' may start a simple key.
      save_possible_simple_key
      # Increase the flow level.
      @flow_level += 1
      @flow_zero = false
      # Simple keys are allowed after '[' and '{'.
      @allow_simple_key = true
      # Add FLOW-SEQUENCE-START or FLOW-MAPPING-START.
      start_mark = get_mark
      forward1
      end_mark = get_mark
      @tokens << token.new(start_mark, end_mark)
    end
    
    def fetch_flow_sequence_end
      fetch_flow_collection_end(FlowSequenceEndToken)
    end
    
    def fetch_flow_mapping_end
      fetch_flow_collection_end(FlowMappingEndToken)
    end
    
    def fetch_flow_collection_end(token)
      # Decrease the flow level.
      @flow_level -= 1
      if @flow_level == 0
        @flow_zero = true
      end
      # No simple keys after ']' or '}'.
      @allow_simple_key = false
      # Add FLOW-SEQUENCE-END or FLOW-MAPPING-END.
      start_mark = get_mark
      forward1
      end_mark = get_mark
      @tokens << token.new(start_mark, end_mark)
    end

    def fetch_flow_entry
      # Simple keys are allowed after ','.
      @allow_simple_key = true
      # Add FLOW-ENTRY.
      start_mark = get_mark
      forward1
      end_mark = get_mark
      @tokens << FlowEntryToken.new(start_mark, end_mark)
    end

    def fetch_block_entry
      # Block context needs additional checks.
      if @flow_zero
        raise ScannerError.new(nil,nil,"sequence entries are not allowed here",get_mark) if !@allow_simple_key
        # We may need to add BLOCK-SEQUENCE-START.
        if add_indent(column)
          mark = get_mark
          @tokens << BlockSequenceStartToken.new(mark, mark)
        end
        # It's an error for the block entry to occur in the flow context,
        # but we let the parser detect this.
      end
      # Simple keys are allowed after '-'.
      @allow_simple_key = true
      # Add BLOCK-ENTRY.
      start_mark = get_mark
      forward1
      end_mark = get_mark
      @tokens << BlockEntryToken.new(start_mark, end_mark)
    end

    def fetch_key
      # Block context needs additional checks.
      if @flow_zero
        # Are we allowed to start a key (not nessesary a simple)?
        raise ScannerError.new(nil,nil,"mapping keys are not allowed here",get_mark) if !@allow_simple_key
        # We may need to add BLOCK-MAPPING-START.
        if add_indent(column)
          mark = get_mark
          @tokens << BlockMappingStartToken.new(mark, mark)
        end
      end
      # Simple keys are allowed after '?' in the block context.
      @allow_simple_key = @flow_zero
      # Add KEY.
      start_mark = get_mark
      forward1
      end_mark = get_mark
      @tokens << KeyToken.new(start_mark, end_mark)
    end
    
    def fetch_value
      key = @possible_simple_keys[@flow_level]
      # Do we determine a simple key?
      if key.nil?
        # Block context needs additional checks.
        # (Do we really need them? They will be catched by the parser
        # anyway.)
        if @flow_zero
          # We are allowed to start a complex value if and only if
          # we can start a simple key.
          raise ScannerError.new(nil,nil,"mapping values are not allowed here",get_mark) if !@allow_simple_key
          # Simple keys are allowed after ':' in the block context.
          @allow_simple_key = true
        end
      else
        # Add KEY.
        @possible_simple_keys.delete(@flow_level)

        # If this key starts a new block mapping, we need to add
        # BLOCK-MAPPING-START.
        se = (@flow_zero && add_indent(key.column)) ? [BlockMappingStartToken.new(key.mark, key.mark)] : []
        se << KeyToken.new(key.mark, key.mark)
        @tokens.insert(key.token_number-@tokens_taken,*se)
        # There cannot be two simple keys one after another.
        @allow_simple_key = false
        # It must be a part of a complex key.
      end
      # Add VALUE.
      start_mark = get_mark
      forward1
      end_mark = get_mark
      @tokens << ValueToken.new(start_mark, end_mark)
    end

    def fetch_alias
      # ALIAS could be a simple key.
      save_possible_simple_key
      # No simple keys after ALIAS.
      @allow_simple_key = false
      # Scan and add ALIAS.
      @tokens << scan_anchor(AliasToken)
    end

    def fetch_anchor
      # ANCHOR could start a simple key.
      save_possible_simple_key
      # No simple keys after ANCHOR.
      @allow_simple_key = false
      # Scan and add ANCHOR.
      @tokens << scan_anchor(AnchorToken)
    end

    def fetch_tag
      # TAG could start a simple key.
      save_possible_simple_key
      # No simple keys after TAG.
      @allow_simple_key = false
      # Scan and add TAG.
      @tokens << scan_tag
    end

    def fetch_literal
      fetch_block_scalar(?|)
    end

    def fetch_folded
      fetch_block_scalar(?>)
    end

    def fetch_block_scalar(style)
      # A simple key may follow a block scalar.
      @allow_simple_key = true
      # Scan and add SCALAR.
      @tokens << scan_block_scalar(style)
    end

    def fetch_single
      fetch_flow_scalar(?')
    end
    
    def fetch_double
      fetch_flow_scalar(?")
    end

    def fetch_flow_scalar(style)
      # A flow scalar could be a simple key.
      save_possible_simple_key
      # No simple keys after flow scalars.
      @allow_simple_key = false
      # Scan and add SCALAR.
      @tokens << scan_flow_scalar(style)
    end
    
    def fetch_plain
      # A plain scalar could be a simple key.
      save_possible_simple_key
      # No simple keys after plain scalars. But note that `scan_plain` will
      # change this flag if the scan is finished at the beginning of the
      # line.
      @allow_simple_key = false
      # Scan and add SCALAR. May change `allow_simple_key`.
      @tokens << scan_plain
    end

    
    # Scanners.
    NULL_OR_LINEBR = "\0\r\n\x85"
    def scan_to_next_token
      # We ignore spaces, line breaks and comments.
      # If we find a line break in the block context, we set the flag
      # `allow_simple_key` on.
      #
      # TODO: We need to make tab handling rules more sane. A good rule is
      #   Tabs cannot precede tokens
      #   BLOCK-SEQUENCE-START, BLOCK-MAPPING-START, BLOCK-END,
      #   KEY(block), VALUE(block), BLOCK-ENTRY
      # So the checking code is
      #   if <TAB>:
      #       @allow_simple_keys = false
      # We also need to add the check for `allow_simple_keys == true` to
      # `unwind_indent` before issuing BLOCK-END.
      # Scanners for block, flow, and plain scalars need to be modified.
      while true
        while peek0 == 32
          forward1
        end
        if peek0 == ?#
          while !NULL_OR_LINEBR.include?(peek0)
            forward1
          end
        end

        if !scan_line_break.empty?
          @allow_simple_key = true if @flow_zero
        else
          break
        end
      end
    end
    
    def scan_directive
      # See the specification for details.
      start_mark = get_mark
      forward1
      name = scan_directive_name(start_mark)
      value = nil
      if name == "YAML"
        value = scan_yaml_directive_value(start_mark)
        end_mark = get_mark
      elsif name == "TAG"
        value = scan_tag_directive_value(start_mark)
        end_mark = get_mark
      else
        end_mark = get_mark
        forward1 while !NULL_OR_LINEBR.include?(peek0)
      end
      scan_directive_ignored_line(start_mark)
      DirectiveToken.new(name, value, start_mark, end_mark)
    end

    ALPHA_REG = /[-0-9A-Za-z_]/
    NULL_BL_LINEBR = "\0 \r\n\x85"
    NULL_BL_T_LINEBR = "\0 \t\r\n\x85"
    def scan_directive_name(start_mark)
      # See the specification for details.
      length = 0
      ch = peek(length)
      zlen = true
      while ALPHA_REG  =~ ch.chr
        zlen = false
        length += 1
        ch = peek(length)
      end
      raise ScannerError.new("while scanning a directive", start_mark,"expected alphabetic or numeric character, but found #{ch.to_s}",get_mark) if zlen
      value = prefix(length)
      forward(length)
      ch = peek0
      raise ScannerError.new("while scanning a directive", start_mark,"expected alphabetic or numeric character, but found #{ch.to_s}",get_mark) if !NULL_BL_LINEBR.include?(ch)
      value
    end

    def scan_yaml_directive_value(start_mark)
      # See the specification for details.
      forward1 while peek0 == 32
      major = scan_yaml_directive_number(start_mark)
      raise ScannerError.new("while scanning a directive", start_mark,"expected a digit or '.', but found #{peek.to_s}",get_mark) if peek0 != ?.
      forward1
      minor = scan_yaml_directive_number(start_mark)
      raise ScannerError.new("while scanning a directive", start_mark,"expected a digit or ' ', but found #{peek.to_s}",get_mark) if !NULL_BL_LINEBR.include?(peek0)
      [major, minor]
    end

    def scan_yaml_directive_number(start_mark)
      # See the specification for details.
      ch = peek0
      raise ScannerError.new("while scanning a directive", start_mark,"expected a digit, but found #{ch.to_s}",get_mark) if !(ch.__is_ascii_num)
      length = 0
      length += 1 while (peek(length).__is_ascii_num)
      value = prefix(length)
      forward(length)
      value
    end
    
    def scan_tag_directive_value(start_mark)
      # See the specification for details.
      forward1 while peek0 == 32
      handle = scan_tag_directive_handle(start_mark)
      forward1 while peek0 == 32
      prefix = scan_tag_directive_prefix(start_mark)
      [handle, prefix]
    end

    def scan_tag_directive_handle(start_mark)
      # See the specification for details.
      value = scan_tag_handle("directive", start_mark)
      raise ScannerError.new("while scanning a directive", start_mark,"expected ' ', but found #{peek0}",get_mark()) if peek0 != 32
      value
    end

    def scan_tag_directive_prefix(start_mark)
      # See the specification for details.
      value = scan_tag_uri("directive", start_mark)
      raise ScannerError.new("while scanning a directive", start_mark,"expected ' ', but found #{peek0}",get_mark()) if !NULL_BL_LINEBR.include?(peek0)
      value
    end
    
    def scan_directive_ignored_line(start_mark)
      # See the specification for details.
      forward1 while peek0 == 32
      if peek0 == ?#
          forward1 while !NULL_OR_LINEBR.include?(peek0)
      end
      ch = peek0
      raise ScannerError.new("while scanning a directive", start_mark,"expected a comment or a line break, but found #{peek0.to_s}",get_mark()) if !NULL_OR_LINEBR.include?(peek0)
      scan_line_break
    end
 
    NON_ALPHA = /[^-0-9A-Za-z_]/   
    NON_ALPHA_OR_NUM = "\0 \t\r\n\x85?:,]}%@`"
    def scan_anchor(token)
      # The specification does not restrict characters for anchors and
      # aliases. This may lead to problems, for instance, the document:
      #   [ *alias, value ]
      # can be interpteted in two ways, as
      #   [ "value" ]
      # and
      #   [ *alias , "value" ]
      # Therefore we restrict aliases to numbers and ASCII letters.
      start_mark = get_mark
      indicator = peek0
      name = (indicator == ?*) ? "alias":"anchor"
      forward1
      length = 0
      chunk_size = 16
      while true
        chunk = prefix(chunk_size)
        if length = (NON_ALPHA =~ chunk)
          break
        end
        chunk_size += 16
      end
      raise ScannerError.new("while scanning an #{name}", start_mark,"expected alphabetic or numeric character, but found something else...",get_mark) if length==0
      value = prefix(length)
      forward(length)
      if !NON_ALPHA_OR_NUM.include?(peek0)
        raise ScannerError.new("while scanning an #{name}", start_mark,"expected alphabetic or numeric character, but found #{peek0}",get_mark)
      end
      end_mark = get_mark
      token.new(value, start_mark, end_mark)
    end

    NULL_T_BL_LINEBR = "\0 \t\r\n\x85"
    def scan_tag
      # See the specification for details.
      start_mark = get_mark
      ch = peek1
      if ch == ?<
        handle = nil
        forward2
        suffix = scan_tag_uri("tag", start_mark)
        raise ScannerError.new("while parsing a tag", start_mark,"expected '>', but found #{peek.to_s}",get_mark) if peek0 != ?>
        forward1
      elsif NULL_T_BL_LINEBR.include?(ch)
        handle = nil
        suffix = "!"
        forward1
      else
        length = 1
        use_handle = false
        while !NULL_T_BL_LINEBR.include?(ch)
          if ch == ?!
            use_handle = true
            break
          end
          length += 1
          ch = peek(length)
        end
        handle = "!"
        if use_handle
          handle = scan_tag_handle("tag", start_mark)
        else
          handle = "!"
          forward1
        end
        suffix = scan_tag_uri("tag", start_mark)
      end
      raise ScannerError.new("while scanning a tag",start_mark,"expected ' ', but found #{peek0}",get_mark) if !NULL_BL_LINEBR.include?(peek0)
      value = [handle, suffix]
      end_mark = get_mark
      TagToken.new(value, start_mark, end_mark)
    end

    BLANK_T = " \t"
    def scan_block_scalar(style)
      # See the specification for details.
      folded = style== ?>
      chunks = []
      start_mark = get_mark
      # Scan the header.
      forward1
      chomping, increment = scan_block_scalar_indicators(start_mark)
      scan_block_scalar_ignored_line(start_mark)
      # Determine the indentation level and go to the first non-empty line.
      min_indent = @indent+1
      min_indent = 1 if min_indent < 1
      if increment.nil?
        breaks, max_indent, end_mark = scan_block_scalar_indentation
        indent = [min_indent, max_indent].max
      else
        indent = min_indent+increment-1
        breaks, end_mark = scan_block_scalar_breaks(indent)
      end
      line_break = ''
      # Scan the inner part of the block scalar.
      while column == indent and peek0 != ?\0
        chunks += breaks
        leading_non_space = !BLANK_T.include?(peek0)
        length = 0
        length += 1 while !NULL_OR_LINEBR.include?(peek(length))
        chunks << prefix(length)
        forward(length)
        line_break = scan_line_break
        breaks, end_mark = scan_block_scalar_breaks(indent)
        if column == indent && peek0 != 0
          # Unfortunately, folding rules are ambiguous.
          #
          # This is the folding according to the specification:
          if folded && line_break == "\n" && leading_non_space && !BLANK_T.include?(peek0)
            chunks << ' ' if breaks.empty?
          else
            chunks << line_break
          end 
          # This is Clark Evans's interpretation (also in the spec
          # examples):
          #
          #if folded and line_break == u'\n':
          #    if not breaks:
          #        if self.peek() not in ' \t':
          #            chunks.append(u' ')
          #        else:
          #            chunks.append(line_break)
          #else:
          #    chunks.append(line_break)
        else
          break
        end
      end
      
      # Chomp the tail.
      if chomping
        chunks << line_break
        chunks += breaks
      end

      # We are done.
      ScalarToken.new(chunks.to_s, false, start_mark, end_mark,style)
    end

    PLUS_MIN = /[+-]/
    def scan_block_scalar_indicators(start_mark)
      # See the specification for details.
      chomping = nil
      increment = nil
      ch = peek0
      if PLUS_MIN =~ ch.chr
        chomping = ch == ?+
        forward1
        ch = peek0
        if ch.__is_ascii_num
          increment = ch.chr.to_i
          raise ScannerError.new("while scanning a block scalar", start_mark,"expected indentation indicator in the range 1-9, but found 0",get_mark) if increment == 0
          forward1
        end
      elsif ch.__is_ascii_num
        increment = ch.chr.to_i
        raise ScannerError.new("while scanning a block scalar", start_mark,"expected indentation indicator in the range 1-9, but found 0",get_mark) if increment == 0
        forward1
        ch = peek0
        if PLUS_MIN =~ ch.chr
          chomping = ch == ?+
          forward1
        end
      end
      raise ScannerError.new("while scanning a block scalar", start_mark,"expected chomping or indentation indicators, but found #{peek0}",get_mark) if !NULL_BL_LINEBR.include?(peek0)
      [chomping, increment]
    end

    def scan_block_scalar_ignored_line(start_mark)
      # See the specification for details.
      forward1 while peek0 == 32
      if peek0 == ?#
          forward1 while !NULL_OR_LINEBR.include?(peek0)
      end
      raise ScannerError.new("while scanning a block scalar", start_mark,"expected a comment or a line break, but found #{peek0}",get_mark) if !NULL_OR_LINEBR.include?(peek0)
      scan_line_break
    end

    BLANK_OR_LINEBR = " \r\n\x85"
    def scan_block_scalar_indentation
      # See the specification for details.
      chunks = []
      max_indent = 0
      end_mark = get_mark
      while BLANK_OR_LINEBR.include?(peek0)
        if peek0 != 32
          chunks << scan_line_break
          end_mark = get_mark
        else
          forward1
          max_indent = column if column > max_indent
        end
      end
      [chunks, max_indent, end_mark]
    end

    FULL_LINEBR = "\r\n\x85"
    def scan_block_scalar_breaks(indent)
      # See the specification for details.
      chunks = []
      end_mark = get_mark
      forward1 while @column < indent && peek0 == 32
      while FULL_LINEBR.include?(peek0)
        chunks << scan_line_break
        end_mark = get_mark
        forward1 while @column < indent && peek0 == 32
      end
      [chunks, end_mark]
    end

    def scan_flow_scalar(style)
      # See the specification for details.
      # Note that we loose indentation rules for quoted scalars. Quoted
      # scalars don't need to adhere indentation because " and ' clearly
      # mark the beginning and the end of them. Therefore we are less
      # restrictive then the specification requires. We only need to check
      # that document separators are not included in scalars.
      double = style == ?"
      chunks = []
      start_mark = get_mark
      quote = peek0
      forward1
      chunks += scan_flow_scalar_non_spaces(double, start_mark)
      while peek0 != quote
        chunks += scan_flow_scalar_spaces(double, start_mark)
        chunks += scan_flow_scalar_non_spaces(double, start_mark)
      end
      forward1
      end_mark = get_mark
      ScalarToken.new(chunks.to_s, false, start_mark, end_mark,style)
    end
    
    ESCAPE_REPLACEMENTS = {
      "0"  =>   "\0",
      "a"  =>   "\x07",
      "b"  =>   "\x08",
      "t"  =>   "\x09",
      "\t" =>   "\x09",
      "n"  =>   "\x0A",
      "v"  =>   "\x0B",
      "f"  =>   "\x0C",
      "r"  =>   "\x0D",
      "e"  =>   "\x1B",
      " "  =>   "\x20",
      '"'  =>   '"',
      "\\" =>   "\\",
      "N"  =>   "\x85",
      "_"  =>   "\xA0"
    }
    
    ESCAPE_CODES = {
      'x' =>   2
    }
    
    SPACES_AND_STUFF = "'\"\\\0 \t\r\n\x85"
    DOUBLE_ESC = "\"\\"
    NOT_HEXA = /[^0-9A-Fa-f]/
    def scan_flow_scalar_non_spaces(double, start_mark)
      # See the specification for details.
      chunks = []
      while true
        length = 0
        length += 1 while !SPACES_AND_STUFF.include?(peek(length))
        if length!=0
          chunks << prefix(length)
          forward(length)
        end
        ch = peek0
        if !double && ch == ?' && peek1 == ?'
          chunks << ?'
          forward2
        elsif (double && ch == ?') || (!double && DOUBLE_ESC.include?(ch))
          chunks << ch
          forward1
        elsif double && ch == ?\\
          forward1
          ch = peek0
          if ESCAPE_REPLACEMENTS.member?(ch.chr)
            chunks << ESCAPE_REPLACEMENTS[ch.chr]
            forward1
          elsif ESCAPE_CODES.member?(ch.chr)
            length = ESCAPE_CODES[ch.chr]
            forward1
            if NOT_HEXA =~ prefix(length)
              raise ScannerError.new("while scanning a double-quoted scalar", start_mark,
                                     "expected escape sequence of #{length} hexdecimal numbers, but found something else: #{prefix(length)}}",get_mark)
            end
            code = prefix(length).to_i(16).to_s
            chunks << code
            forward(length)
          elsif FULL_LINEBR.include?(ch)
            scan_line_break
            chunks += scan_flow_scalar_breaks(double, start_mark)
          else
            raise ScannerError.new("while scanning a double-quoted scalar", start_mark,"found unknown escape character #{ch}",get_mark)
          end
        else
          return chunks
        end
      end
    end

    def scan_flow_scalar_spaces(double, start_mark)
      # See the specification for details.
      chunks = []
      length = 0
      length += 1 while BLANK_T.include?(peek(length))
      whitespaces = prefix(length)
      forward(length)
      ch = peek0
      if ch == ?\0
        raise ScannerError.new("while scanning a quoted scalar", start_mark,"found unexpected end of stream",get_mark)
      elsif FULL_LINEBR.include?(ch)
        line_break = scan_line_break
        breaks = scan_flow_scalar_breaks(double, start_mark)
        if line_break != "\n"
          chunks << line_break
        elsif breaks.empty?
          chunks << ' '
        end
        chunks += breaks
      else
        chunks << whitespaces
      end
      chunks
    end
    
    def scan_flow_scalar_breaks(double, start_mark)
      # See the specification for details.
      chunks = []
      while true
        # Instead of checking indentation, we check for document
        # separators.
        prefix = prefix(3)
        if (prefix == "---" || prefix == "...") &&NULL_BL_T_LINEBR.include?(peek3)
          raise ScannerError.new("while scanning a quoted scalar", start_mark,"found unexpected document separator", get_mark)
        end
        forward1 while BLANK_T.include?(peek0)
        if FULL_LINEBR.include?(peek0)
          chunks << scan_line_break
        else
          return chunks
        end
      end
    end
    

    R_flowzero = /[\0 \t\r\n\x85]|(:[\0 \t\r\n\x28])/
    R_flownonzero = /[\0 \t\r\n\x85\[\]{},:?]/
    S4 = "\0 \t\r\n\x28[]{}"

    def scan_plain
      # See the specification for details.
      # We add an additional restriction for the flow context:
      #   plain scalars in the flow context cannot contain ',', ':' and '?'.
      # We also keep track of the `allow_simple_key` flag here.
      # Indentation rules are loosed for the flow context.
      chunks = []
      end_mark = start_mark = get_mark
      indent = @indent+1
      # We allow zero indentation for scalars, but then we need to check for
      # document separators at the beginning of the line.
      #if indent == 0
      #    indent = 1
      spaces = []
      if @flow_zero
        f_nzero, r_check = false, R_flowzero
      else
        f_nzero, r_check = true, R_flownonzero
      end

      while peek0 != ?#
        length = 0
        chunk_size = 32
        chunk_size += 32 until length = (r_check =~ prefix(chunk_size))
        ch = peek(length)
        if f_nzero && ch == ?: && !S4.include?(peek(length+1))
          forward(length)
          raise ScannerError.new("while scanning a plain scalar",start_mark,"found unexpected ':'",get_mark,"Please check http://pyyaml.org/wiki/YAMLColonInFlowContext for details.")
        end
        break if length == 0
        @allow_simple_key = false
        chunks += spaces
        chunks << prefix(length)
        forward(length)
        end_mark = get_mark
        spaces = scan_plain_spaces(indent, start_mark)
        break if !spaces || (@flow_zero && @column < indent)
      end
      return ScalarToken.new(chunks.to_s, true, start_mark, end_mark)
    end

    END_OR_START = /^(---|\.\.\.)[\0 \t\r\n\x85]$/
    def scan_plain_spaces(indent, start_mark)
      # See the specification for details.
      # The specification is really confusing about tabs in plain scalars.
      # We just forbid them completely. Do not use tabs in YAML!
      chunks = []
      length = 0
      length += 1 while peek(length) == 32
      whitespaces = prefix(length)
      forward(length)
      ch = peek0
      if FULL_LINEBR.include?(ch)
        line_break = scan_line_break
        @allow_simple_key = true
        return if END_OR_START =~ prefix(4)
        breaks = []
        while BLANK_OR_LINEBR.include?(peek0)
          if peek0 == 32
            forward1
          else
            breaks << scan_line_break
            return if END_OR_START =~ prefix(4)
          end
        end
        chunks << line_break
        chunks += breaks
      else
        chunks << whitespaces
      end
      chunks
    end

    
    def scan_tag_handle(name, start_mark)
      # See the specification for details.
      # For some strange reasons, the specification does not allow '_' in
      # tag handles. I have allowed it anyway.
      ch = peek0
      raise ScannerError.new("while scanning a #{name}", start_mark,"expected '!', but found #{ch}",get_mark) if ch != ?!
      length = 1
      ch = peek(length)
      if ch != 32
        while ALPHA_REG =~ ch.chr
          length += 1
          ch = peek(length)
        end
        if ch != ?!
          forward(length)
          raise ScannerError.new("while scanning a #{name}", start_mark,"expected '!', but found #{ch}",get_mark)
        end
        length += 1
      end
      value = prefix(length)
      forward(length)
      value
    end
    
    STRANGE_CHR = /[\]\[\-';\/?:@&=+$,.!~*()%\w]/
    def scan_tag_uri(name, start_mark)
      # See the specification for details.
      # Note: we do not check if URI is well-formed.
      chunks = []
      length = 0
      ch = peek(length)
      while  STRANGE_CHR =~ ch.chr
        if ch == ?%
          chunks << prefix(length)
          forward(length)
          length = 0
          chunks << scan_uri_escapes(name, start_mark)
        else
          length += 1
        end
        ch = peek(length)
      end
      if length!=0
        chunks << prefix(length)
        forward(length)
      end
      
      raise ScannerError.new("while parsing a #{name}", start_mark,"expected URI, but found #{ch}",get_mark) if chunks.empty?
      chunks.to_s
    end

    HEXA_REG = /[0-9A-Fa-f]/
    def scan_uri_escapes(name, start_mark)
      # See the specification for details.
      bytes = []
      mark = get_mark
      while peek0 == ?%
        forward1
        raise ScannerError.new("while scanning a #{name}", start_mark,"expected URI escape sequence of 2 hexdecimal numbers, but found #{peek1} and #{peek2}",get_mark) if HEXA_REG !~ peek1.chr || HEXA_REG !~ peek2.chr
        bytes << prefix(2).to_i(16).to_s
        forward2
      end
      bytes.to_s
    end

    RN = "\r\n"
    def scan_line_break
      # Transforms:
      #   '\r\n'      :   '\n'
      #   '\r'        :   '\n'
      #   '\n'        :   '\n'
      #   '\x85'      :   '\n'
      #   default     :   ''
      if FULL_LINEBR.include?(peek0)
        if prefix2 == RN
          forward2
        else
          forward1
        end
        return "\n"
      end
      ""
    end
  end
end

