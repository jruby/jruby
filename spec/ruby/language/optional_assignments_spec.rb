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

  describe 'using compunded constants' do
    before do
      Object.send(:remove_const, :A) if defined? Object::A
    end

    it 'with ||= assignments' do
      Object::A ||= 10
      Object::A.should == 10
    end

    it 'with ||= do not reassign' do
      Object::A = 20
      Object::A ||= 10
      Object::A.should == 20
    end

    it 'with &&= assignments' do
      Object::A = 20
      Object::A &&= 10
      Object::A.should == 10
    end

    it 'with &&= assignments will fail with non-existant constants' do
      lambda { Object::A &&= 10 }.should raise_error(NameError)
    end

    it 'with operator assignments' do
      Object::A = 20
      Object::A += 10
      Object::A.should == 30
    end

    it 'with operator assignments will fail with non-existant constants' do
      lambda { Object::A += 10 }.should raise_error(NameError)
    end
  end
end
