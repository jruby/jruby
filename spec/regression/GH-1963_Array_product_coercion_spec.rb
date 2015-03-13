if RUBY_VERSION >= "1.9"
  describe "GH-1963: Array#product" do
    it "coerces Array-like objects that define method_missing" do
      o = Object.new
      def o.method_missing(name, *args)
        [2]
      end

      expect([1].product(o)).to eq([[1,2]])
    end

    it "coerces Array-like objects that define to_ary" do
      o = Object.new
      def o.to_ary
        [2]
      end

      expect([1].product(o)).to eq([[1,2]])
    end

    it "does not coerce Array-like objects that define to_a" do
      o = Object.new
      def o.to_a
        [2]
      end

      expect {[1].product(o)}.to raise_exception(TypeError)
    end
  end
end
