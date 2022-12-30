require File.dirname(__FILE__) + "/../spec_helper"

describe "Thread" do

  context 'RubyThread' do

    it 'is (default) convertible to a java thread' do
      expect( Thread.current.to_java ).to be_a java.lang.Thread
      expect( Thread.current.to_java ).to be java.lang.Thread.currentThread
    end

    it 'is explicitly convertible to a java thread' do
      thread = Thread.start { sleep 1.0 }
      expect( thread.to_java(java.lang.Thread) ).to be_a java.lang.Thread
      expect( thread.to_java('java.lang.Object') ).to be_a java.lang.Thread
    end

    it 'can be converted to internal JRuby class' do
      expect( Thread.current.to_java('org.jruby.RubyThread') ).to be_a org.jruby.RubyThread
    end

  end

end
