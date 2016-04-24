# NOTE: these Ruby extensions were moved to native code!
# @see org.jruby.javasupport.ext.JavaUtilRegex.java
# this file is no longer loaded but is kept to provide doc stubs

# *java.util.regex.Pattern* enhanced to act similar to Ruby's `Regexp`.
# @note Only explicit (or customized) Ruby methods are listed, instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
class Java::java::util::regex::Pattern
  # Matches this pattern against provided string.
  # @return [Integer, nil] start (index) of the match if any
  def =~(str)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
    # m = matcher(str)
    # m.find ? m.start : nil
  end

  # Case equality for Java patterns.
  # @return [true, false]
  def ===(str)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
    # matcher(str).find
  end

  # Returns a `Matcher` object describing the match against the string (or nil if there was no match).
  # @example
  #    pattern = java.util.regex.Pattern.compile('[a-f]')
  #    matcher = pattern.match('abcdef') # java.util.regex.Matcher[pattern=[a-f] region=0,6 lastmatch=a]
  # @return [Java::java::util::regex::Matcher, nil]
  def match(str)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
    # m = matcher(str)
    # m.find ? m : nil
  end

  # Returns the value of the case-insensitive flag.
  # @return [true, false]
  # @since 9.1
  def casefold?
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end
end if false

# *java.util.regex.Matcher* represents a Java regex `Pattern` match, customized to quack like Ruby's `MatchData`.
# @note Only explicit (or customized) Ruby methods are listed, instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html
class Java::java::util::regex::Matcher
  # @private
  #attr_accessor :str

  # @return [Java::java::util::regex::Pattern]
  # @since 9.1
  def regexp
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  def string
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns an array of captures.
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    pattern.match('THX1138.').captures # ['H', 'X', '113', '8']
  # @return [Array]
  # @see #to_a
  def captures
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Matcher acts like an array and its capture elements might be accessed.
  # @example
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    matcher = pattern.match('THX1138.')
  #    expect( m[0] ).to eq 'HX1138'
  #    expect( m[1, 2] ).to eq ['H', 'X']
  #    expect( m[1..3] ).to eq ['H', 'X', '113']
  # @return [Array]
  # @see #to_a
  def [](*args)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns the array of matches.
  # @example
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    pattern.match('THX1138.').captures # ['HX1138', 'H', 'X', '113', '8']
  # @return [Array]
  def to_a
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  def values_at(*args)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns the number of elements in the match array.
  # @example
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    pattern.match('THX1138.').size # 5
  # @return [Integer]
  def size
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end
  alias length size

  # Returns the offset of the start of the n-th element of the match array in the string.
  # @example
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    matcher = pattern.match('THX1138.')
  #    expect( matcher.begin(0) ).to eq 1
  #    expect( matcher.begin(2) ).to eq 2
  # @param n can be a string or symbol to reference a named capture
  # @return [Integer]
  # @see #offset
  # @see #end
  # @note Named captures referencing is not available on Java 7.
  def begin(n)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns the offset of the character immediately following the end of the n-th element of the match array in the string.
  # @example
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    matcher = pattern.match('THX1138.')
  #    expect( matcher.begin(0) ).to eq 7
  #    expect( matcher.begin(2) ).to eq 3
  # @param n can be a string or symbol to reference a named capture
  # @return [Integer]
  # @see #offset
  # @see #begin
  # @note Named captures referencing is not available on Java 7.
  def end(n)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns the offset of the character immediately following the end of the n-th element of the match array in the string.
  # @example
  #    pattern = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
  #    matcher = pattern.match('THX1138.')
  #    expect( m.offset(0) ).to eq [1, 7]
  #    expect( m.offset(4) ).to eq [6, 7]
  #
  #    pattern = java.util.regex.Pattern.compile("(?<foo>.)(.)(?<bar>.)")
  #    matcher = pattern.match('hoge')
  #    expect( m.offset(:bar) ).to eq [2, 3]
  # @param n can be a string or symbol to reference a named capture
  # @return [Array]
  # @see #begin
  # @see #end
  def offset(n)
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns the portion of the original string before the current match.
  # @return [String]
  def pre_match
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

  # Returns the portion of the original string after the current match.
  # @return [String]
  def post_match
    # stub implemented in org.jruby.javasupport.ext.JavaUtilRegex.java
  end

end if false
