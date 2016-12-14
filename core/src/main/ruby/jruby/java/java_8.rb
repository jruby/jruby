# Extensions for Java 8

# @private shall be moved to Java when compiling against Java 8
org.jruby.RubyEnumerator.class_eval do

  def stream(parallel = false)
    java.util.stream.StreamSupport.stream spliterator, parallel
  end

  def spliterator(mod = nil)
    size = self.size
    # mod = java.util.Spliterator::NONNULL
    # we do not have ArrayNexter detection - assume immutable
    mod ||= java.util.Spliterator::IMMUTABLE
    mod ||= java.util.Spliterator::SIZED if size >= 0
    java.util.Spliterators.spliterator(self, size, mod)
  end

end