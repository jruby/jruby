require File.expand_path('../../../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe 'Thread::Backtrace::Location#path' do
  context 'outside a main script' do
    it 'returns an absolute path' do
      frame = ThreadBacktraceLocationSpecs.locations[0]

      frame.path.should == __FILE__
    end
  end

  context 'in a main script' do
    before do
      @script = fixture(__FILE__, 'main.rb')
    end

    context 'when the script is in the working directory' do
      before do
        @directory = File.dirname(@script)
      end

      context 'when using a relative script path' do
        it 'returns a path relative to the working directory' do
          ruby_exe('main.rb', :dir => @directory).should == 'main.rb'
        end
      end

      context 'when using an absolute script path' do
        it 'returns an absolute path' do
          ruby_exe(@script, :dir => @directory).should == @script
        end
      end
    end

    context 'when the script is in a sub directory of the working directory' do
      context 'when using a relative script path' do
        it 'returns a path relative to the working directory' do
          path      = 'fixtures/main.rb'
          directory = File.dirname(__FILE__)

          ruby_exe(path, :dir => directory).should == path
        end
      end

      context 'when using an absolute script path' do
        it 'returns an absolute path' do
          ruby_exe(@script).should == @script
        end
      end
    end

    context 'when the script is outside of the working directory' do
      before do
        @parent_dir = tmp('path_outside_pwd')
        @sub_dir    = File.join(@parent_dir, 'sub')
        @script     = File.join(@parent_dir, 'main.rb')
        source      = fixture(__FILE__, 'main.rb')

        mkdir_p(@sub_dir)

        cp(source, @script)
      end

      after do
        rm_r(@script)
        rm_r(@sub_dir)
        rm_r(@parent_dir)
      end

      context 'when using a relative script path' do
        it 'returns a path relative to the working directory' do
          ruby_exe('../main.rb', :dir => @sub_dir).should == '../main.rb'
        end
      end

      context 'when using an absolute path' do
        it 'returns an absolute path' do
          ruby_exe(@script).should == @script
        end
      end
    end
  end
end
