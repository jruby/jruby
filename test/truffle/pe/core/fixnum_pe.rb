PETests.tests do

  describe "A Fixnum" do
    
    example "literal" do
      Truffle::Debug.assert_constant 14
    end
    
    describe "#+" do
      
      example "a Fixnum" do
        Truffle::Debug.assert_constant 14 + 2
      end
      
      counter_example "a Bignum" do
        Truffle::Debug.assert_constant 14 + 0xfffffffffffffffffffffffffffffff
      end
      
      example "a Float" do
        Truffle::Debug.assert_constant 14 + 2.0
      end
      
      counter_example "rand" do
        Truffle::Debug.assert_constant 14 + rand
      end

    end
    
    describe "#*" do
      
      example "a Fixnum" do
        Truffle::Debug.assert_constant 14 * 2
      end
      
      counter_example "a Bignum" do
        Truffle::Debug.assert_constant 14 * 0xfffffffffffffffffffffffffffffff
      end
      
      example "a Float" do
        Truffle::Debug.assert_constant 14 * 2.0
      end
      
      counter_example "rand" do
        Truffle::Debug.assert_constant 14 * rand
      end

    end
    
    describe "#/" do
      
      example "a Fixnum" do
        Truffle::Debug.assert_constant 14 / 2
      end
      
      example "a Bignum" do
        Truffle::Debug.assert_constant 14 / 0xfffffffffffffffffffffffffffffff
      end
      
      example "a Float" do
        Truffle::Debug.assert_constant 14 / 2.0
      end
      
      counter_example "rand" do
        Truffle::Debug.assert_constant 14 / rand
      end

    end
    
    describe "#<=>" do
      
      example "a Fixnum" do
        Truffle::Debug.assert_constant 14 <=> 2
      end

    end

  end

end
