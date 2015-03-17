# -*- encoding: us-ascii -*-
require File.expand_path('../../../../spec_helper', __FILE__)
require 'zlib'

describe "Zlib::Deflate#set_dictionary" do
  it "sets the dictionary" do
    d = Zlib::Deflate.new
    d.set_dictionary 'aaaaaaaaaa'
    d << 'abcdefghij'

    d.finish.should ==
      "x\273\024\341\003\313KLJNIMK\317\310\314\002\000\025\206\003\370"
  end
end

