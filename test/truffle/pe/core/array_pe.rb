PETests.tests do

  describe "A small Array" do
    
    example "indexed by a constant" do
      array = [3, 1, 2]
      Truffle::Debug.assert_constant array[1]
    end
    
    example "sorted and indexed" do
      array = [3, 1, 2]
      sorted = array.sort
      Truffle::Debug.assert_constant sorted[1]
    end

  end

end
