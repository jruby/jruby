module Krypt::FFI

  ##
  # A Krypt::Provider implementation for C-based implementations of the
  # krypt Provider C API (krypt-provider.h). Provides the necessary "glue"
  # to link the native implementation to the corresponding Ruby interfaces.
  #
  class Provider

    ##
    # call-seq: 
    #    Krypt::FFI::Provider.new(native_provider) -> Provider
    #
    # The +native_provider+ is typically obtained by a separate FFI
    # call to a publicly visible function offered by the implementation
    # of the krypt Provider C API. 
    #
    def initialize(native_provider)
      @provider = Krypt::FFI::ProviderAPI::ProviderInterface.new(native_provider)
      @provider[:init].call(@provider, nil)
    end

    ##
    # call-seq:
    #     provider.name -> String
    #
    # Every Provider has a default name identifying it.
    #
    def name
      @provider[:name]
    end

    ##
    # call-seq:
    #     provider.new_service(klass, [arg1, arg2, ...]) -> service
    #
    # Provides access to the individual services offered by this provider.
    # +klass+ is the Ruby class of the desired service (e.g. Krypt::Digest),
    # followed by optional additional arguments needed to create an instance
    # of the service.
    #
    # === Example
    #
    # digest = provider.new_service(Krypt::Digest, "SHA1")
    #
    def new_service(klass, *args)
      return new_digest(*args) if klass == Krypt::Digest
      nil
    end

    ##
    # call-seq:
    #    provider.finalize -> nil
    #
    # Depending on its implementation, it may be possible that a native krypt
    # provider needs to do some cleanup before it is subject to GC. This method
    # delegates to the native krypt_provider implementation of finalize. It is
    # called whenever a Provider::delete is called to remove a specific
    # provider. 
    #
    def finalize
      @provider[:finalize].call(@provider)
    end

    private
      
      def new_digest(name_or_oid)
        Krypt::FFI::Digest.new(@provider, name_or_oid)
      end

  end
end
