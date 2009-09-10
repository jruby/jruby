module java::lang::Runnable
  def to_proc
    proc { self.run }
  end
end

module java::lang::Iterable
  include Enumerable

  def each
    iter = iterator
    yield(iter.next) while iter.hasNext
  end

  def each_with_index
    index = 0
    iter = iterator
    while iter.hasNext
      yield(iter.next, index)
      index += 1
    end
  end
end

module java::lang::Comparable
  include Comparable
  def <=>(a)
    return nil if a.nil?
    compareTo(a)
  end
end

class java::lang::Exception
  def self.===(rhs)
    (NativeException == rhs.class) && (java_class.assignable_from?(rhs.cause.java_class))
  end
end