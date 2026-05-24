require 'rspec'

require 'ffi'
require 'ffi/io'

if RbConfig::CONFIG['host_os'] !~ /mingw|mswin/ 
  describe "JRUBY-5684: Piped file descriptors wrapped in an IO" do
    before :all do
      @libc = Module.new do
        extend FFI::Library
        ffi_lib FFI::Library::LIBC
        attach_function :_pipe, :pipe, [:buffer_in], :int

        def self.pipe
          ptr = FFI::MemoryPointer.new(:int, 2)
          ret = _pipe(ptr)
          if ret == 0
            r, w = ptr.read_array_of_int(2)
            [ FFI::IO.for_fd(r, 'r'), FFI::IO.for_fd(w, 'w') ]
          else
            raise SystemCallError.new(FFI::LastError.errno), 'pipe(2)'
          end
        end
      end
    end

    it "can write and read to and from the pipe" do
      r, w = @libc.pipe
      w.puts 'hi'
      w.close
      expect(r.read.chomp).to eq("hi")
    end
  end
end
