PETests.tests do

  describe "A Symbol" do
    
    example "literal" do
      Truffle::Debug.assert_constant :foo
    end
    
    example "#== a Symbol" do
      Truffle::Debug.assert_constant :foo == :foo
    end
    
    example "#!= a Symbol" do
      Truffle::Debug.assert_constant :foo != :bar
    end

  end

end
