# enable to_java_bytes and from_java_bytes
class String
  def to_java_bytes
    JavaArrayUtilities.ruby_string_to_bytes(self)
  end

  def self.from_java_bytes(bytes)
    JavaArrayUtilities.bytes_to_ruby_string(bytes)
  end
end