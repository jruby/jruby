PETests.tests do

  describe "A boolean" do
    
    example "true literal" do
      Truffle::Debug.assert_constant true
    end

    example "false literal" do
      Truffle::Debug.assert_constant false
    end

  end

end
