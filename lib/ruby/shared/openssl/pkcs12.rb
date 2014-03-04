require 'java'

module OpenSSL
  class PKCS12
    class PKCS12Error < OpenSSLError
    end

    java_import java.io.StringReader
    java_import java.io.StringBufferInputStream
    java_import java.io.ByteArrayOutputStream
    java_import org.bouncycastle.openssl.PEMReader
    java_import org.jruby.ext.openssl.SecurityHelper

    def self.create(pass, name, key, cert, ca = nil)
      pkcs12 = self.new
      pkcs12.generate(pass, name, key, cert, ca)
      pkcs12
    end

    attr_reader :key, :certificate, :ca_certs

    def initialize(str = nil, password = '')
      return @der = nil unless str

      if str.is_a?(File)
        file = File.open(str.path, "rb")
        @der = file.read
        file.close
      else
        str.force_encoding(Encoding::ASCII_8BIT)
        @der = str
      end

      p12_input_stream = StringBufferInputStream.new(@der)

      store = SecurityHelper.getKeyStore("PKCS12")
      store.load(p12_input_stream, password.to_java.to_char_array)

      aliases = store.aliases
      aliases.each do |alias_name|
        if store.is_key_entry(alias_name)
          java_certificate = store.get_certificate(alias_name)
          if java_certificate
            der = String.from_java_bytes(java_certificate.get_encoded)
            @certificate = OpenSSL::X509::Certificate.new(der)
          end

          java_key = store.get_key(alias_name, password.to_java.to_char_array)
          if java_key
            der = String.from_java_bytes(java_key.get_encoded)
            algorithm = java_key.get_algorithm
            if algorithm == "RSA"
              @key = OpenSSL::PKey::RSA.new(der)
            elsif algorithm == "DSA"
              @key = OpenSSL::PKey::DSA.new(der)
            elsif algorithm == "DH"
              @key = OpenSSL::PKey::DH.new(der)
            elsif algorithm == "EC"
              @key = OpenSSL::PKey::EC.new(der)
            else
              raise PKCS12Error, "Unknown key algorithm #{algorithm}"
            end
          end

          @ca_certs = Array.new
          java_ca_certs = store.get_certificate_chain(alias_name)
          if java_ca_certs
            java_ca_certs.each do |java_ca_cert|
                der = String.from_java_bytes(java_ca_cert.get_encoded)
                ruby_cert = OpenSSL::X509::Certificate.new(der)
                if (ruby_cert.to_pem != @certificate.to_pem)
                  @ca_certs << ruby_cert
                end
            end
          end
        end
        break
      end
    rescue java.lang.Exception => e
      raise PKCS12Error, "Exception: #{e}"
    end

    def generate(pass, alias_name, key, cert, ca = nil)
      @key = key
      @certificate = cert
      @ca_certs = ca

      key_reader = StringReader.new(key.to_pem)
      key_pair = PEMReader.new(key_reader).read_object

      certificates = cert.to_pem
      if ca
        ca.each { |ca_cert|
          certificates << ca_cert.to_pem
        }
      end

      cert_input_stream = StringBufferInputStream.new(certificates)
      certs = SecurityHelper.getCertificateFactory("X.509").generate_certificates(cert_input_stream)

      store = SecurityHelper.getKeyStore("PKCS12")
      store.load(nil, nil)
      store.set_key_entry(alias_name, key_pair.get_private, nil, certs.to_array(Java::java.security.cert.Certificate[certs.size].new))

      pkcs12_output_stream = ByteArrayOutputStream.new
      password = pass.nil? ? "" : pass;
      begin
        store.store(pkcs12_output_stream, password.to_java.to_char_array)
      rescue java.lang.Exception => e
        raise PKCS12Error, "Exception: #{e}"
      end

      @der = String.from_java_bytes(pkcs12_output_stream.to_byte_array)
    end

    def to_der
      @der
    end
  end
end
