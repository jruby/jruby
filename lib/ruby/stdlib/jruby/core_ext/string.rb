
class String
  
  # Construct a new string with a buffer of the specified size. The buffer is
  # filled with null bytes to start.
  # 
  # May be useful in cases where you know how large a string will grow, and want
  # to pre-allocate the buffer for that size.
  #
  # @deprecated use String.new(capacity: size)
  def self.alloc(size)
    new(capacity: size)
  end
end