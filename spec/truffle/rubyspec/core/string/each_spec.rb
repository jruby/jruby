require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/each_line', __FILE__)

describe "String#each" do
  ruby_version_is ''...'1.9' do
    it_behaves_like(:string_each_line, :each)
  end
end
