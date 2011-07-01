class WIN32OLE
  module VARIANT
    VT_I2 = 2 # Short
    VT_I4 = 3 # Int
    VT_R4 = 4 # Float
    VT_R8 = 5 # Double
    VT_CY = 6 # Currency
    VT_DATE = 7 # Date
    VT_BSTR = 8 # String
    VT_DISPATCH = 9 # Dispatch
    VT_ERROR = 10 # Error
    VT_BOOL = 11 # Boolean
    VT_VARIANT = 12 # Variant containing Variant
    VT_UNKNOWN = 13 # Unknown
    VT_DECIMAL = 14 # Decimal
    VT_I1 = 16 # Nothing in Jacob
    VT_UI1 = 17 # Byte
    VT_UI2 = 18 # Nothing in Jacob
    VT_UI4 = 19 # Nothing in Jacob
    VT_I8 = 20 # Not in MRI win32ole but in Jacob
    VT_UI8 = 21 # !Jacob
    VT_INT = 22 # Nothing in Jacob
    VT_UINT = 23 # Nothing in Jacob
    VT_VOID = 24 # !Jacob
    VT_HRESULT = 25 # !Jacob
    VT_PTR = 26 # Pointer
    VT_SAFEARRAY = 27 # !Jacob
    VT_CARRAY = 28 # !Jacob
    VT_USERDEFINED = 29 # !Jacob
    VT_LPSTR = 30 # !Jacob
    VT_LPWSTR = 31 # !Jacob
    VT_ARRAY = 8192 # Array
    VT_BYREF = 16384 # Reference

    VARIANTS = {
      VT_I2 => "I2", VT_I4 => "I4", VT_R4 => "R4", VT_R8 => "R8",
      VT_CY => "CY", VT_DATE => "DATE", VT_BSTR => "BSTR", VT_BOOL => "BOOL",
      VT_VARIANT => "VARIANT", VT_DECIMAL => "DECIMAL", VT_I1 => "I1",
      VT_UI1 => "UI1", VT_UI2 => "UI2", VT_UI4 => "UI4", VT_I8 => "I8",
      VT_UI8 => "UI8", VT_INT => "INT", VT_UINT => "UINT", VT_VOID => "VOID",
      VT_HRESULT => "HRESULT", VT_PTR => "PTR", VT_SAFEARRAY => "SAFEARRAY",
      VT_CARRAY => "CARRAY", VT_USERDEFINED => "USERDEFINED",
      VT_UNKNOWN => "UNKNOWN", VT_DISPATCH => "DISPATCH", VT_ERROR => "ERROR",
      VT_LPSTR => "LPSTR", VT_LPWSTR => "LPWSTR"
    }
    
    def variant_to_string(vt)
      VARIANTS[vt]
    end
    module_function :variant_to_string
  end
end
