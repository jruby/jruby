require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/new', __FILE__)

describe "Regexp.compile" do
  it_behaves_like :regexp_new, :compile
end

describe "Regexp.compile given a String" do
  it_behaves_like :regexp_new_string, :compile
end

describe "Regexp.compile given a Regexp" do
  it_behaves_like :regexp_new_regexp, :compile
end
