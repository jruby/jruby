describe "Colon2 lookup with changing receiver under concurrent load" do
  it "never fails to produce the correct value" do
    foo = Module.new do
      const_set :Bar, 1
    end
    
    baz = Module.new do
      const_set :Bar, 2
    end
    
    obj1 = Class.new do
      define_method :module do
        foo
      end
      def value
        1
      end
    end.new

    obj2 = Class.new do
      define_method :module do
        baz
      end
      def value
        2
      end
    end.new

    Thread.abort_on_exception = true

    obj3 = Object.new
    def obj3.casething(obj)
      raise "values did not match" unless obj.value == obj.module::Bar
    end

    # The logic here causes the same colon2 to flip between two different
    # target modules, which in JRuby 1.7.4 and lower could sometimes return
    # the wrong result due to a data race in the caching logic. This was fixed
    # in JRuby 1.7.5.
    ary = []
    50.times { ary << Thread.new { 10000.times { obj3.casething(obj1); obj3.casething(obj2) } } }

    lambda { ary.each(&:join) }.should_not raise_error
  end
end
