require File.expand_path('../../../spec_helper', __FILE__)
require 'stringio'
require File.expand_path('../shared/each_byte', __FILE__)

ruby_version_is "1.8.7" do
  describe "StringIO#bytes" do
    it_behaves_like :stringio_each_byte, :bytes
  end

  describe "StringIO#bytes when self is not readable" do
    it_behaves_like :stringio_each_byte_not_readable, :bytes
  end
end
