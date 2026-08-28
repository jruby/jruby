# NOTE: these Ruby extensions were moved to native code!
# @see org.jruby.javasupport.ext.JavaIo.java
# this file is no longer loaded but is kept to provide doc stubs

# Java *java.io.InputStream* objects are convertible to Ruby `IO`.
# @note Only explicit (or customized) Ruby methods are listed here,
#       instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html
class Java::java::io::InputStream
  # Convert a Java input stream to a Ruby `IO`.
  # @option opts [Types] autoclose changes `IO#autoclose=` if set
  # @return [IO]
  def to_io(opts = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaIo.java
  end
end if false

# Java *java.io.OutputStream* objects are convertible to Ruby `IO`.
# @note Only explicit (or customized) Ruby methods are listed here,
#       instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html
class Java::java::io::OutputStream
  # Convert a Java output stream to a Ruby `IO`.
  # @option opts [Types] autoclose changes `IO#autoclose=` if set
  # @return [IO]
  def to_io(opts = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaIo.java
  end
end if false

# Java channels (*java.nio.channels.Channel*) are convertible to Ruby `IO`.
# @note Only explicit (or customized) Ruby methods are listed here,
#       instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/nio/channels/Channel.html
module Java::java::nio::channels::Channel
  # Convert a Java channel to a Ruby `IO`.
  # @option opts [Types] autoclose changes `IO#autoclose=` if set
  # @return [IO]
  def to_io(opts = nil)
    # stub implemented in org.jruby.javasupport.ext.JavaIo.java
  end
end if false
