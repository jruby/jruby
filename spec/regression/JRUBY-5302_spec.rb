describe "A Hash" do
  it "yields k, v to a map() block receiving k, v" do
    hash = {1 => 2}
    mapped = hash.map {|k, v| "#{k} #{v}"}
    mapped.should == ["1 2"]
  end
end
