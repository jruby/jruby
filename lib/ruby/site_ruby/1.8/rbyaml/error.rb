
module RbYAML
  Mark = Struct.new(:name,:column,:buffer,:pointer)
  class Mark
    def get_snippet(indent=4, max_length=75)
      return nil if buffer.nil?
      head = ""
      start = pointer
      while start > 0 && !"\0\r\n\x85".include?(buffer[start-1])
        start -= 1
        if pointer-start > max_length/2-1
          head = " ... "
          start += 5
          break
        end
      end
      tail = ""
      tend = pointer
      while tend < buffer.length && !"\0\r\n\x85".include?(buffer[tend])
        tend += 1
        if tend-pointer > max_length/2-1
          tail = " ... "
          tend -= 5
          break
        end
      end
      snippet = buffer[start..tend]
      ' ' * indent + "#{head}#{snippet}#{tail}\n" + ' '*(indent+pointer-start+head.length) + ' '
    end
    
    def to_s
      snippet = get_snippet()
      where = "  in \"#{name}\", line ?, column #{column+1}"
      if snippet
        where << ":\n" << snippet
      end
    end

    def hash
      object_id
    end
  end
  
  class YAMLError < StandardError
  end

  class TypeError < YAMLError
  end
  
  class MarkedYAMLError < YAMLError
    def initialize(context=nil, context_mark=nil, problem=nil, problem_mark=nil, note=nil)
      super()
      @context = context
      @context_mark = context_mark
      @problem = problem
      @problem_mark = problem_mark
      @note = note
    end
    
    def to_s
      lines = []
      
      lines << @context if @context
      if @context_mark && (@problem.nil? || @problem_mark.nil? || 
                           @context_mark.name != @problem_mark.name ||
                           @context_mark.column != @problem_mark.column)
        lines << @context_mark.to_s          
      end
      lines << @problem if @problem
      lines << @problem_mark.to_s if @problem_mark
      lines << @note if @note
      lines.join("\n")
    end
  end
end
