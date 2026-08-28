describe "Proc#curry" do
  describe "when applied successively" do
    it "combines arguments and calculates incoming arity accurately" do
      l = lambda{|a,b,c| a+b+c }
      l1 = l.curry.call(1)
      # the l1 currying seems unnecessary, but it triggered the original issue
      l2 = l1.curry.call(2)

      expect(l2.curry.call(3)).to eq 6
      expect(l1.curry.call(2,3)).to eq 6
    end
  end
end unless RUBY_VERSION < '1.9'
