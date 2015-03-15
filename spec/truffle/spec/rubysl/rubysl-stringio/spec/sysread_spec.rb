require "stringio"
require File.expand_path('../shared/read', __FILE__)
require File.expand_path('../shared/sysread', __FILE__)

describe "StringIO#sysread when passed length, buffer" do
  it_behaves_like :stringio_read, :sysread
end

describe "StringIO#sysread when passed length" do
  it_behaves_like :stringio_read_length, :sysread
end

describe "StringIO#sysread when passed no arguments" do
  it_behaves_like :stringio_read_no_arguments, :sysread
end

describe "StringIO#sysread when self is not readable" do
  it_behaves_like :stringio_read_not_readable, :sysread
end

describe "StringIO#sysread when passed nil" do
  it_behaves_like :stringio_read_nil, :sysread
end

describe "StringIO#sysread when passed length" do
  it_behaves_like :stringio_sysread_length, :sysread
end
