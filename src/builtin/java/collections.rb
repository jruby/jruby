# TODO java.util.Comparator support?
JavaUtilities.extend_proxy('java.util.Map') {
  include Enumerable
  def each(&block)
    entrySet.each { |pair| block.call(pair.key, pair.value) }
  end
  def [](key)
    get(key)
  end
  def []=(key,val)
    put(key,val)
    val
  end
}
  
JavaUtilities.extend_proxy('java.lang.Comparable') {
  include Comparable
  def <=>(a)
    return nil if a.nil?
    compareTo(a)
  end
}

JavaUtilities.extend_proxy('java.util.Collection') { 
  include Enumerable
  def each(&block)
    iter = iterator
    while iter.hasNext
      block.call(iter.next)
    end
  end
  def <<(a); add(a); self; end
  def +(oth)
    nw = self.dup
    nw.addAll(oth)
    nw
  end
  def -(oth)
    nw = self.dup
    nw.removeAll(oth)
    nw
  end
  def length
    self.size
  end
}


JavaUtilities.extend_proxy('java.util.List') {
  def [](ix)
    if ix < size
      get(ix)
    else
      nil
    end
  end
  def []=(ix,val)
    if size < ix
      ((ix-size)+1).times { self << nil }
    end
    set(ix,val)
    val
  end
  def sort()
    include_class 'java.util.ArrayList'
    include_class 'java.util.Collections'
    include_class 'java.util.Comparator'

    comparator = Comparator.new

    if block_given?
      class << comparator
        def compare(o1, o2); yield(o1, o2); end
      end
    else
      class << comparator
        def compare(o1, o2); o1 <=> o2; end
      end
    end

    list = ArrayList.new
    list.addAll(self)

    Collections.sort(list, comparator)

    list
  end
  def sort!()
    include_class 'java.util.Collections'
    include_class 'java.util.Comparator'
    comparator = Comparator.new
    if block_given?
      class << comparator
        def compare(o1, o2); yield(o1, o2); end
      end
    else
      class << comparator
        def compare(o1, o2); o1 <=> o2; end;
      end
    end

    Collections.sort(java_object, comparator)

    self
  end
  def _wrap_yield(*args)
    p = yield(*args)
    p p
  end
}
