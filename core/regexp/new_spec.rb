require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/new_ascii', __FILE__)
require File.expand_path('../shared/new_ascii_8bit', __FILE__)

describe "Regexp.new" do
  it_behaves_like :regexp_new_ascii, :new
  it_behaves_like :regexp_new_ascii_8bit, :new
end

describe "Regexp.new given a String" do
  it_behaves_like :regexp_new_string_ascii, :new
  it_behaves_like :regexp_new_string_ascii_8bit, :new
end

describe "Regexp.new given a Regexp" do
  it_behaves_like :regexp_new_regexp_ascii, :new
  it_behaves_like :regexp_new_regexp_ascii_8bit, :new
end
