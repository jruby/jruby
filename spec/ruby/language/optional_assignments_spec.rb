require File.expand_path('../../spec_helper', __FILE__)

describe 'Optional variable assignments' do
  describe 'using a single variable' do
    it 'assigns a new variable' do
      a ||= 10

      a.should == 10
    end

    it 're-assigns an existing variable set to false' do
      a = false
      a ||= 10

      a.should == 10
    end

    it 're-assigns an existing variable set to nil' do
      a = nil
      a ||= 10

      a.should == 10
    end

    it 'does not re-assign a variable with a truthy value' do
      a = 10
      a ||= 20

      a.should == 10
    end

    it 'does not re-assign a variable with a truthy value when using an inline rescue' do
      a = 10
      a ||= 20 rescue 30

      a.should == 10
    end
  end
end
