require "stringio"
require File.expand_path('../shared/read', __FILE__)
require File.expand_path('../shared/sysread', __FILE__)

ruby_version_is "1.9" do
  describe "StringIO#readpartial when passed length, buffer" do
    it_behaves_like :stringio_read, :readpartial
  end

  describe "StringIO#readpartial when passed length" do
    it_behaves_like :stringio_read_length, :readpartial
  end

  describe "StringIO#readpartial when passed no arguments" do
    it_behaves_like :stringio_read_no_arguments, :readpartial
  end

  describe "StringIO#readpartial when self is not readable" do
    it_behaves_like :stringio_read_not_readable, :readpartial
  end

  describe "StringIO#readpartial when passed nil" do
    it_behaves_like :stringio_read_nil, :readpartial
  end

  describe "StringIO#readpartial when passed length" do
    it_behaves_like :stringio_sysread_length, :readpartial
  end
end
