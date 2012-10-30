require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/join', __FILE__)
require File.expand_path('../shared/inspect', __FILE__)

describe "Array#to_s" do
  ruby_version_is "".."1.9" do
    it_behaves_like :array_join_with_default_separator, :to_s
  end

  ruby_version_is "1.9" do
    it_behaves_like :array_inspect, :to_s
  end
end
