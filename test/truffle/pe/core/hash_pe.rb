PETests.tests do

  describe "A small Hash" do
    
    example "indexed by a constant" do
      hash = {a: 0, b: 1, c: 2}
      Truffle::Debug.assert_constant hash[:b]
    end
    
    broken_example "mapped to an Array and indexed by a constant" do
      hash = {a: 0, b: 1, c: 2}
      array = hash.map { |k, v| v }
      Truffle::Debug.assert_constant array[0]
    end

  end

end
