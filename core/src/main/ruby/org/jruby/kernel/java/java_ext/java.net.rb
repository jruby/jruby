# NOTE: these Ruby extensions were moved to native code!
# - **org.jruby.javasupport.ext.JavaNet.java**
# this file is no longer loaded but is kept to provide doc stubs

# *java.net.URL* extensions.
# @note Only explicit (or customized) Ruby methods are listed here,
#       instances will have all of their Java methods available.
# @see http://docs.oracle.com/javase/8/docs/api/java/net/URL.html
class Java::java::net::URL
  # Open the URL stream and yield it as a Ruby `IO`.
  # @return [IO] if no block given, otherwise yielded result
  def open(&block)
    # stub implemented in org.jruby.javasupport.ext.JavaNet.java
    # stream = openStream
    # io = stream.to_io
    # if block
    #   begin
    #     block.call(io)
    #   ensure
    #     stream.close
    #   end
    # else
    #   io
    # end
  end
end if false
