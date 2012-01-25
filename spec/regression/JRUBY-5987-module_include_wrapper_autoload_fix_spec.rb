require 'rspec'

# Of course this comes from a real example...
module ActiveRecord
  module Associations
    autoload :HasOneThroughAssociation, File.expand_path('../has_one_through', __FILE__)
  end

  class Base
    include Associations
  end
end

class MyModel < ActiveRecord::Base
  def self.activate
    HasOneThroughAssociation
  end
end


describe "JRUBY-5987: Module include wrappers" do
  it "delegate to the included module for autoloads" do
    MyModel.activate.to_s.should == "ActiveRecord::Associations::HasOneThroughAssociation"
  end
end
