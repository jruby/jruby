# TODO java.util.Comparator support?
JavaUtilities.extend_proxy('java.util.Map') {
  def each(&block)
    entrySet.each { |pair| block.call(pair.key, pair.value) }
  end
}
  
JavaUtilities.extend_proxy('java.util.Set') {
  def each(&block)
    iter = iterator
    while iter.hasNext
      block.call(iter.next)
    end
  end
}

JavaUtilities.extend_proxy('java.lang.Comparable') {
  include Comparable
  def <=>(a)
    compareTo(a)
  end
}

JavaUtilities.extend_proxy('java.util.List') {
  include Enumerable

  def each
# TODO: With 'def each(&block)' the following line will not work.
#      0.upto(size-1) { |index| block.call(get(index)) }
    0.upto(size-1) { |index| yield(get(index)) }
  end
  def <<(a); add(a); end
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