# coding: US-ASCII
require File.expand_path('../test_helper', File.dirname(__FILE__))
require 'jopenssl/load'

class TestX509Extension < Test::Unit::TestCase

  def test_attrs
    ext = OpenSSL::X509::Extension.new('1.1.1.1.1.1', 'foo')
    assert_equal false, ext.critical?

    ext.critical = nil
    assert_equal false, ext.critical?

    ext.critical = true
    assert_equal true, ext.critical?

    assert_equal 'foo', ext.value

    ext.value = 'bar'
    assert_equal 'bar', ext.value

    assert_equal '1.1.1.1.1.1', ext.oid

    # TODO ASN1 needs to fallback to ASN1Registry
    #ext.oid = '1.2'
    #assert_equal 'member-body', ext.oid
  end

  def test_to_der # reproducing #389
    ext = OpenSSL::X509::Extension.new('1.1.1.1.1.1', 'foo')

    mri_to_der = "0\f\x06\x05)\x01\x01\x01\x01\x04\x03foo"
    assert_equal mri_to_der, ext.to_der
    # MRI 1.8  "0\f\006\005)\001\001\001\001\004\003foo"
    # MRI 2.x  "0\f\x06\x05)\x01\x01\x01\x01\x04\x03foo"

    dec = OpenSSL::ASN1.decode(ext.to_der)

    assert_instance_of OpenSSL::ASN1::Sequence, dec
    assert_equal 2, ( value = dec.value ).size

    assert_instance_of OpenSSL::ASN1::ObjectId, value[0]
    # assert_equal 4, value[0].tag
    assert_equal '1.1.1.1.1.1', value[0].value

    assert_instance_of OpenSSL::ASN1::OctetString, value[1]
    # assert_equal 6, value[1].tag
    assert_equal 'foo', value[1].value
  end

  def test_to_der_is_the_same_for_non_critical
    ext1 = OpenSSL::X509::Extension.new('1.1.1.1.1.1', 'foo')
    ext2 = OpenSSL::X509::Extension.new('1.1.1.1.1.1', 'foo', false)

    assert_equal ext1.to_der, ext2.to_der

    ext1.critical = nil

    assert_equal ext1.to_der, ext2.to_der

    ext1.critical = false

    assert_equal ext1.to_der, ext2.to_der

    ext1.critical = true

    assert_not_equal ext1.to_der, ext2.to_der
  end

end