package org.jruby.ir.persistence.read.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.BIG5Encoding;
import org.jcodings.specific.Big5HKSCSEncoding;
import org.jcodings.specific.Big5UAOEncoding;
import org.jcodings.specific.CP1251Encoding;
import org.jcodings.specific.CP949Encoding;
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.EUCTWEncoding;
import org.jcodings.specific.EmacsMuleEncoding;
import org.jcodings.specific.GB18030Encoding;
import org.jcodings.specific.GBKEncoding;
import org.jcodings.specific.ISO8859_10Encoding;
import org.jcodings.specific.ISO8859_11Encoding;
import org.jcodings.specific.ISO8859_13Encoding;
import org.jcodings.specific.ISO8859_14Encoding;
import org.jcodings.specific.ISO8859_15Encoding;
import org.jcodings.specific.ISO8859_16Encoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jcodings.specific.ISO8859_2Encoding;
import org.jcodings.specific.ISO8859_3Encoding;
import org.jcodings.specific.ISO8859_4Encoding;
import org.jcodings.specific.ISO8859_5Encoding;
import org.jcodings.specific.ISO8859_6Encoding;
import org.jcodings.specific.ISO8859_7Encoding;
import org.jcodings.specific.ISO8859_8Encoding;
import org.jcodings.specific.ISO8859_9Encoding;
import org.jcodings.specific.KOI8Encoding;
import org.jcodings.specific.KOI8REncoding;
import org.jcodings.specific.KOI8UEncoding;
import org.jcodings.specific.NonStrictEUCJPEncoding;
import org.jcodings.specific.NonStrictUTF8Encoding;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.SpecializedInstType;
import org.jruby.ir.operands.OperandType;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.parser.StaticScope.Type;
import org.jruby.runtime.CallType;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;

public class NonIRObjectFactory {
    public static Operation createOperation(String name) {
        return Operation.valueOf(name.toUpperCase());
    }

    public static OperandType createOperandType(String name) {
        return OperandType.valueOf(name.toUpperCase());
    }

    public static SpecializedInstType createSpecilizedInstrType(String specializedInstName) {
        return SpecializedInstType.valueOf(specializedInstName);
    }

    public static IRScopeType createScopeType(String type) {
        return IRScopeType.valueOf(type);
    }

    public static ISourcePosition createSourcePosition(String fileName, int line) {
        return new SimpleSourcePosition(fileName, line);
    }

    public static Type createStaticScopeType(String type) {
        return Type.valueOf(type);
    }

    public static Encoding createEncoding(String name) {
        if(name == null) {
            return null;
        } else if (ASCIIEncoding.INSTANCE.toString().equals(name)) {
            return ASCIIEncoding.INSTANCE;
        } else if (USASCIIEncoding.INSTANCE.toString().equals(name)) {
            return USASCIIEncoding.INSTANCE;
        } else if (UTF8Encoding.INSTANCE.equals(name)) {
            return UTF8Encoding.INSTANCE;
        } else if(BIG5Encoding.INSTANCE.toString().equals(name)) {
            return BIG5Encoding.INSTANCE;
        } else if (Big5HKSCSEncoding.INSTANCE.toString().equals(name)) {
            return Big5HKSCSEncoding.INSTANCE;
        } else if (Big5UAOEncoding.INSTANCE.toString().equals(name)) {
            return Big5UAOEncoding.INSTANCE;
        } else if (NonStrictEUCJPEncoding.INSTANCE.toString().equals(name)) {
            return NonStrictEUCJPEncoding.INSTANCE;
        } else if (SJISEncoding.INSTANCE.toString().equals(name)) {
            return SJISEncoding.INSTANCE;
        } else if (CP949Encoding.INSTANCE.toString().equals(name)) {
            return CP949Encoding.INSTANCE;
        } else if (GBKEncoding.INSTANCE.toString().equals(name)) {
            return GBKEncoding.INSTANCE;
        } else if (EmacsMuleEncoding.INSTANCE.toString().equals(name)) {
            return EmacsMuleEncoding.INSTANCE;
        } else if (EUCJPEncoding.INSTANCE.toString().equals(name)) {
            return EUCJPEncoding.INSTANCE;
        } else if (EUCTWEncoding.INSTANCE.toString().equals(name)) {
            return EUCTWEncoding.INSTANCE;
        } else if (GB18030Encoding.INSTANCE.toString().equals(name)) {
            return GB18030Encoding.INSTANCE;
        } else if (NonStrictUTF8Encoding.INSTANCE.toString().equals(name)) {
            return NonStrictUTF8Encoding.INSTANCE;
        } else if (UTF32BEEncoding.INSTANCE.toString().equals(name)) {
            return UTF32BEEncoding.INSTANCE;
        } else if (UTF32LEEncoding.INSTANCE.toString().equals(name)) {
            return UTF32LEEncoding.INSTANCE;
        } else if (UTF16BEEncoding.INSTANCE.toString().equals(name)) {
            return UTF16BEEncoding.INSTANCE;
        } else if (UTF16LEEncoding.INSTANCE.toString().equals(name)) {
            return UTF16LEEncoding.INSTANCE;
        } else if (CP1251Encoding.INSTANCE.toString().equals(name)) {
            return CP1251Encoding.INSTANCE;
        } else if (UTF32BEEncoding.INSTANCE.toString().equals(name)) {
            return UTF32BEEncoding.INSTANCE;
        } else if (ISO8859_10Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_10Encoding.INSTANCE;
        } else if (ISO8859_11Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_11Encoding.INSTANCE;
        } else if (ISO8859_13Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_13Encoding.INSTANCE;
        } else if (ISO8859_14Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_14Encoding.INSTANCE;
        } else if (ISO8859_15Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_15Encoding.INSTANCE;
        } else if (ISO8859_16Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_16Encoding.INSTANCE;
        } else if (ISO8859_1Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_1Encoding.INSTANCE;
        } else if (ISO8859_2Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_2Encoding.INSTANCE;
        } else if (ISO8859_3Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_3Encoding.INSTANCE;
        } else if (ISO8859_4Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_4Encoding.INSTANCE;
        } else if (ISO8859_5Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_5Encoding.INSTANCE;
        } else if (ISO8859_6Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_6Encoding.INSTANCE;
        } else if (ISO8859_7Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_7Encoding.INSTANCE;
        } else if (ISO8859_8Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_8Encoding.INSTANCE;
        } else if (ISO8859_9Encoding.INSTANCE.toString().equals(name)) {
            return ISO8859_9Encoding.INSTANCE;
        } else if (KOI8Encoding.INSTANCE.toString().equals(name)) {
            return KOI8Encoding.INSTANCE;
        } else if (KOI8REncoding.INSTANCE.toString().equals(name)) {
            return KOI8REncoding.INSTANCE;
        } else if (KOI8UEncoding.INSTANCE.toString().equals(name)) {
            return KOI8UEncoding.INSTANCE;
        } else {
            return UTF8Encoding.INSTANCE;
        }
    }

    /**
     * RegexpOptions(kcode:$kcode(, encodingNone)?(, extended)?(, fixed)?(,
     * ignorecase)?(, java)?(, kcodeDefault)?(, literal)?(, multiline)?(,
     * once)?)
     */
    public static RegexpOptions createRegexpOptions(String kcodeString, String[] options) {
        KCode kCode = KCode.valueOf(kcodeString);

        if (options != null) {
            List<String> optionList = new ArrayList<String>(Arrays.asList(options));

            boolean isKCodeDefault = false;
            if (optionList.contains("kcodeDefault")) {
                isKCodeDefault = true;
                // already used
                optionList.remove("kcodeDefault");
            }
            RegexpOptions result = new RegexpOptions(kCode, isKCodeDefault);

            for (String option : optionList) {
                if ("encodingNone".equals(option)) {
                    result.setEncodingNone(true);
                } else if ("extended".equals(option)) {
                    result.setExtended(true);
                } else if ("fixed".equals(option)) {
                    result.setFixed(true);
                } else if ("ignorecase".equals(option)) {
                    result.setIgnorecase(true);
                } else if ("java".equals(option)) {
                    result.setJava(true);
                } else if ("literal".equals(option)) {
                    result.setLiteral(true);
                } else if ("multiline".equals(option)) {
                    result.setMultiline(true);
                } else if ("once".equals(option)) {
                    result.setOnce(true);
                }
            }

            return result;
        } else {
            return new RegexpOptions(kCode, false);
        }
    }

    public static CallType createCallType(String callTypeString) {
        return CallType.valueOf(callTypeString);
    }

    public static Reason createReason(String reasonString) {
        return Reason.valueOf(reasonString.toUpperCase());
    }

    public static KCode createKcode(String kcodeName) {
        return KCode.valueOf(kcodeName);
    }

}
