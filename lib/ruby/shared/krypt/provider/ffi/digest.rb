module Krypt::FFI
  
  ##
  # A Krypt::Digest implementation using FFI.
  #
  class Digest
    include Krypt::FFI::LibC

    ##
    # call-seq:
    #    Krypt::FFI::Digest.new(provider, name_or_oid) -> Digest
    #
    # Creates a Digest using a C struct krypt_provider as +provider+
    # argument. The +provider+ is typically obtained by a separate FFI
    # call to a publicly visible function offered by the implementation
    # of the krypt Provider C API. +name_or_oid+ can be either the name
    # of the digest algorithm to be used (e.g. "SHA1") or the OID String
    # uniquely identifying the digest algorithm.
    #
    def initialize(provider, type)
      unless (@handle = interface_for_name(provider, type))
        unless (@handle = interface_for_oid(provider, type))
          raise Krypt::Provider::ServiceNotAvailableError.new("Unknown digest algorithm: #{type}")
        end
      end
    end

    ##
    # call-seq:
    #    digest.reset -> self
    #
    # Resets the Digest in the sense that any Digest#update that has been
    # performed is abandoned and the Digest is set to its initial state again.
    #
    def reset
      result = @handle.interface[:md_reset].call(@handle.container)
      raise_on_error("Error while resetting digest", result)
      self
    end

    ## 
    # call-seq:
    #    digest.update(string) -> aString
    #
    # Not every message digest can be computed in one single pass. If a message
    # digest is to be computed from several subsequent sources, then each may
    # be passed individually to the Digest instance.
    #
    # === Example
    #   
    #  digest = Krypt::Digest::SHA256.new
    #  digest.update('First input')
    #  digest << 'Second input' # equivalent to digest.update('Second input')
    #  result = digest.digest
    #
    def update(data)
      result = @handle.interface[:md_update].call(@handle.container, data, data.length)
      raise_on_error("Error while updating digest", result)
      self
    end
    alias << update

    ##
    # call-seq:
    #    digest.digest([string]) -> String
    #
    # When called with no arguments, the result will be the hash of the data that
    # has been fed to this Digest instance so far. If called with a String
    # argument, the hash of that argument will be computed.
    #
    # === Example
    #
    #  digest = Krypt::Digest::SHA256.new
    #  result = digest.digest('First input')
    #
    # is equivalent to
    #   
    #  digest = Krypt::Digest::SHA256.new
    #  digest << 'First input' # equivalent to digest.update('Second input')
    #  result = digest.digest
    #
    def digest(data=nil)
      if data
        ret = digest_once(data)
      else
        ret = digest_finalize
      end
      reset
      ret
    end

    ## 
    # call-seq:
    #    digest.hexdigest([string]) -> String
    #
    # Works the with the same semantics as Digest#digest with the difference that
    # instead of the raw bytes the hex-encoded form of the raw representation is
    # returned.
    #
    def hexdigest(data=nil)
      Krypt::Hex.encode(digest(data))
    end

    ##
    # call-seq:
    #    digest.digest_length -> integer
    #
    # Returns the output size of the digest, i.e. the length in bytes of the
    # final message digest result.
    #
    # === Example
    #  digest = Krypt::Digest::SHA1.new
    #  puts digest.digest_length # => 20
    #
    def digest_length
      read_length(@handle.interface[:md_digest_length])
    end

    ##
    # call-seq:
    #    digest.block_length -> integer
    #
    # Returns the block length of the digest algorithm, i.e. the length in bytes
    # of an individual block. Most modern algorithms partition a message to be
    # digested into a sequence of fix-sized blocks that are processed
    # consecutively.
    #
    # === Example
    #  digest = Krypt::Digest::SHA1.new
    #  puts digest.block_length # => 64
    #
    def block_length
      read_length(@handle.interface[:md_block_length])
    end

    ##
    # call-seq:
    #    digest.name -> string
    #
    # Returns the sn of this Digest instance.
    #
    # === Example
    #
    #  digest = Krypt::Digest::SHA512.new
    #  puts digest.name # => SHA512
    #
    def name
      name_ptr = FFI::MemoryPointer.new(:pointer)
      result = @handle.interface[:md_name].call(@handle.container, name_ptr)
      raise_on_error("Error while obtaining digest name", result)

      name_ptr.read_pointer.get_string(0)
    end

    private

      def raise_on_error(msg, result)
        raise Krypt::Digest::DigestError.new(msg) unless result == Krypt::FFI::ProviderAPI::KRYPT_OK
      end

      def digest_once(data)
        digest_ptr = ::FFI::MemoryPointer.new(:pointer)
        size_ptr = ::FFI::MemoryPointer.new(:pointer)
        result = @handle.interface[:md_digest].call(@handle.container, data, data.length, digest_ptr, size_ptr)
        raise_on_error("Error while computing digest", result)

        digest_ptr = digest_ptr.read_pointer
        size = size_ptr.read_int
        ret = digest_ptr.get_bytes(0, size)
        free(digest_ptr)
        ret
      end

      def digest_finalize
        digest_ptr = ::FFI::MemoryPointer.new(:pointer)
        size_ptr = ::FFI::MemoryPointer.new(:pointer)
        result = @handle.interface[:md_final].call(@handle.container, digest_ptr, size_ptr)
        raise_on_error("Error while computing digest", result)

        digest_ptr = digest_ptr.read_pointer
        size = size_ptr.read_int
        ret = digest_ptr.get_bytes(0, size)
        free(digest_ptr)
        ret
      end

      def read_length(fp)
        size_ptr = ::FFI::MemoryPointer.new(:pointer)
        result = fp.call(@handle.container, size_ptr)
        raise_on_error("Error while obtaining block length", result)

        size_ptr.read_int
      end

      def interface_for_name(provider, name)
        digest_ctor = provider[:md_new_name]
        get_native_handle(provider, digest_ctor, name)
      end

      def interface_for_oid(provider, oid)
        digest_ctor = provider[:md_new_oid]
        get_native_handle(provider, digest_ctor, oid)
      end

      def get_native_handle(provider, digest_ctor, type)
        container_ptr = digest_ctor.call(provider, type)
        return nil if nil == container_ptr || container_ptr.null?

        container = Krypt::FFI::ProviderAPI::KryptMd.new(container_ptr)
        interface_ptr = container[:methods]
        interface = Krypt::FFI::ProviderAPI::DigestInterface.new(interface_ptr)
        NativeHandle.new(container, interface)
      end

    class NativeHandle #:nodoc:
      attr_reader :container
      attr_reader :interface

      def initialize(container, interface)
        @container = container
        @interface = interface
      end
    end

  end
end
