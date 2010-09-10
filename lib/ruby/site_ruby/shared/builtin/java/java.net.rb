class java::net::URL
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
