describe :string_each_line_without_block, :shared => true do
  it "returns an enumerator when no block given" do
    enum = "hello world".send(@method, ' ')
    enum.should be_an_instance_of(enumerator_class)
    enum.to_a.should == ["hello ", "world"]
  end
end
