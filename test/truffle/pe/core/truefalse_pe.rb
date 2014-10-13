PETests.tests do

  describe "A boolean" do
    
    example "true literal" do
      truffle_assert_constant true
    end

    example "false literal" do
      truffle_assert_constant false
    end

  end

end
