class RubyTestObject
  attr_reader :number, :string, :list, :bool, :map, :object
  attr_writer :number, :string, :list, :bool, :map, :object

  def initialize
    @number = 0
    @string = @object = nil
    @list = []
    @bool = false
    @map = {}
  end

  def duplicate
    ret = RubyTestObject.new

    ret.number = self.number
    ret.string = self.string
    ret.object = self.object
    ret.list   = self.list
    ret.bool   = self.bool
    ret.map    = self.map

    return ret
  end

  def noArgs
  end

  def isSelf (who)
    return self == who
  end

  def to_s
    return "<" + self.string.to_s + ">"
  end

  def joinList
    return @list.join(",")
  end

  # The | and - operators used in these two methods operate on sets
  # and will implicitly remove duplicates from the list.

  def addToList (obj)
    @list |= [obj]
  end

  def removeFromList (obj)
    @list -= [obj]
  end

  # The Java "getNumberAs*" and "getListAs*" methods are mapped to the
  # number and list attributes via this method.  The reason that the
  # index calls don't look for "getNumber" and "getList" is that the
  # respondsTo call in RubyInvocationHandler will fail on these
  # methods because it does not take method_missing into
  # consideration.  Thus, it will attempt to strip off the get and
  # lowercase the first character.

  def method_missing (id)
    str = id.id2name
    
    if (str.index("number") == 0)
      return self.number
    elsif (str.index("list") == 0)
      return self.list
    elsif (str.index("string") == 0)
      return self.string
    elsif (str.index("object") == 0)
      return self.object
    elsif (str.index("bool") == 0)
      return self.bool
    elsif (str.index("map") == 0)
      return self.map
    else
      return super.method_missing(id)
    end
  end
end

class TestIntWrapper
  attr_reader :integer

  def initialize (i)
    @integer = i
  end

  def to_s
    return "<ruby: " + @integer + ">"
  end

  def equal (o)
	if (!o)
	return false
	end

    return self.integer == o.integer
  end
end

$global_test = RubyTestObject.new
$global_test.string = "global obj"
