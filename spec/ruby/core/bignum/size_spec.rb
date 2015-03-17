require File.expand_path('../../../spec_helper', __FILE__)

describe "Bignum#size" do
  ruby_version_is "2.1" do
    it "returns the number of bytes whose number of bytes is larger than the size of allocated binum data" do
      (256**7).size.should >= 8
      (256**8).size.should >= 9
      (256**9).size.should >= 10
      (256**10).size.should >= 10
      (256**10-1).size.should >= 9
      (256**11).size.should >= 12
      (256**12).size.should >= 13
      (256**20-1).size.should >= 20
      (256**40-1).size.should >= 40
    end
  end

  ruby_version_is ""..."2.1" do
    compliant_on :ironruby do
      it "returns the number of bytes in the machine representation in multiples of sizeof(BDIGIT) which is 4 where long long is 64 bit" do
        (256**7).size.should == 8
        (256**8).size.should == 12
        (256**9).size.should == 12
        (256**10).size.should == 12
        (256**10-1).size.should == 12
        (256**11).size.should == 12
        (256**12).size.should == 16
        (256**20-1).size.should == 20
        (256**40-1).size.should == 40
      end
    end

    deviates_on :rubinius, :jruby do
      it "returns the number of bytes in the machine representation" do
        (256**7).size   .should == 8
        (256**8).size   .should == 9
        (256**9).size   .should == 10
        (256**10).size  .should == 11
        (256**10-1).size.should == 10
        (256**11).size   .should == 12
        (256**12).size   .should == 13
        (256**20-1).size .should == 20
        (256**40-1).size .should == 40
      end
    end

    deviates_on :maglev do
      it "returns the number of bytes in the machine representation in multiples of four" do
        (256**7).size   .should ==  8
        (256**8).size   .should == 16
        (256**9).size   .should == 16
        (256**10).size  .should == 16
        (256**10-1).size.should == 16
        (256**11).size  .should == 16
        (256**12).size  .should == 20
        (256**20-1).size.should == 24
        (256**40-1).size.should == 44
      end
    end
  end
end
