describe "The retry statement" do
  it "raises a LocalJumpError if used outside of a block" do
    def bad_meth_retry; retry; end
    lambda { bad_meth_retry()      }.should raise_error(LocalJumpError)
    lambda { lambda { retry }.call }.should raise_error(LocalJumpError)
  end

  # block retry has been officially deprecated by matz and is unsupported in 1.9
  not_compliant_on :rubinius, :jruby do
    it "re-executes the entire enumeration" do
      list = []
      [1,2,3].each do |x|
        list << x
        break if list.size == 6
        retry if x == 3
      end
      list.should == [1,2,3,1,2,3]
    end
  end
end
