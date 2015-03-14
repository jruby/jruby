require "stringio"
require File.expand_path('../shared/read', __FILE__)
require File.expand_path('../shared/sysread', __FILE__)

ruby_version_is "1.9" do
  describe "StringIO#read_nonblock when passed length, buffer" do
    it_behaves_like :stringio_read, :read_nonblock
  end

  describe "StringIO#read_nonblock when passed length" do
    it_behaves_like :stringio_read_length, :read_nonblock
  end

  describe "StringIO#read_nonblock when passed no arguments" do
    it_behaves_like :stringio_read_no_arguments, :read_nonblock
  end

  describe "StringIO#read_nonblock when self is not readable" do
    it_behaves_like :stringio_read_not_readable, :read_nonblock
  end

  describe "StringIO#read_nonblock when passed nil" do
    it_behaves_like :stringio_read_nil, :read_nonblock
  end

  describe "StringIO#read_nonblock when passed length" do
    it_behaves_like :stringio_sysread_length, :read_nonblock
  end
end
