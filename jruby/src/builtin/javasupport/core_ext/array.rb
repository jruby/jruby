# enable :to_java for arrays
class Array
  def to_java(*args,&block)
    JavaArrayUtilities.ruby_to_java(*(args.unshift(self)),&block)
  end
end