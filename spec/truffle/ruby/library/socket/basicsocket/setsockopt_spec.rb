require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "BasicSocket#setsockopt" do

  before(:each) do
    @sock = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
  end

  after :each do
    @sock.close unless @sock.closed?
  end

  describe "using constants" do
    platform_is :windows do
      it "sets the socket linger to 0" do
        linger = pack_int(0, 0)
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER, linger).should == 0
        n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s

        n.should == pack_int(0)
      end

      it "sets the socket linger to some positive value" do
        linger = pack_int(64, 64)
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER, linger).should == 0
        n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s
        n.should == pack_int(64)
      end
    end
    platform_is_not :windows do
      it "sets the socket linger to 0" do
        linger = pack_int(0, 0)
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER, linger).should == 0
        n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s

        n.should == pack_int(0, 0)
      end

      it "sets the socket linger to some positive value" do
        linger = pack_int(64, 64)
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER, linger).should == 0
        n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s
        a = n.unpack('ii')
        a[0].should_not == 0
        a[1].should == 64
      end
    end

    it "sets the socket option Socket::SO_OOBINLINE" do
      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, true).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should_not == pack_int(0)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, false).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should == pack_int(0)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, 1).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should_not == pack_int(0)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, 0).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should == pack_int(0)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, 2).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should_not == pack_int(0)

      platform_is_not :os => :windows do
        lambda {
          @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, "")
        }.should raise_error(SystemCallError)
      end

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, "blah").should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should_not == pack_int(0)

      platform_is_not :os => :windows do
        lambda {
          @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, "0")
        }.should raise_error(SystemCallError)
      end

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, "\x00\x00\x00\x00").should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should == pack_int(0)

      platform_is_not :os => :windows do
        lambda {
          @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, "1")
        }.should raise_error(SystemCallError)
      end

      platform_is_not :os => :windows do
        lambda {
          @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, "\x00\x00\x00")
        }.should raise_error(SystemCallError)
      end

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, pack_int(1)).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should_not == pack_int(0)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, pack_int(0)).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should == pack_int(0)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE, pack_int(1000)).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
      n.should_not == pack_int(0)
    end

    it "sets the socket option Socket::SO_SNDBUF" do
      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, 4000).should == 0
      sndbuf = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      # might not always be possible to set to exact size
      sndbuf.unpack('i')[0].should >= 4000

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, true).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      n.unpack('i')[0].should >= 1

      lambda {
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, nil).should == 0
      }.should raise_error(TypeError)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, 1).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      n.unpack('i')[0].should >= 1

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, 2).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      n.unpack('i')[0].should >= 2

      lambda {
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, "")
      }.should raise_error(SystemCallError)

      lambda {
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, "bla")
      }.should raise_error(SystemCallError)

      lambda {
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, "0")
      }.should raise_error(SystemCallError)

      lambda {
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, "1")
      }.should raise_error(SystemCallError)

      lambda {
        @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, "\x00\x00\x00")
      }.should raise_error(SystemCallError)

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, "\x00\x00\x01\x00").should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      n.unpack('i')[0].should >= "\x00\x00\x01\x00".unpack('i')[0]

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, pack_int(4000)).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      n.unpack('i')[0].should >= 4000

      @sock.setsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF, pack_int(1000)).should == 0
      n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
      n.unpack('i')[0].should >= 1000
    end
  end

  ruby_version_is "1.9" do
    describe "using strings" do
      context "without prefix" do

        platform_is :windows do
          it "sets the socket linger to 0" do
            linger = pack_int(0, 0)
            @sock.setsockopt("SOCKET", "LINGER", linger).should == 0
            n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s

            n.should == pack_int(0)
          end

          it "sets the socket linger to some positive value" do
            linger = pack_int(64, 64)
            @sock.setsockopt("SOCKET", "LINGER", linger).should == 0
            n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s
            n.should == pack_int(64)
          end
        end
        platform_is_not :windows do
          it "sets the socket linger to 0" do
            linger = pack_int(0, 0)
            @sock.setsockopt("SOCKET", "LINGER", linger).should == 0
            n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s

            n.should == pack_int(0, 0)
          end

          it "sets the socket linger to some positive value" do
            linger = pack_int(64, 64)
            @sock.setsockopt("SOCKET", "LINGER", linger).should == 0
            n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s
            a = n.unpack('ii')
            a[0].should_not == 0
            a[1].should == 64
          end
        end

        it "sets the socket option Socket::SO_OOBINLINE" do
          @sock.setsockopt("SOCKET", "OOBINLINE", true).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should_not == pack_int(0)

          @sock.setsockopt("SOCKET", "OOBINLINE", false).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should == pack_int(0)

          @sock.setsockopt("SOCKET", "OOBINLINE", 1).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should_not == pack_int(0)

          @sock.setsockopt("SOCKET", "OOBINLINE", 0).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should == pack_int(0)

          @sock.setsockopt("SOCKET", "OOBINLINE", 2).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should_not == pack_int(0)

          platform_is_not :os => :windows do
            lambda {
              @sock.setsockopt("SOCKET", "OOBINLINE", "")
            }.should raise_error(SystemCallError)
          end

          @sock.setsockopt("SOCKET", "OOBINLINE", "blah").should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should_not == pack_int(0)

          platform_is_not :os => :windows do
            lambda {
              @sock.setsockopt("SOCKET", "OOBINLINE", "0")
            }.should raise_error(SystemCallError)
          end

          @sock.setsockopt("SOCKET", "OOBINLINE", "\x00\x00\x00\x00").should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should == pack_int(0)

          platform_is_not :os => :windows do
            lambda {
              @sock.setsockopt("SOCKET", "OOBINLINE", "1")
            }.should raise_error(SystemCallError)
          end

          platform_is_not :os => :windows do
            lambda {
              @sock.setsockopt("SOCKET", "OOBINLINE", "\x00\x00\x00")
            }.should raise_error(SystemCallError)
          end

          @sock.setsockopt("SOCKET", "OOBINLINE", pack_int(1)).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should_not == pack_int(0)

          @sock.setsockopt("SOCKET", "OOBINLINE", pack_int(0)).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should == pack_int(0)

          @sock.setsockopt("SOCKET", "OOBINLINE", pack_int(1000)).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
          n.should_not == pack_int(0)
        end

        it "sets the socket option Socket::SO_SNDBUF" do
          @sock.setsockopt("SOCKET", "SNDBUF", 4000).should == 0
          sndbuf = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          # might not always be possible to set to exact size
          sndbuf.unpack('i')[0].should >= 4000

          @sock.setsockopt("SOCKET", "SNDBUF", true).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          n.unpack('i')[0].should >= 1

          lambda {
            @sock.setsockopt("SOCKET", "SNDBUF", nil).should == 0
          }.should raise_error(TypeError)

          @sock.setsockopt("SOCKET", "SNDBUF", 1).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          n.unpack('i')[0].should >= 1

          @sock.setsockopt("SOCKET", "SNDBUF", 2).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          n.unpack('i')[0].should >= 2

          lambda {
            @sock.setsockopt("SOCKET", "SNDBUF", "")
          }.should raise_error(SystemCallError)

          lambda {
            @sock.setsockopt("SOCKET", "SNDBUF", "bla")
          }.should raise_error(SystemCallError)

          lambda {
            @sock.setsockopt("SOCKET", "SNDBUF", "0")
          }.should raise_error(SystemCallError)

          lambda {
            @sock.setsockopt("SOCKET", "SNDBUF", "1")
          }.should raise_error(SystemCallError)

          lambda {
            @sock.setsockopt("SOCKET", "SNDBUF", "\x00\x00\x00")
          }.should raise_error(SystemCallError)

          @sock.setsockopt("SOCKET", "SNDBUF", "\x00\x00\x01\x00").should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          n.unpack('i')[0].should >= "\x00\x00\x01\x00".unpack('i')[0]

          @sock.setsockopt("SOCKET", "SNDBUF", pack_int(4000)).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          n.unpack('i')[0].should >= 4000

          @sock.setsockopt("SOCKET", "SNDBUF", pack_int(1000)).should == 0
          n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
          n.unpack('i')[0].should >= 1000
        end
      end
    end
  end
end
