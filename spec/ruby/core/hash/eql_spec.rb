require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/eql', __FILE__)

# Do not use #should_receive(:eql?) mocks in these specs
# because MSpec uses Hash for mocks and Hash calls #eql?.

describe "Hash#eql?" do
  it_behaves_like :hash_eql, :eql?
  it_behaves_like :hash_eql_additional, :eql?
  it_behaves_like :hash_eql_additional_more, :eql?
end
