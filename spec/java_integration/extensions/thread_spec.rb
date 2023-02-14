require File.dirname(__FILE__) + "/../spec_helper"

describe "Thread" do

  context 'RubyThread' do

    it 'maintains compatibility with <= 9.3 when doing a default to_java conversion (till 9.6)' do
      if JRUBY_VERSION < '9.6'
        expect( Thread.current.to_java ).to be_a org.jruby.RubyThread
      else
        # NOTE: a naive attempt to get this looked into in 9.6 and channge the Thread#to_java default
        expect( Thread.current.to_java ).to be_a java.lang.Thread
        expect( Thread.current.to_java(java.lang.Object) ).to be java.lang.Thread.currentThread
      end
    end

    it 'is explicitly convertible to a java thread' do
      thread = Thread.start { sleep 1.0 }
      expect( thread.to_java(java.lang.Thread) ).to be_a java.lang.Thread
      expect( thread.to_java('java.lang.Runnable') ).to be_a java.lang.Thread
    end

    it 'can be converted to internal JRuby class' do
      expect( Thread.current.to_java('org.jruby.RubyThread') ).to be_a org.jruby.RubyThread
    end

  end

end
