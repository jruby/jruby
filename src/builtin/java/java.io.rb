require 'jruby'

class java::io::InputStream
  def to_io(opts = nil)
    ruby_io = org.jruby.RubyIO.new(JRuby.runtime, self)
    if opts && !opts[:autoclose]
      ruby_io.setAutoclose(false)
    end
    JRuby.dereference(ruby_io)
  end
end

class java::io::OutputStream
  def to_io(opts = nil)
    ruby_io = org.jruby.RubyIO.new(JRuby.runtime, self)
    if opts && !opts[:autoclose]
      ruby_io.setAutoclose(false)
    end
    JRuby.dereference(ruby_io)
  end
end

module java::nio::channels::Channel
  def to_io(opts = nil)
    ruby_io = org.jruby.RubyIO.new(JRuby.runtime, self)
    if opts && !opts[:autoclose]
      ruby_io.setAutoclose(false)
    end
    JRuby.dereference(ruby_io)
  end
end
