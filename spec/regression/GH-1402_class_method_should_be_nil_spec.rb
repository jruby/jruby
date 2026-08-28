describe "A call to __method__" do
  describe "Inside a class body within a method body" do
    it "returns nil" do
      def gh1402
        eval 'class GH1402; __method__; end'
      end
      expect(gh1402).to be_nil
    end
  end
end