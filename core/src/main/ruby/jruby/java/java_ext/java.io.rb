# NOTE: these Ruby extensions were moved to native code!
# @see org.jruby.javasupport.ext.JavaIo.java
# this file is no longer loaded but is kept to provide doc stubs

class Java::java::io::InputStream
  def to_io(opts = nil)
    ruby_io = org.jruby.RubyIO.new(JRuby.runtime, self)
    if opts && !opts[:autoclose]
      ruby_io.setAutoclose(false)
    end
    JRuby.dereference(ruby_io)
  end
end

class Java::java::io::OutputStream
  def to_io(opts = nil)
    ruby_io = org.jruby.RubyIO.new(JRuby.runtime, self)
    if opts && !opts[:autoclose]
      ruby_io.setAutoclose(false)
    end
    JRuby.dereference(ruby_io)
  end
end

module Java::java::nio::channels::Channel
  def to_io(opts = nil)
    ruby_io = org.jruby.RubyIO.new(JRuby.runtime, self)
    if opts && !opts[:autoclose]
      ruby_io.setAutoclose(false)
    end
    JRuby.dereference(ruby_io)
  end
end
