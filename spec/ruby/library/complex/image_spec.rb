require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/complex/image', __FILE__)

require 'complex'

describe "Complex#image" do
  it_behaves_like(:complex_image, :image)
end
