5_000.times do |n|
  eval %{
    def foo#{n}(a, b, c)
      if a == b
        a + b + c # comments
      else
        a.foo + b.foo + c.foo
      end
    end
    
    class Foo#{n}
      def bar#{n}
        [#{n}, #{n} + 1, #{n} + 2]
      end
      
      def baz#{n}
        {a: #{n}, b: #{n + 1}, c: #{n + 2}}
      end
    end
  }
end
