describe "Dir" do
  # We have common option processing code and a logic mistake made
  # us process argv[2] as both a kwarg then try to convert it to an
  # integer.
  it "does not think the third arg should be an integer when it is kwargs" do
    Dir["*", "*", base: "."]
  end
end
