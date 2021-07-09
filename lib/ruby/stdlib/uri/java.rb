module URI
  class Generic
    def to_java
      java.net.URI.new(self.to_s)
    end
  end
end
