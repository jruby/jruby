require File.expand_path('../shared/bol.rb', __FILE__)
require 'strscan'

describe "StringScanner#bol?" do
  it_behaves_like(:strscan_bol, :bol?)
end
