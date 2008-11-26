module FFI
  def self.errno
    JRuby::FFI::LastError.error
  end
  def self.set_errno(error)
    JRuby::FFI::LastError.error = error
  end
end