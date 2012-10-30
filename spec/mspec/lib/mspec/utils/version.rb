class SpecVersion
  # If beginning implementations have a problem with this include, we can
  # manually implement the relational operators that are needed.
  include Comparable

  # SpecVersion handles comparison correctly for the context by filling in
  # missing version parts according to the value of +ceil+. If +ceil+ is
  # +false+, 0 digits fill in missing version parts. If +ceil+ is +true+, 9
  # digits fill in missing parts. (See e.g. VersionGuard and BugGuard.)
  def initialize(version, ceil = false)
    @version = version
    @ceil    = ceil
    @integer = nil
  end

  def to_s
    @version
  end

  def to_str
    to_s
  end

  # Converts a string representation of a version major.minor.tiny.patchlevel
  # to an integer representation so that comparisons can be made. For example,
  # "1.8.6.77" < "1.8.6.123" would be false if compared as strings.
  def to_i
    unless @integer
      major, minor, tiny, patch = @version.split "."
      if @ceil
        tiny = 99 unless tiny
        patch = 9999 unless patch
      end
      parts = [major, minor, tiny, patch].map { |x| x.to_i }
      @integer = ("1%02d%02d%02d%04d" % parts).to_i
    end
    @integer
  end

  def to_int
    to_i
  end

  def <=>(other)
    if other.respond_to? :to_int
      other = Integer other
    else
      other = SpecVersion.new(String(other)).to_i
    end

    self.to_i <=> other
  end
end
