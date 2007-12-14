require 'java'

# Implement 1.9'S SecureRandom class in Java with java.security.SecureRandom.
class SecureRandom
  # SecureRandom.random_bytes generates a random binary string.
  #
  # The argument n specifies the length of the result string.
  #
  # If n is not specified, 16 is assumed.
  # It may be larger in future.
  #
  # If secure random number generator is not available,
  # NotImplementedError is raised.
  def self.random_bytes(n=nil)
    n ||= 16
    bytes = Java::byte[n].new
    java.security.SecureRandom.new.nextBytes(bytes)
    String.from_java_bytes bytes
  end

  # SecureRandom.hex generates a random hex string.
  #
  # The argument n specifies the length of the random length.
  # The length of the result string is twice of n.
  #
  # If n is not specified, 16 is assumed.
  # It may be larger in future.
  #
  # If secure random number generator is not available,
  # NotImplementedError is raised.
  def self.hex(n=nil)
    random_bytes(n).unpack("H*")[0]
  end

  # SecureRandom.base64 generates a random base64 string.
  #
  # The argument n specifies the length of the random length.
  # The length of the result string is about 4/3 of n.
  #
  # If n is not specified, 16 is assumed.
  # It may be larger in future.
  #
  # If secure random number generator is not available,
  # NotImplementedError is raised.
  def self.base64(n=nil)
    [random_bytes(n)].pack("m*").delete("\n")
  end

  # SecureRandom.random_number generates a random number.
  #
  # If an positive integer is given as n,
  # SecureRandom.random_number returns an integer:
  # 0 <= SecureRandom.random_number(n) < n.
  #
  # If 0 is given or an argument is not given,
  # SecureRandom.random_number returns an float:
  # 0.0 <= SecureRandom.random_number() < 1.0.
  def self.random_number(n=0)
    if n == 0
      java.security.SecureRandom.new.nextInt(n)
    else
      java.security.SecureRandom.new.nextDouble
    end
  end
end