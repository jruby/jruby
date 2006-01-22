# Slow pure-ruby version of strscan
# This is missing many strscan functions.  The only implemented methods are the ones which
# erb uses.

class StringScanner
  def initialize(string)
    @string = string
    @position = 0
  end

  def scan(regexp)
    sub_string = @string.slice(@position..-1)
    regexp = Regexp.new(regexp) unless regexp.kind_of? Regexp
    regexp = Regexp.new("^" + regexp.source)
    @match_data = regexp.match(sub_string)
    return nil if @match_data.nil?
    @position = @position + @match_data.end(0)
    @match_data[0]
  end

  def [](i)
    @match_data[i]
  end

  def eos?
    @position >= @string.length
  end
end
