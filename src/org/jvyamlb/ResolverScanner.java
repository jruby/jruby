
package org.jvyamlb;

import org.jruby.util.ByteList;

public class ResolverScanner {



static final byte[] _resolver_scanner_actions = {
	0, 1, 0, 1, 1, 1, 2, 1, 
	3, 1, 4, 1, 5, 1, 6, 1, 
	7
};

static final short[] _resolver_scanner_key_offsets = {
	0, 20, 20, 24, 29, 31, 33, 35, 
	36, 37, 38, 43, 47, 51, 53, 56, 
	63, 67, 74, 76, 77, 78, 79, 81, 
	84, 86, 92, 96, 99, 100, 102, 104, 
	105, 107, 109, 114, 116, 118, 120, 124, 
	126, 127, 129, 135, 141, 146, 151, 152, 
	154, 155, 156, 157, 158, 159, 160, 163, 
	164, 165, 169, 170, 171, 173, 174, 175, 
	177, 178, 179, 180, 182, 184, 185, 186, 
	186, 191, 193, 193, 202, 209, 212, 215, 
	222, 227, 231, 233, 242, 249, 256, 264, 
	270, 273, 274, 274, 281, 285, 290, 295, 
	300, 306, 306, 306
};

static final char[] _resolver_scanner_trans_keys = {
	32, 43, 45, 46, 48, 60, 61, 70, 
	78, 79, 84, 89, 102, 110, 111, 116, 
	121, 126, 49, 57, 46, 48, 49, 57, 
	73, 95, 105, 48, 57, 43, 45, 48, 
	57, 78, 110, 70, 102, 110, 46, 58, 
	95, 48, 57, 48, 53, 54, 57, 46, 
	58, 48, 57, 46, 58, 95, 48, 49, 
	95, 48, 57, 65, 70, 97, 102, 48, 
	53, 54, 57, 73, 78, 95, 105, 110, 
	48, 57, 65, 97, 78, 97, 110, 48, 
	57, 45, 48, 57, 48, 57, 9, 32, 
	84, 116, 48, 57, 9, 32, 48, 57, 
	58, 48, 57, 58, 48, 57, 48, 57, 
	58, 48, 57, 48, 57, 9, 32, 43, 
	45, 90, 48, 57, 48, 57, 48, 57, 
	9, 32, 84, 116, 48, 57, 45, 48, 
	57, 9, 32, 84, 116, 48, 57, 45, 
	46, 58, 95, 48, 57, 46, 58, 95, 
	48, 57, 46, 58, 95, 48, 57, 60, 
	65, 97, 76, 83, 69, 108, 115, 101, 
	79, 111, 117, 108, 108, 70, 78, 102, 
	110, 70, 102, 82, 114, 85, 117, 69, 
	101, 83, 115, 97, 111, 117, 102, 110, 
	114, 101, 69, 95, 101, 48, 57, 48, 
	57, 46, 58, 95, 98, 120, 48, 55, 
	56, 57, 46, 58, 95, 48, 55, 56, 
	57, 95, 48, 57, 95, 48, 49, 95, 
	48, 57, 65, 70, 97, 102, 46, 58, 
	95, 48, 57, 46, 58, 48, 57, 46, 
	58, 46, 58, 95, 98, 120, 48, 55, 
	56, 57, 46, 58, 95, 48, 55, 56, 
	57, 46, 58, 95, 48, 55, 56, 57, 
	45, 46, 58, 95, 48, 55, 56, 57, 
	9, 32, 43, 45, 46, 90, 58, 48, 
	57, 58, 9, 32, 43, 45, 90, 48, 
	57, 9, 32, 84, 116, 46, 58, 95, 
	48, 57, 46, 58, 95, 48, 57, 46, 
	58, 95, 48, 57, 45, 46, 58, 95, 
	48, 57, 0
};

static final byte[] _resolver_scanner_single_lengths = {
	18, 0, 2, 3, 2, 0, 2, 1, 
	1, 1, 3, 0, 2, 2, 1, 1, 
	0, 5, 2, 1, 1, 1, 0, 1, 
	0, 4, 2, 1, 1, 0, 0, 1, 
	0, 0, 5, 0, 0, 0, 4, 0, 
	1, 0, 4, 4, 3, 3, 1, 2, 
	1, 1, 1, 1, 1, 1, 3, 1, 
	1, 4, 1, 1, 2, 1, 1, 2, 
	1, 1, 1, 2, 2, 1, 1, 0, 
	3, 0, 0, 5, 3, 1, 1, 1, 
	3, 2, 2, 5, 3, 3, 4, 6, 
	1, 1, 0, 5, 4, 3, 3, 3, 
	4, 0, 0, 0
};

static final byte[] _resolver_scanner_range_lengths = {
	1, 0, 1, 1, 0, 1, 0, 0, 
	0, 0, 1, 2, 1, 0, 1, 3, 
	2, 1, 0, 0, 0, 0, 1, 1, 
	1, 1, 1, 1, 0, 1, 1, 0, 
	1, 1, 0, 1, 1, 1, 0, 1, 
	0, 1, 1, 1, 1, 1, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	1, 1, 0, 2, 2, 1, 1, 3, 
	1, 1, 0, 2, 2, 2, 2, 0, 
	1, 0, 0, 1, 0, 1, 1, 1, 
	1, 0, 0, 0
};

static final short[] _resolver_scanner_index_offsets = {
	0, 20, 20, 24, 29, 32, 34, 37, 
	39, 41, 43, 48, 51, 55, 58, 61, 
	66, 69, 76, 79, 81, 83, 85, 87, 
	90, 92, 98, 102, 105, 107, 109, 111, 
	113, 115, 117, 123, 125, 127, 129, 134, 
	136, 138, 140, 146, 152, 157, 162, 164, 
	167, 169, 171, 173, 175, 177, 179, 183, 
	185, 187, 192, 194, 196, 199, 201, 203, 
	206, 208, 210, 212, 215, 218, 220, 222, 
	223, 228, 230, 231, 239, 245, 248, 251, 
	256, 261, 265, 268, 276, 282, 288, 295, 
	302, 305, 307, 308, 315, 320, 325, 330, 
	335, 341, 342, 343
};

static final byte[] _resolver_scanner_indicies = {
	29, 56, 56, 57, 58, 60, 61, 62, 
	63, 64, 65, 66, 67, 68, 69, 70, 
	71, 29, 59, 0, 49, 50, 17, 0, 
	43, 11, 44, 11, 0, 55, 55, 0, 
	9, 0, 34, 35, 0, 28, 0, 28, 
	0, 35, 0, 11, 20, 19, 19, 0, 
	86, 51, 0, 10, 20, 51, 0, 10, 
	20, 0, 13, 13, 0, 14, 14, 14, 
	14, 0, 83, 16, 0, 43, 45, 11, 
	44, 46, 11, 0, 40, 40, 0, 28, 
	0, 31, 0, 28, 0, 87, 0, 53, 
	54, 0, 97, 0, 1, 1, 2, 2, 
	89, 0, 1, 1, 72, 0, 48, 47, 
	0, 48, 0, 94, 0, 84, 0, 42, 
	0, 93, 0, 82, 0, 5, 5, 6, 
	6, 8, 0, 81, 0, 85, 0, 8, 
	0, 1, 1, 2, 2, 0, 72, 0, 
	52, 0, 88, 0, 1, 1, 2, 2, 
	73, 0, 23, 11, 20, 19, 19, 0, 
	11, 20, 19, 76, 0, 11, 20, 19, 
	92, 0, 41, 0, 80, 79, 0, 36, 
	0, 32, 0, 25, 0, 78, 0, 24, 
	0, 25, 0, 25, 25, 27, 0, 77, 
	0, 29, 0, 37, 25, 30, 25, 0, 
	25, 0, 25, 0, 33, 26, 0, 32, 
	0, 24, 0, 38, 39, 0, 25, 0, 
	25, 0, 79, 0, 25, 27, 0, 30, 
	25, 0, 26, 0, 39, 0, 0, 12, 
	11, 12, 11, 0, 9, 0, 0, 11, 
	20, 18, 21, 22, 18, 19, 0, 11, 
	20, 18, 18, 19, 0, 10, 10, 0, 
	13, 13, 0, 14, 14, 14, 14, 0, 
	11, 15, 17, 17, 0, 10, 15, 16, 
	0, 10, 15, 0, 11, 20, 18, 21, 
	22, 95, 96, 0, 11, 20, 18, 91, 
	92, 0, 11, 20, 18, 75, 76, 0, 
	23, 11, 20, 18, 18, 19, 0, 5, 
	5, 6, 6, 7, 8, 0, 3, 4, 
	0, 3, 0, 0, 5, 5, 6, 6, 
	8, 7, 0, 1, 1, 2, 2, 0, 
	11, 15, 17, 98, 0, 11, 15, 17, 
	90, 0, 11, 15, 17, 74, 0, 23, 
	11, 15, 17, 17, 0, 0, 0, 0, 
	0
};

static final byte[] _resolver_scanner_trans_targs_wi = {
	1, 26, 39, 36, 89, 34, 35, 91, 
	90, 73, 77, 72, 4, 78, 79, 16, 
	82, 80, 76, 10, 11, 14, 15, 22, 
	53, 99, 62, 55, 74, 71, 59, 21, 
	50, 61, 7, 8, 49, 58, 64, 65, 
	19, 97, 32, 6, 9, 18, 20, 28, 
	29, 3, 75, 13, 41, 24, 40, 5, 
	2, 17, 83, 93, 46, 98, 47, 54, 
	57, 60, 63, 66, 67, 68, 69, 70, 
	27, 92, 96, 86, 43, 56, 52, 51, 
	48, 88, 87, 81, 31, 37, 12, 23, 
	42, 38, 95, 85, 44, 33, 30, 84, 
	45, 25, 94
};

static final byte[] _resolver_scanner_trans_actions_wi = {
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0
};

static final byte[] _resolver_scanner_eof_actions = {
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 5, 
	13, 13, 13, 15, 15, 13, 15, 15, 
	15, 15, 15, 15, 15, 15, 15, 9, 
	9, 9, 9, 9, 7, 15, 15, 15, 
	15, 3, 11, 1
};

static final int resolver_scanner_start = 0;

static final int resolver_scanner_error = 1;


   public String recognize(ByteList list) {
       String tag = null;
       int cs;
       int act;
       int have = 0;
       int nread = 0;
       int p=0;
       int pe = list.realSize;
       int tokstart = -1;
       int tokend = -1;

       byte[] data = list.bytes;
       if(pe == 0) {
         data = new byte[]{(byte)'~'};
         pe = 1;
       }
              

	{
	cs = resolver_scanner_start;
	}


	{
	int _klen;
	int _trans;
	int _keys;

	if ( p != pe ) {
	_resume: while ( true ) {
	_again: do {
	if ( cs == 1 )
		break _resume;
	_match: do {
	_keys = _resolver_scanner_key_offsets[cs];
	_trans = _resolver_scanner_index_offsets[cs];
	_klen = _resolver_scanner_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( data[p] < _resolver_scanner_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( data[p] > _resolver_scanner_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _resolver_scanner_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( data[p] < _resolver_scanner_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( data[p] > _resolver_scanner_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	_trans = _resolver_scanner_indicies[_trans];
	cs = _resolver_scanner_trans_targs_wi[_trans];

	} while (false);
	if ( ++p == pe )
		break _resume;
	}
	}
	}


	int _acts = _resolver_scanner_eof_actions[cs];
	int _nacts = (int) _resolver_scanner_actions[_acts++];
	while ( _nacts-- > 0 ) {
		switch ( _resolver_scanner_actions[_acts++] ) {
	case 0:
	{ tag = "tag:yaml.org,2002:bool"; }
	break;
	case 1:
	{ tag = "tag:yaml.org,2002:merge"; }
	break;
	case 2:
	{ tag = "tag:yaml.org,2002:null"; }
	break;
	case 3:
	{ tag = "tag:yaml.org,2002:timestamp#ymd"; }
	break;
	case 4:
	{ tag = "tag:yaml.org,2002:timestamp"; }
	break;
	case 5:
	{ tag = "tag:yaml.org,2002:value"; }
	break;
	case 6:
	{ tag = "tag:yaml.org,2002:float"; }
	break;
	case 7:
	{ tag = "tag:yaml.org,2002:int"; }
	break;
		}
	}

       return tag;
   }

   public static void main(String[] args) {
       ByteList b = new ByteList(78);
       b.append(args[0].getBytes());
/*
       for(int i=0;i<b.realSize;i++) {
           System.err.println("byte " + i + " is " + b.bytes[i] + " char is: " + args[0].charAt(i));
       }
*/
       System.err.println(new ResolverScanner().recognize(b));
   }
}
