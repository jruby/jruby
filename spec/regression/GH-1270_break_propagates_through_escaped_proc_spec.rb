describe "A break passing through an intermediate escaped closure" do
  it "does not turn into a LocalJumpError" do
    class Enumerator1270
      def initialize(&b)
        @g = Generator1270.new(&b)
      end

      def each
        @g.each do |x|
          yield x
        end
      end
    end

    class Generator1270
      def initialize(&b)
        @b = b
      end

      def each(&b)
        y = Yielder1270.new(&b)
        @b.call(y)
      end
    end

    class Yielder1270
      def initialize(&b)
        @b = b
      end

      def yield(x)
        @b.call(x)
      end
    end

    Enumerator1270.new {|y| y.yield 1}.each {break 1}.should == 1
  end
end