require File.dirname(__FILE__) + '/../../spec_helper'

context "Ruby numbers in various ways" do

  specify "the standard way" do
    435.should == 435
  end

  specify "with underscore separations" do
    4_35.should == 435
  end

  specify "with some decimals" do
    4.35.should == 4.35
  end

  # TODO : find a better description
  specify "using the e expression" do
    1.2e-3.should == 0.0012
  end

  specify "the hexdecimal notation" do
    0xffff.should == 65535
  end

  specify "the binary notation" do
    0b01011.should == 11
  end

  specify "octal representation" do
    0377.should == 255
  end

  specify "character to numeric shortcut" do
    ?z.should == 122
  end

  specify "character with control character to numeric shortcut" do
    # Control-Z
    ?\C-z.should == 26

    # Meta-Z
    ?\M-z.should == 250

    # Meta-Control-Z
    ?\M-\C-z.should == 154
  end

end
