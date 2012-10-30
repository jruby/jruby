describe :math_atanh_base, :shared => true do
  it "returns a float" do
    @object.send(@method, 0.5).should be_an_instance_of(Float)
  end

  it "returns the inverse hyperbolic tangent of the argument" do
    @object.send(@method, 0.0).should == 0.0
    @object.send(@method, -0.0).should == -0.0
    @object.send(@method, 0.5).should be_close(0.549306144334055, TOLERANCE)
    @object.send(@method, -0.2).should be_close(-0.202732554054082, TOLERANCE)
  end

  # These are quarantined because MRI's behavior here is completely confusing
  # between 1.8 and 1.9 and whether lib/complex.rb is loaded on 1.8.
  #
  # TODO: file a ticket
  quarantine! do
    it "raises a TypeError if the argument is nil" do
      lambda { @object.send(@method, nil) }.should raise_error(TypeError)
    end

    it "accepts any argument that can be coerced with Float()" do
      obj = mock("Float 0.5")
      obj.should_receive(:to_f).and_return(0.5)
      @object.send(@method, obj).should be_close(0.549306144334055, TOLERANCE)
    end

    ruby_version_is ""..."1.9" do
      it "raises an ArgumentError if the argument cannot be coerced with Float()" do
        lambda { @object.send(@method, "test") }.should raise_error(ArgumentError)
      end
    end

    ruby_version_is "1.9" do
      it "raises a TypeError if the argument cannot be coerced with Float()" do
        lambda { @object.send(@method, "test") }.should raise_error(TypeError)
      end
    end
  end

  # These are quarantined because MRI depends mostly on libc behavior, but
  # not entirely. For example, on OS X 10.5.8:
  #
  #   $ ruby1.8.7 -v -e 'p Math.atanh(1.0)'
  #   ruby 1.8.7 (2009-12-24 patchlevel 248) [i686-darwin9.8.0]
  #   Infinity
  #
  #   $ ruby1.9 -v -e 'p Math.atanh(1.0)'
  #   ruby 1.9.2dev (2010-02-17 trunk 26689) [i386-darwin9.8.0]
  #   -e:1:in `atanh': Numerical argument out of domain - atanh (Errno::EDOM)
  #           from -e:1:in `<main>'
  #
  # Since MRI is linking with the same libc on OS X (afaik), MRI's behavior
  # is not solely determined by what libc is doing. So, MRI needs to decide
  # what behavior is expected or state that the behavior is undefined.
  quarantine! do
    ruby_version_is ""..."1.9" do
      platform_is :darwin, :freebsd, :java do
        it "returns Infinity for 1.0" do
          @object.send(@method, 1.0).infinite?.should == 1
        end

        it "returns -Infinity for -1.0" do
          @object.send(@method, -1.0).infinite?.should == -1
        end
      end

      platform_is :windows, :openbsd do
        # jruby is cross-platform and behaves as :darwin above
        not_compliant_on :jruby do
          it "raises an Errno::EDOM if x == 1.0" do
            lambda { @object.send(@method, 1.0) }.should raise_error(Errno::EDOM)
          end

          it "raises an Errno::EDOM if x == -1.0" do
            lambda { @object.send(@method, -1.0) }.should raise_error(Errno::EDOM)
          end
        end
      end

      platform_is :linux do
        it "raises an Errno::ERANGE if x == 1.0" do
          lambda { @object.send(@method, 1.0) }.should raise_error(Errno::ERANGE)
        end

        it "raises an Errno::ERANGE if x == -1.0" do
          lambda { @object.send(@method, -1.0) }.should raise_error(Errno::ERANGE)
        end
      end
    end

    ruby_version_is "1.9" do
      platform_is_not :linux do
        it "raises an Errno::EDOM if x == 1.0" do
          lambda { @object.send(@method, 1.0) }.should raise_error(Errno::EDOM)
        end

        it "raises an Errno::EDOM if x == -1.0" do
          lambda { @object.send(@method, -1.0) }.should raise_error(Errno::EDOM)
        end
      end

      platform_is :linux do
        it "raises an Errno::ERANGE if x == 1.0" do
          lambda { @object.send(@method, 1.0) }.should raise_error(Errno::ERANGE)
        end

        it "raises an Errno::ERANGE if x == -1.0" do
          lambda { @object.send(@method, -1.0) }.should raise_error(Errno::ERANGE)
        end
      end
    end
  end
end

describe :math_atanh_private, :shared => true do
  it "is a private instance method" do
    Math.should have_private_instance_method(@method)
  end
end

describe :math_atanh_no_complex, :shared => true do
  ruby_version_is ""..."1.9" do
    it "raises an Errno::EDOM for arguments greater than 1.0" do
      lambda { @object.send(@method, 1.0 + Float::EPSILON)  }.should raise_error(Errno::EDOM)
    end

    it "raises an Errno::EDOM for arguments less than -1.0" do
      lambda { @object.send(@method, -1.0 - Float::EPSILON) }.should raise_error(Errno::EDOM)
    end
  end

  ruby_version_is "1.9" do
    it "raises an Math::DomainError for arguments greater than 1.0" do
      lambda { @object.send(@method, 1.0 + Float::EPSILON)  }.should raise_error(Math::DomainError)
    end

    it "raises an Math::DomainError for arguments less than -1.0" do
      lambda { @object.send(@method, -1.0 - Float::EPSILON) }.should raise_error(Math::DomainError)
    end
  end
end
