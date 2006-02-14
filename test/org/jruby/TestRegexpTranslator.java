package org.jruby;

import junit.framework.TestCase;

public class TestRegexpTranslator extends TestCase {

	private RegexpTranslator translator;
	public void setUp() {
		translator = new RegexpTranslator();
	}

	public void testSpaceInClass() {
		verifyTranslation("[ ]", 		"[ ]", 		false);
		verifyTranslation("[\\x20]", 	"[ ]", 		true);
		verifyTranslation("[A\\x20]", 	"[A ]", 		true);
		verifyTranslation("[A\\x20B]", 	"[A B]", 		true);
		verifyTranslation(" [ ] ", 		" [ ] ", 	false);
		verifyTranslation(" [\\x20] ", 	" [ ] ", 	true);
	}

	public void testSharpInClass() {
		verifyTranslation("[#]", 		"[#]", 		false);
		verifyTranslation("[\\x23]", 	"[#]", 		true);
		verifyTranslation("[A\\x23]", 	"[A#]", 		true);
		verifyTranslation("[A\\x23B]", 	"[A#B]", 		true);
		verifyTranslation(" [#] ", 		" [#] ", 	false);
		verifyTranslation(" [\\x23] ", 	" [#] ", 	true);
	}

	public void testThreeDigitOctal() {
		verifyTranslation("\\0177", 	"\\177",false);
		verifyTranslation("\\0277", 	"\\277",false);
		verifyTranslation("\\0377", 	"\\377",false);
		verifyTranslation("\\0477", 		"\\477",false);
	}

	private void verifyTranslation(String expected, String rubyRE, boolean withComments) {
		String actual = translator.translatePattern(rubyRE, withComments);
		assertEquals(rubyRE + "; withComments=" + withComments, expected, actual);
	}
}
