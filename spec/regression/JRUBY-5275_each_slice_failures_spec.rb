describe "Enumerator\#each_slice" do
  describe "when called with no block" do
    describe "and .first" do
      it "returns the first slice" do
        expect([0, 1, 2, 3].each_slice(2).first).to eq([0, 1])
      end
    end

    describe "and .to_a" do
      it "returns the slices in an array" do
        expect([0, 1, 2, 3].each_slice(2).to_a).to eq([[0, 1], [2, 3]])
      end
    end

    describe "and .map{}" do
      it "yields the slices and produces mapped result" do
        expect([0, 1, 2, 3].each_slice(2).map{|a, b| [a, b]}).to eq([[0, 1], [2, 3]])
      end
    end
  end

  describe "called with a block" do
    describe "with n arguments" do
      it "yields the slice arrays' elements" do
        ary = []
        [0, 1, 2, 3].each_slice(2) {|a, b| ary << [a, b]}
        expect(ary).to eq([[0, 1], [2, 3]])
      end
    end
  end
end
