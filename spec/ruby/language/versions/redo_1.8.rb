describe "The redo statement" do
  it "raises a LocalJumpError if used not within block or while/for loop" do
    def bad_meth_redo; redo; end
    lambda { bad_meth_redo() }.should raise_error(LocalJumpError)
  end
end
