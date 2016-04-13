# NOTE: these Ruby extensions were moved to native code!
# @see org.jruby.javasupport.ext.JavaIo.java
# this file is no longer loaded but is kept to provide doc stubs

class Java::java::net::URL
  def open(*rest, &block)
    stream = openStream
    io = stream.to_io
    if block
      begin
        block.call(io)
      ensure
        stream.close
      end
    else
      io
    end
  end
end
