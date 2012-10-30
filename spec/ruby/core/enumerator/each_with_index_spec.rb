require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7" do
  require File.expand_path('../../../shared/enumerator/with_index', __FILE__)

  describe "Enumerator#each_with_index" do
    it_behaves_like(:enum_with_index, :each_with_index)

    it "raises an ArgumentError if passed extra arguments" do
      lambda do
        [1].to_enum.each_with_index(:glark)
      end.should raise_error(ArgumentError)
    end

    it "passes on the given block's return value" do
      arr = [1,2,3]
      arr.delete_if.with_index { |a,b| false }
      arr.should == [1,2,3]
    end

    it "returns the iterator's return value" do
      [1,2,3].select.with_index { |a,b| false }.should == []
    end
  end
end

describe "Enumerator#each_with_index" do
  it "returns the correct value if chained with itself" do
    [:a].each_with_index.each_with_index.to_a.should == [[[:a,0],0]]
    [:a].each.with_index.with_index.to_a.should == [[[:a,0],0]]
  end
end
