# TODO java.util.Comparator support?
JavaUtilities.add_proxy_extender JavaInterfaceExtender.new('java.util.Map') {
    def each(&block)
      entrySet.each { |pair| block.call(pair.key, pair.value) }
    end
  }
  
JavaUtilities.add_proxy_extender JavaInterfaceExtender.new('java.util.Set') {
    def each(&block)
      iter = iterator
      while iter.hasNext
        block.call(iter.next)
      end
    end
  }
JavaUtilities.add_proxy_extender JavaInterfaceExtender.new('java.lang.Comparable') {
    include Comparable
    def <=>(a)
      compareTo(a)
    end
  }

JavaUtilities.add_proxy_extender JavaInterfaceExtender.new('java.util.List') {
    include Enumerable

    def each(&block)
      0.upto(size-1) { |index| block.call(get(index)) }
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
    def construct()
      include_class 'java.util.ArrayList'

      ArrayList.new
    end
    def _wrap_yield(*args)
      p = yield(*args)
      p p
    end
  }