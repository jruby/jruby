# enable :to_java for arrays
class Array
  def to_java(type = (simple = true; nil), dimensions = (nodims = true; nil), fill = nil, &block)
    if simple
      JavaArrayUtilities.array_to_java_simple(self)
    elsif nodims
      JavaArrayUtilities.array_to_java_typed(self, type)
    else
      JavaArrayUtilities.ruby_to_java(self, type, dimensions, fill, &block)
    end
  end
end