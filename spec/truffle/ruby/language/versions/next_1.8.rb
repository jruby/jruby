describe "The next statement" do
  it "raises a LocalJumpError if used not within block or while/for loop" do
    def bad_meth; next; end
    lambda { bad_meth }.should raise_error(LocalJumpError)
  end
end
