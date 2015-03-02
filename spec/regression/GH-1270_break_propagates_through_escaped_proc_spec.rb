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

    # This is hacked a bit because the single-method case is not currently fixable in 1.7:
    #   Enumerator1270.new {|y| y.yield 1}.each {break 1}
    # This case shares a frame -- and therefore a jumpTarget -- which causes the escaped
    # Generator proc to prematurely turn the break into a LocalJumpError. We only have the
    # one identifier for non-local jumps, so there's no easy way to fix this at the
    # moment.
    #
    # JRuby 9k avoids this by associating jumps with the originating closure rather than
    # the target method. The single-line version works correctly in JRuby 9k.
    obj = Object.new
    def obj.make_enum
      Enumerator1270.new {|y| y.yield 1}
    end
    def obj.main
      make_enum.each {break 1}
    end
    obj.main.should == 1
  end
end