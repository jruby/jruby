/*
 *  gnu/regexp/util/REApplet.java
 *  Copyright (C) 1998 Wes Biggs
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published
 *  by the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package gnu.regexp.util;
import java.applet.*;
import java.awt.*;
import gnu.regexp.*;

/**
 * This is a simple applet to demonstrate the capabilities of gnu.regexp.
 * To run it, use appletviewer on the reapplet.html file included in the
 * documentation directory.
 *
 * @author <A HREF="mailto:wes@cacas.org">Wes Biggs</A>
 * @version 1.02
 */
public class REApplet extends Applet {
    private Label l1, l2, l3, l4;
    private Button b;
    private TextField tf;
    private TextArea input, output;
    private Checkbox insens;
    private Choice syntax;
    private static String[] names = new String[] { 
	"awk", "ed", "egrep", "emacs", "grep", "POSIX awk", "POSIX basic", 
	"POSIX egrep", "POSIX extended", "POSIX minimal basic", 
	"POSIX minimal extended", "sed", "perl 4", "perl 4 (singe line)", 
	"perl 5", "perl 5 (single line)" 
    };

    private static RESyntax[] values = new RESyntax[] { 
	new RESyntax(RESyntax.RE_SYNTAX_AWK).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_ED).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_EGREP).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_EMACS).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_GREP).setLineSeparator("\n"),
	new RESyntax(RESyntax.RE_SYNTAX_POSIX_AWK).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_POSIX_BASIC).setLineSeparator("\n"),
	new RESyntax(RESyntax.RE_SYNTAX_POSIX_EGREP).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_POSIX_EXTENDED).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_POSIX_MINIMAL_BASIC).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_POSIX_MINIMAL_EXTENDED).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_SED).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_PERL4).setLineSeparator("\n"),
	new RESyntax(RESyntax.RE_SYNTAX_PERL4_S).setLineSeparator("\n"), 
	new RESyntax(RESyntax.RE_SYNTAX_PERL5).setLineSeparator("\n"),
	new RESyntax(RESyntax.RE_SYNTAX_PERL5_S).setLineSeparator("\n")
    };

    /** Creates an REApplet. */
    public REApplet() { super(); }
    
    /** Initializes the applet and constructs GUI elements. */
    public void init() {
	// test run RE stuff to cache gnu.regexp.* classes.
	try {
	    RE x = new RE("^.*(w[x])\1$");
	    REMatchEnumeration xx = x.getMatchEnumeration("wxwx");
	    while (xx.hasMoreMatches()) xx.nextMatch().toString();
	} catch (REException arg) { }
	
	setBackground(Color.lightGray);
	
	/*
	  Layout looks like this:
	  
	  [0,0:[0,0: Regular Expression] [1,0: Textbox]
	  [0,1: Expression Syntax]  [1,1: [0,0: Choice] [1,0: Checkbox]]
	  [1,2: Button]]
	  [0,1: Input Text] [1,1: Match]
	  [0,2: Textarea]   [1,2: Textarea]
	*/
	
	GridBagLayout gbag = new GridBagLayout();
	setLayout(gbag);
	GridBagConstraints c = new GridBagConstraints();
	Panel p = new Panel();
	GridBagLayout gbag2 = new GridBagLayout();
	p.setLayout(gbag2);
	
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 1.0;
	
	// [0,0: Regular Expression]
	c.gridx = 0;
	c.gridy = 0;
	l1 = new Label("Regular Expression");
	gbag2.setConstraints(l1,c);
	p.add(l1);
	
	// [1,0: TextField]
	c.gridx = 1;
	tf = new TextField(getParameter("regexp"),30);
	gbag2.setConstraints(tf,c);
	p.add(tf);
	
	// [0,1: Expression Syntax]
	c.gridx = 0;
	c.gridy = 1;
	l4 = new Label("Expression Syntax");
	gbag2.setConstraints(l4,c);
	p.add(l4);
	
	// [1,1: subpanel]
	Panel p2 = new Panel();
	GridBagLayout gbag3 = new GridBagLayout();
	p2.setLayout(gbag3);
	c.gridx = 1;
	gbag2.setConstraints(p2,c);
	p.add(p2);
	
	// Subpanel [0,0: Choice]
	c.gridx = 0;
	c.gridy = 0;
	syntax = new Choice();
	for (int i = 0; i < names.length; i++) syntax.addItem(names[i]);
	String zz = getParameter("syntax");
	if (zz != null) {
	    try {
		syntax.select(getParameter("syntax"));
	    } catch (IllegalArgumentException e) { }
	}

	gbag3.setConstraints(syntax,c);
	p2.add(syntax);
        
	c.gridx = 1;
	insens = new Checkbox("Ignore case",false);
	gbag3.setConstraints(insens,c);
	p2.add(insens);
	
	// Next Row
	c.gridx = 1;
	c.gridy = 2;
	b = new Button("Match");
	gbag2.setConstraints(b,c);
	p.add(b);
	
	// Add the entire upper panel.
	c.gridwidth = 2;
	c.gridheight = 1;
	c.gridx = 0;
	c.gridy = 0;
	c.anchor = GridBagConstraints.CENTER;
	gbag.setConstraints(p,c);
	add(p);
	
	c.gridwidth = 1;
	c.gridheight = 1;
	
	// Main: [0,1]:
	l2 = new Label("Input Text");
	c.gridwidth = 1;
	c.gridx = 0;
	c.gridy = 1;
	gbag.setConstraints(l2,c);
	add(l2);
	
	l3 = new Label("Matches Found");
	c.gridx = 1;
	gbag.setConstraints(l3,c);
	add(l3);
	
	input = new TextArea(getParameter("input"),5,30);
	c.gridx = 0;
	c.gridy = 2;
	gbag.setConstraints(input,c);
	add(input);
	
	c.gridx = 1;
	output = new TextArea(5,30);
	output.setEditable(false);
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbag.setConstraints(output,c);
	add(output);
    }
    
    /**
     * Handles events in the applet.  Returns true if the indicated event
     * was handled, false for all other events.
     */
    public boolean action(Event e, Object arg) {
	Object target = e.target;
	
	if (target == b) { // match
	    try {
		String expr = tf.getText();
		RE reg = null;
		RESyntax res = values[syntax.getSelectedIndex()];
		reg = new RE(expr,insens.getState() ? RE.REG_ICASE | RE.REG_MULTILINE : RE.REG_MULTILINE, res);
		REMatchEnumeration en = reg.getMatchEnumeration(input.getText());
		StringBuffer sb = new StringBuffer();
		int matchNum = 0;
		while (en.hasMoreMatches()) {
		    sb.append(String.valueOf(++matchNum));
		    sb.append(". ");
		    sb.append(en.nextMatch().toString());
		    sb.append('\n');
		}
		output.setText(sb.toString());
	    } catch (REException err) { 
		output.setText("Expression compilation error: " + err.getMessage());
	    }
	return true;
	} else return false;
    }
}
