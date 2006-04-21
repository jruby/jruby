# Slow pure-ruby version of strscan
# This is missing many strscan functions.  The only implemented methods are the ones which
# erb uses.

class StringScanner
  attr_reader :matched, :pre_match, :post_match
  
  def initialize(string)
    @string = string
    @position = 0
  end
  
  def check(regexp)
    sub_string = @string.slice(@position..-1)
    regexp = Regexp.new(regexp) unless regexp.kind_of? Regexp
    regexp = Regexp.new("^" + regexp.source)
    @match_data = regexp.match(sub_string)
    if @match_data.nil?
      @pre_match, @matched, @post_match = nil, nil, nil
	  return nil 
    end
	@pre_match, @matched, @post_match = @match_data.pre_match, @match_data[0], @match_data.post_match
    @match_data[0]
  end
  
  def getch
    a = @string[@position..@position]
    @position = @position + 1 unless eos?
    @matched = a == "" ? nil : a
    @pre_match = @string.slice(0, @position-1)
    @matched
  end
  
  def pos
    @position
  end
  
  def pos=(position)
    @position = position
  end
  
  def rest
  	@string.slice(@position..-1)
  end

  def scan(regexp)
    sub_string = @string.slice(@position..-1)
    regexp = Regexp.new(regexp) unless regexp.kind_of? Regexp
    regexp = Regexp.new("^" + regexp.source)
    @match_data = regexp.match(sub_string)
    if @match_data.nil?
      @pre_match, @matched, @post_match = nil, nil, nil
      return nil 
    end
    
	@pre_match, @matched, @post_match = @match_data.pre_match, @match_data[0], @match_data.post_match
    @position = @position + @match_data.end(0)
    @matched
  end
  
  def scan_until(regexp)
    sub_string = @string.slice(@position..-1)
    regexp = Regexp.new(regexp) unless regexp.kind_of? Regexp
    @match_data = regexp.match(sub_string)
    if @match_data.nil?
      @pre_match, @matched, @post_match = nil, nil, nil
	  return nil 
	end
	
	@pre_match, @matched, @post_match = @match_data.pre_match, @match_data[0], @match_data.post_match
    @position = @position + @match_data.end(0)
	@pre_match + @matched
  end
  
  def skip(regexp)
    sub_string = @string.slice(@position..-1)
    regexp = Regexp.new(regexp) unless regexp.kind_of? Regexp
    regexp = Regexp.new("^" + regexp.source)
    @match_data = regexp.match(sub_string)
    if @match_data.nil?
      @pre_match, @matched, @post_match = nil, nil, nil
      return nil
    end
    
	@pre_match, @matched, @post_match = @match_data.pre_match, @match_data[0], @match_data.post_match
    @position = @position + @match_data.end(0)
    @match_data.end(0)
  end

  def [](i)
    @match_data[i]
  end
  
  def eos?
    @position >= @string.length
  end
end
