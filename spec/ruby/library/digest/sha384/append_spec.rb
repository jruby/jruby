require_relative '../../../spec_helper'
require_relative 'shared/constants'
require_relative 'shared/update'

describe "Digest::SHA384#<<" do
  it_behaves_like :sha384_update, :<<
end
