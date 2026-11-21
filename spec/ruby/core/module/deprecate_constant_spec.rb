require_relative '../../spec_helper'

describe "Module#deprecate_constant" do
  before :each do
    @module = Module.new
    @value = :value
    @module::PUBLIC1 = @value
    @module::PUBLIC2 = @value
    @module::PRIVATE = @value
    @module.private_constant :PRIVATE
    @module.deprecate_constant :PRIVATE
  end

  describe "when accessing the deprecated module" do
    it "passes the accessing" do
      @module.deprecate_constant :PUBLIC1

      value = nil
      -> {
        value = @module::PUBLIC1
      }.should complain(/warning: constant .+::PUBLIC1 is deprecated/)
      value.should.equal?(@value)

      -> { @module::PRIVATE }.should.raise(NameError)
    end

    it "warns with a message" do
      @module.deprecate_constant :PUBLIC1

      -> { @module::PUBLIC1 }.should complain(/warning: constant .+::PUBLIC1 is deprecated/)
      -> { @module.const_get :PRIVATE }.should complain(/warning: constant .+::PRIVATE is deprecated/)
    end

    it "does not warn if Warning[:deprecated] is false" do
      @module.deprecate_constant :PUBLIC1

      deprecated = Warning[:deprecated]
      begin
        Warning[:deprecated] = false
        -> { @module::PUBLIC1 }.should_not complain
      ensure
        Warning[:deprecated] = deprecated
      end
    end
  end

  ruby_bug '#20900', ''...'3.4' do
    describe "when removing the deprecated module" do
      it "warns with a message" do
        @module.deprecate_constant :PUBLIC1
        -> { @module.module_eval {remove_const :PUBLIC1} }.should complain(/warning: constant .+::PUBLIC1 is deprecated/)
      end
    end
  end

  describe "when reassigning the deprecated constant" do
    it "warns that it is already initialized but not that it is deprecated" do
      @module.deprecate_constant :PUBLIC1

      deprecated = Warning[:deprecated]
      begin
        Warning[:deprecated] = true
        -> {
          @module.const_set :PUBLIC1, :new_value
        }.should complain(/\A(?!.*is deprecated).*already initialized constant .+::PUBLIC1/m)
      ensure
        Warning[:deprecated] = deprecated
      end
    end

    it "keeps the constant deprecated" do
      @module.deprecate_constant :PUBLIC1
      -> { @module.const_set :PUBLIC1, :new_value }.should complain(/already initialized constant/)

      -> { @module::PUBLIC1 }.should complain(/warning: constant .+::PUBLIC1 is deprecated/)
    end
  end

  describe "when changing the visibility of the deprecated constant" do
    it "keeps the constant deprecated after private_constant" do
      @module.deprecate_constant :PUBLIC1
      @module.private_constant :PUBLIC1

      -> { @module.const_get :PUBLIC1 }.should complain(/warning: constant .+::PUBLIC1 is deprecated/)
    end

    it "keeps the constant deprecated after public_constant" do
      @module.public_constant :PRIVATE

      -> { @module::PRIVATE }.should complain(/warning: constant .+::PRIVATE is deprecated/)
    end
  end

  it "accepts multiple symbols and strings as constant names" do
    @module.deprecate_constant "PUBLIC1", :PUBLIC2

    -> { @module::PUBLIC1 }.should complain(/warning: constant .+::PUBLIC1 is deprecated/)
    -> { @module::PUBLIC2 }.should complain(/warning: constant .+::PUBLIC2 is deprecated/)
  end

  it "returns self" do
    @module.deprecate_constant(:PUBLIC1).should.equal?(@module)
  end

  it "raises a NameError when given an undefined name" do
    -> { @module.deprecate_constant :UNDEFINED }.should.raise(NameError)
  end
end
