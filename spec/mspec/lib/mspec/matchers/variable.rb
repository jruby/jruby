require 'mspec/matchers/stringsymboladapter'

class VariableMatcher
  include StringSymbolAdapter

  class << self
    attr_accessor :variables_method, :description
  end

  def initialize(variable)
    @variable = convert_name(variable)
  end

  def matches?(object)
    @object = object
    @object.send(self.class.variables_method).include? @variable
  end

  def failure_message
    ["Expected #{@object} to have #{self.class.description} '#{@variable}'",
     "but it does not"]
  end

  def negative_failure_message
    ["Expected #{@object} NOT to have #{self.class.description} '#{@variable}'",
     "but it does"]
  end
end