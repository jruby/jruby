
package org.jvyamlb;

import org.jruby.util.ByteList;

public class ResolverScanner {



static final byte[] _resolver_scanner_actions = {
	0, 1, 0, 1, 1, 1, 2, 1, 
	3, 1, 4, 1, 5, 1, 6, 1, 
	7
};

static final short[] _resolver_scanner_key_offsets = {
	0, 20, 20, 24, 28, 30, 32, 34, 
	35, 36, 37, 41, 45, 49, 51, 54, 
	61, 65, 69, 75, 77, 78, 79, 80, 
	82, 85, 87, 93, 97, 100, 101, 103, 
	105, 106, 108, 110, 115, 117, 119, 121, 
	125, 127, 128, 130, 136, 141, 145, 149, 
	150, 152, 153, 154, 155, 156, 157, 158, 
	161, 162, 163, 167, 168, 169, 171, 172, 
	173, 175, 176, 177, 178, 180, 182, 183, 
	184, 184, 188, 190, 190, 199, 206, 209, 
	212, 219, 224, 228, 230, 234, 237, 238, 
	247, 254, 261, 269, 275, 278, 279, 279, 
	286, 290, 295, 300, 305, 311, 311, 311
};

static final char[] _resolver_scanner_trans_keys = {
	32, 43, 45, 46, 48, 60, 61, 70, 
	78, 79, 84, 89, 102, 110, 111, 116, 
	121, 126, 49, 57, 46, 48, 49, 57, 
	73, 105, 48, 57, 43, 45, 48, 57, 
	78, 110, 70, 102, 110, 46, 58, 48, 
	57, 48, 53, 54, 57, 46, 58, 48, 
	57, 46, 58, 95, 48, 49, 95, 48, 
	57, 65, 70, 97, 102, 48, 53, 54, 
	57, 48, 53, 54, 57, 73, 78, 105, 
	110, 48, 57, 65, 97, 78, 97, 110, 
	48, 57, 45, 48, 57, 48, 57, 9, 
	32, 84, 116, 48, 57, 9, 32, 48, 
	57, 58, 48, 57, 58, 48, 57, 48, 
	57, 58, 48, 57, 48, 57, 9, 32, 
	43, 45, 90, 48, 57, 48, 57, 48, 
	57, 9, 32, 84, 116, 48, 57, 45, 
	48, 57, 9, 32, 84, 116, 48, 57, 
	45, 46, 58, 48, 57, 46, 58, 48, 
	57, 46, 58, 48, 57, 60, 65, 97, 
	76, 83, 69, 108, 115, 101, 79, 111, 
	117, 108, 108, 70, 78, 102, 110, 70, 
	102, 82, 114, 85, 117, 69, 101, 83, 
	115, 97, 111, 117, 102, 110, 114, 101, 
	69, 101, 48, 57, 48, 57, 46, 58, 
	95, 98, 120, 48, 55, 56, 57, 46, 
	58, 95, 48, 55, 56, 57, 95, 48, 
	55, 95, 48, 49, 95, 48, 57, 65, 
	70, 97, 102, 46, 58, 95, 48, 57, 
	46, 58, 48, 57, 46, 58, 58, 95, 
	48, 57, 58, 48, 57, 58, 46, 58, 
	95, 98, 120, 48, 55, 56, 57, 46, 
	58, 95, 48, 55, 56, 57, 46, 58, 
	95, 48, 55, 56, 57, 45, 46, 58, 
	95, 48, 55, 56, 57, 9, 32, 43, 
	45, 46, 90, 58, 48, 57, 58, 9, 
	32, 43, 45, 90, 48, 57, 9, 32, 
	84, 116, 46, 58, 95, 48, 57, 46, 
	58, 95, 48, 57, 46, 58, 95, 48, 
	57, 45, 46, 58, 95, 48, 57, 0
};

static final byte[] _resolver_scanner_single_lengths = {
	18, 0, 2, 2, 2, 0, 2, 1, 
	1, 1, 2, 0, 2, 2, 1, 1, 
	0, 0, 4, 2, 1, 1, 1, 0, 
	1, 0, 4, 2, 1, 1, 0, 0, 
	1, 0, 0, 5, 0, 0, 0, 4, 
	0, 1, 0, 4, 3, 2, 2, 1, 
	2, 1, 1, 1, 1, 1, 1, 3, 
	1, 1, 4, 1, 1, 2, 1, 1, 
	2, 1, 1, 1, 2, 2, 1, 1, 
	0, 2, 0, 0, 5, 3, 1, 1, 
	1, 3, 2, 2, 2, 1, 1, 5, 
	3, 3, 4, 6, 1, 1, 0, 5, 
	4, 3, 3, 3, 4, 0, 0, 0
};

static final byte[] _resolver_scanner_range_lengths = {
	1, 0, 1, 1, 0, 1, 0, 0, 
	0, 0, 1, 2, 1, 0, 1, 3, 
	2, 2, 1, 0, 0, 0, 0, 1, 
	1, 1, 1, 1, 1, 0, 1, 1, 
	0, 1, 1, 0, 1, 1, 1, 0, 
	1, 0, 1, 1, 1, 1, 1, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 1, 1, 0, 2, 2, 1, 1, 
	3, 1, 1, 0, 1, 1, 0, 2, 
	2, 2, 2, 0, 1, 0, 0, 1, 
	0, 1, 1, 1, 1, 0, 0, 0
};

static final short[] _resolver_scanner_index_offsets = {
	0, 20, 20, 24, 28, 31, 33, 36, 
	38, 40, 42, 46, 49, 53, 56, 59, 
	64, 67, 70, 76, 79, 81, 83, 85, 
	87, 90, 92, 98, 102, 105, 107, 109, 
	111, 113, 115, 117, 123, 125, 127, 129, 
	134, 136, 138, 140, 146, 151, 155, 159, 
	161, 164, 166, 168, 170, 172, 174, 176, 
	180, 182, 184, 189, 191, 193, 196, 198, 
	200, 203, 205, 207, 209, 212, 215, 217, 
	219, 220, 224, 226, 227, 235, 241, 244, 
	247, 252, 257, 261, 264, 268, 271, 273, 
	281, 287, 293, 300, 307, 310, 312, 313, 
	320, 325, 330, 335, 340, 346, 347, 348
};

static final byte[] _resolver_scanner_indicies = {
	32, 59, 59, 60, 61, 63, 64, 65, 
	66, 67, 68, 69, 70, 71, 72, 73, 
	74, 32, 62, 0, 52, 53, 20, 0, 
	46, 47, 10, 0, 58, 58, 0, 9, 
	0, 37, 38, 0, 31, 0, 31, 0, 
	38, 0, 10, 23, 22, 0, 90, 54, 
	0, 9, 23, 54, 0, 9, 23, 0, 
	13, 13, 0, 15, 15, 15, 15, 0, 
	87, 19, 0, 86, 16, 0, 46, 48, 
	47, 49, 10, 0, 43, 43, 0, 31, 
	0, 34, 0, 31, 0, 91, 0, 56, 
	57, 0, 101, 0, 1, 1, 2, 2, 
	93, 0, 1, 1, 75, 0, 51, 50, 
	0, 51, 0, 98, 0, 88, 0, 45, 
	0, 97, 0, 85, 0, 5, 5, 6, 
	6, 8, 0, 84, 0, 89, 0, 8, 
	0, 1, 1, 2, 2, 0, 75, 0, 
	55, 0, 92, 0, 1, 1, 2, 2, 
	76, 0, 26, 10, 23, 22, 0, 10, 
	23, 79, 0, 10, 23, 96, 0, 44, 
	0, 83, 82, 0, 39, 0, 35, 0, 
	28, 0, 81, 0, 27, 0, 28, 0, 
	28, 28, 30, 0, 80, 0, 32, 0, 
	40, 28, 33, 28, 0, 28, 0, 28, 
	0, 36, 29, 0, 35, 0, 27, 0, 
	41, 42, 0, 28, 0, 28, 0, 82, 
	0, 28, 30, 0, 33, 28, 0, 29, 
	0, 42, 0, 0, 11, 11, 10, 0, 
	9, 0, 0, 10, 23, 14, 24, 25, 
	21, 22, 0, 10, 23, 14, 21, 22, 
	0, 14, 14, 0, 13, 13, 0, 15, 
	15, 15, 15, 0, 10, 18, 17, 20, 
	0, 9, 18, 19, 0, 9, 18, 0, 
	12, 17, 17, 0, 12, 16, 0, 12, 
	0, 10, 23, 14, 24, 25, 99, 100, 
	0, 10, 23, 14, 95, 96, 0, 10, 
	23, 14, 78, 79, 0, 26, 10, 23, 
	14, 21, 22, 0, 5, 5, 6, 6, 
	7, 8, 0, 3, 4, 0, 3, 0, 
	0, 5, 5, 6, 6, 8, 7, 0, 
	1, 1, 2, 2, 0, 10, 18, 17, 
	102, 0, 10, 18, 17, 94, 0, 10, 
	18, 17, 77, 0, 26, 10, 18, 17, 
	20, 0, 0, 0, 0, 0
};

static final byte[] _resolver_scanner_trans_targs_wi = {
	1, 27, 40, 37, 93, 35, 36, 95, 
	94, 74, 73, 4, 17, 79, 78, 80, 
	86, 84, 16, 83, 81, 77, 10, 11, 
	14, 15, 23, 54, 103, 63, 56, 75, 
	72, 60, 22, 51, 62, 7, 8, 50, 
	59, 65, 66, 20, 101, 33, 6, 9, 
	19, 21, 29, 30, 3, 76, 13, 42, 
	25, 41, 5, 2, 18, 87, 97, 47, 
	102, 48, 55, 58, 61, 64, 67, 68, 
	69, 70, 71, 28, 96, 100, 90, 44, 
	57, 53, 52, 49, 92, 91, 85, 82, 
	32, 38, 12, 24, 43, 39, 99, 89, 
	45, 34, 31, 88, 46, 26, 98
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
	0, 0, 0, 0, 0, 0, 0
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
	0, 0, 0, 0, 0, 0, 0, 0, 
	5, 13, 13, 13, 15, 15, 15, 15, 
	15, 15, 15, 15, 15, 15, 15, 15, 
	15, 15, 15, 9, 9, 9, 9, 9, 
	7, 15, 15, 15, 15, 3, 11, 1
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
