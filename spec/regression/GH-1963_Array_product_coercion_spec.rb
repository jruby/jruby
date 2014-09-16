if RUBY_VERSION >= "1.9"
  describe "GH-1963: Array#product" do
    it "coerces Array-like objects that only define method_missing" do
      o = Object.new
      def o.method_missing(name, *args)
        [2]
      end

      expect([1].product(o)).to eq([[1,2]])
    end
  end
end
