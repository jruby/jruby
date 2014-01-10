describe "The break statement" do
  it "raises a LocalJumpError if used not within block or while/for loop" do
    def x; break; end
    lambda { x }.should raise_error(LocalJumpError)
  end
end
