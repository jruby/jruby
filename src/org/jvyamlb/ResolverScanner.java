// line 1 "src/org/jvyamlb/resolver_scanner.rl"

package org.jvyamlb;

import org.jruby.util.ByteList;

public class ResolverScanner {
// line 52 "src/org/jvyamlb/resolver_scanner.rl"



// line 13 "src/org/jvyamlb/ResolverScanner.java"
private static void init__resolver_scanner_actions_0( byte[] r )
{
	r[0]=0; r[1]=1; r[2]=0; r[3]=1; r[4]=1; r[5]=1; r[6]=2; r[7]=1; 
	r[8]=3; r[9]=1; r[10]=4; r[11]=1; r[12]=5; r[13]=1; r[14]=6; r[15]=1; 
	r[16]=7; 
}

private static byte[] create__resolver_scanner_actions( )
{
	byte[] r = new byte[17];
	init__resolver_scanner_actions_0( r );
	return r;
}

private static final byte _resolver_scanner_actions[] = create__resolver_scanner_actions();


private static void init__resolver_scanner_key_offsets_0( short[] r )
{
	r[0]=0; r[1]=0; r[2]=21; r[3]=26; r[4]=30; r[5]=32; r[6]=34; r[7]=38; 
	r[8]=40; r[9]=41; r[10]=42; r[11]=43; r[12]=48; r[13]=52; r[14]=56; r[15]=58; 
	r[16]=61; r[17]=69; r[18]=73; r[19]=77; r[20]=83; r[21]=85; r[22]=86; r[23]=87; 
	r[24]=88; r[25]=90; r[26]=93; r[27]=95; r[28]=101; r[29]=105; r[30]=108; r[31]=109; 
	r[32]=111; r[33]=113; r[34]=114; r[35]=116; r[36]=118; r[37]=123; r[38]=125; r[39]=127; 
	r[40]=129; r[41]=133; r[42]=135; r[43]=136; r[44]=138; r[45]=144; r[46]=150; r[47]=155; 
	r[48]=160; r[49]=161; r[50]=163; r[51]=164; r[52]=165; r[53]=166; r[54]=167; r[55]=168; 
	r[56]=169; r[57]=173; r[58]=174; r[59]=175; r[60]=176; r[61]=177; r[62]=181; r[63]=182; 
	r[64]=183; r[65]=185; r[66]=186; r[67]=187; r[68]=189; r[69]=190; r[70]=191; r[71]=192; 
	r[72]=194; r[73]=196; r[74]=197; r[75]=198; r[76]=198; r[77]=202; r[78]=204; r[79]=204; 
	r[80]=214; r[81]=222; r[82]=225; r[83]=228; r[84]=236; r[85]=242; r[86]=248; r[87]=251; 
	r[88]=252; r[89]=257; r[90]=261; r[91]=263; r[92]=273; r[93]=281; r[94]=289; r[95]=298; 
	r[96]=304; r[97]=307; r[98]=308; r[99]=308; r[100]=315; r[101]=319; r[102]=325; r[103]=331; 
	r[104]=337; r[105]=344; r[106]=344; r[107]=344; 
}

private static short[] create__resolver_scanner_key_offsets( )
{
	short[] r = new short[108];
	init__resolver_scanner_key_offsets_0( r );
	return r;
}

private static final short _resolver_scanner_key_offsets[] = create__resolver_scanner_key_offsets();


private static void init__resolver_scanner_trans_keys_0( char[] r )
{
	r[0]=32; r[1]=44; r[2]=46; r[3]=48; r[4]=60; r[5]=61; r[6]=70; r[7]=78; 
	r[8]=79; r[9]=84; r[10]=89; r[11]=102; r[12]=110; r[13]=111; r[14]=116; r[15]=121; 
	r[16]=126; r[17]=43; r[18]=45; r[19]=49; r[20]=57; r[21]=44; r[22]=46; r[23]=48; 
	r[24]=49; r[25]=57; r[26]=44; r[27]=46; r[28]=48; r[29]=57; r[30]=43; r[31]=45; 
	r[32]=48; r[33]=57; r[34]=73; r[35]=105; r[36]=48; r[37]=57; r[38]=78; r[39]=110; 
	r[40]=70; r[41]=102; r[42]=110; r[43]=44; r[44]=46; r[45]=58; r[46]=48; r[47]=57; 
	r[48]=48; r[49]=53; r[50]=54; r[51]=57; r[52]=46; r[53]=58; r[54]=48; r[55]=57; 
	r[56]=46; r[57]=58; r[58]=95; r[59]=48; r[60]=49; r[61]=44; r[62]=95; r[63]=48; 
	r[64]=57; r[65]=65; r[66]=70; r[67]=97; r[68]=102; r[69]=48; r[70]=53; r[71]=54; 
	r[72]=57; r[73]=48; r[74]=53; r[75]=54; r[76]=57; r[77]=73; r[78]=78; r[79]=105; 
	r[80]=110; r[81]=48; r[82]=57; r[83]=65; r[84]=97; r[85]=78; r[86]=97; r[87]=110; 
	r[88]=48; r[89]=57; r[90]=45; r[91]=48; r[92]=57; r[93]=48; r[94]=57; r[95]=9; 
	r[96]=32; r[97]=84; r[98]=116; r[99]=48; r[100]=57; r[101]=9; r[102]=32; r[103]=48; 
	r[104]=57; r[105]=58; r[106]=48; r[107]=57; r[108]=58; r[109]=48; r[110]=57; r[111]=48; 
	r[112]=57; r[113]=58; r[114]=48; r[115]=57; r[116]=48; r[117]=57; r[118]=9; r[119]=32; 
	r[120]=43; r[121]=45; r[122]=90; r[123]=48; r[124]=57; r[125]=48; r[126]=57; r[127]=48; 
	r[128]=57; r[129]=9; r[130]=32; r[131]=84; r[132]=116; r[133]=48; r[134]=57; r[135]=45; 
	r[136]=48; r[137]=57; r[138]=9; r[139]=32; r[140]=84; r[141]=116; r[142]=48; r[143]=57; 
	r[144]=44; r[145]=45; r[146]=46; r[147]=58; r[148]=48; r[149]=57; r[150]=44; r[151]=46; 
	r[152]=58; r[153]=48; r[154]=57; r[155]=44; r[156]=46; r[157]=58; r[158]=48; r[159]=57; 
	r[160]=60; r[161]=65; r[162]=97; r[163]=76; r[164]=83; r[165]=69; r[166]=108; r[167]=115; 
	r[168]=101; r[169]=79; r[170]=85; r[171]=111; r[172]=117; r[173]=76; r[174]=76; r[175]=108; 
	r[176]=108; r[177]=70; r[178]=78; r[179]=102; r[180]=110; r[181]=70; r[182]=102; r[183]=82; 
	r[184]=114; r[185]=85; r[186]=117; r[187]=69; r[188]=101; r[189]=83; r[190]=115; r[191]=97; 
	r[192]=111; r[193]=117; r[194]=102; r[195]=110; r[196]=114; r[197]=101; r[198]=69; r[199]=101; 
	r[200]=48; r[201]=57; r[202]=48; r[203]=57; r[204]=44; r[205]=46; r[206]=58; r[207]=95; 
	r[208]=98; r[209]=120; r[210]=48; r[211]=55; r[212]=56; r[213]=57; r[214]=44; r[215]=46; 
	r[216]=58; r[217]=95; r[218]=48; r[219]=55; r[220]=56; r[221]=57; r[222]=95; r[223]=48; 
	r[224]=55; r[225]=95; r[226]=48; r[227]=49; r[228]=44; r[229]=95; r[230]=48; r[231]=57; 
	r[232]=65; r[233]=70; r[234]=97; r[235]=102; r[236]=44; r[237]=46; r[238]=58; r[239]=95; 
	r[240]=48; r[241]=57; r[242]=44; r[243]=46; r[244]=58; r[245]=95; r[246]=48; r[247]=57; 
	r[248]=58; r[249]=48; r[250]=57; r[251]=58; r[252]=44; r[253]=58; r[254]=95; r[255]=48; 
	r[256]=57; r[257]=46; r[258]=58; r[259]=48; r[260]=57; r[261]=46; r[262]=58; r[263]=44; 
	r[264]=46; r[265]=58; r[266]=95; r[267]=98; r[268]=120; r[269]=48; r[270]=55; r[271]=56; 
	r[272]=57; r[273]=44; r[274]=46; r[275]=58; r[276]=95; r[277]=48; r[278]=55; r[279]=56; 
	r[280]=57; r[281]=44; r[282]=46; r[283]=58; r[284]=95; r[285]=48; r[286]=55; r[287]=56; 
	r[288]=57; r[289]=44; r[290]=45; r[291]=46; r[292]=58; r[293]=95; r[294]=48; r[295]=55; 
	r[296]=56; r[297]=57; r[298]=9; r[299]=32; r[300]=43; r[301]=45; r[302]=46; r[303]=90; 
	r[304]=58; r[305]=48; r[306]=57; r[307]=58; r[308]=9; r[309]=32; r[310]=43; r[311]=45; 
	r[312]=90; r[313]=48; r[314]=57; r[315]=9; r[316]=32; r[317]=84; r[318]=116; r[319]=44; 
	r[320]=46; r[321]=58; r[322]=95; r[323]=48; r[324]=57; r[325]=44; r[326]=46; r[327]=58; 
	r[328]=95; r[329]=48; r[330]=57; r[331]=44; r[332]=46; r[333]=58; r[334]=95; r[335]=48; 
	r[336]=57; r[337]=44; r[338]=45; r[339]=46; r[340]=58; r[341]=95; r[342]=48; r[343]=57; 
	r[344]=0; 
}

private static char[] create__resolver_scanner_trans_keys( )
{
	char[] r = new char[345];
	init__resolver_scanner_trans_keys_0( r );
	return r;
}

private static final char _resolver_scanner_trans_keys[] = create__resolver_scanner_trans_keys();


private static void init__resolver_scanner_single_lengths_0( byte[] r )
{
	r[0]=0; r[1]=17; r[2]=3; r[3]=2; r[4]=2; r[5]=0; r[6]=2; r[7]=2; 
	r[8]=1; r[9]=1; r[10]=1; r[11]=3; r[12]=0; r[13]=2; r[14]=2; r[15]=1; 
	r[16]=2; r[17]=0; r[18]=0; r[19]=4; r[20]=2; r[21]=1; r[22]=1; r[23]=1; 
	r[24]=0; r[25]=1; r[26]=0; r[27]=4; r[28]=2; r[29]=1; r[30]=1; r[31]=0; 
	r[32]=0; r[33]=1; r[34]=0; r[35]=0; r[36]=5; r[37]=0; r[38]=0; r[39]=0; 
	r[40]=4; r[41]=0; r[42]=1; r[43]=0; r[44]=4; r[45]=4; r[46]=3; r[47]=3; 
	r[48]=1; r[49]=2; r[50]=1; r[51]=1; r[52]=1; r[53]=1; r[54]=1; r[55]=1; 
	r[56]=4; r[57]=1; r[58]=1; r[59]=1; r[60]=1; r[61]=4; r[62]=1; r[63]=1; 
	r[64]=2; r[65]=1; r[66]=1; r[67]=2; r[68]=1; r[69]=1; r[70]=1; r[71]=2; 
	r[72]=2; r[73]=1; r[74]=1; r[75]=0; r[76]=2; r[77]=0; r[78]=0; r[79]=6; 
	r[80]=4; r[81]=1; r[82]=1; r[83]=2; r[84]=4; r[85]=4; r[86]=1; r[87]=1; 
	r[88]=3; r[89]=2; r[90]=2; r[91]=6; r[92]=4; r[93]=4; r[94]=5; r[95]=6; 
	r[96]=1; r[97]=1; r[98]=0; r[99]=5; r[100]=4; r[101]=4; r[102]=4; r[103]=4; 
	r[104]=5; r[105]=0; r[106]=0; r[107]=0; 
}

private static byte[] create__resolver_scanner_single_lengths( )
{
	byte[] r = new byte[108];
	init__resolver_scanner_single_lengths_0( r );
	return r;
}

private static final byte _resolver_scanner_single_lengths[] = create__resolver_scanner_single_lengths();


private static void init__resolver_scanner_range_lengths_0( byte[] r )
{
	r[0]=0; r[1]=2; r[2]=1; r[3]=1; r[4]=0; r[5]=1; r[6]=1; r[7]=0; 
	r[8]=0; r[9]=0; r[10]=0; r[11]=1; r[12]=2; r[13]=1; r[14]=0; r[15]=1; 
	r[16]=3; r[17]=2; r[18]=2; r[19]=1; r[20]=0; r[21]=0; r[22]=0; r[23]=0; 
	r[24]=1; r[25]=1; r[26]=1; r[27]=1; r[28]=1; r[29]=1; r[30]=0; r[31]=1; 
	r[32]=1; r[33]=0; r[34]=1; r[35]=1; r[36]=0; r[37]=1; r[38]=1; r[39]=1; 
	r[40]=0; r[41]=1; r[42]=0; r[43]=1; r[44]=1; r[45]=1; r[46]=1; r[47]=1; 
	r[48]=0; r[49]=0; r[50]=0; r[51]=0; r[52]=0; r[53]=0; r[54]=0; r[55]=0; 
	r[56]=0; r[57]=0; r[58]=0; r[59]=0; r[60]=0; r[61]=0; r[62]=0; r[63]=0; 
	r[64]=0; r[65]=0; r[66]=0; r[67]=0; r[68]=0; r[69]=0; r[70]=0; r[71]=0; 
	r[72]=0; r[73]=0; r[74]=0; r[75]=0; r[76]=1; r[77]=1; r[78]=0; r[79]=2; 
	r[80]=2; r[81]=1; r[82]=1; r[83]=3; r[84]=1; r[85]=1; r[86]=1; r[87]=0; 
	r[88]=1; r[89]=1; r[90]=0; r[91]=2; r[92]=2; r[93]=2; r[94]=2; r[95]=0; 
	r[96]=1; r[97]=0; r[98]=0; r[99]=1; r[100]=0; r[101]=1; r[102]=1; r[103]=1; 
	r[104]=1; r[105]=0; r[106]=0; r[107]=0; 
}

private static byte[] create__resolver_scanner_range_lengths( )
{
	byte[] r = new byte[108];
	init__resolver_scanner_range_lengths_0( r );
	return r;
}

private static final byte _resolver_scanner_range_lengths[] = create__resolver_scanner_range_lengths();


private static void init__resolver_scanner_index_offsets_0( short[] r )
{
	r[0]=0; r[1]=0; r[2]=20; r[3]=25; r[4]=29; r[5]=32; r[6]=34; r[7]=38; 
	r[8]=41; r[9]=43; r[10]=45; r[11]=47; r[12]=52; r[13]=55; r[14]=59; r[15]=62; 
	r[16]=65; r[17]=71; r[18]=74; r[19]=77; r[20]=83; r[21]=86; r[22]=88; r[23]=90; 
	r[24]=92; r[25]=94; r[26]=97; r[27]=99; r[28]=105; r[29]=109; r[30]=112; r[31]=114; 
	r[32]=116; r[33]=118; r[34]=120; r[35]=122; r[36]=124; r[37]=130; r[38]=132; r[39]=134; 
	r[40]=136; r[41]=141; r[42]=143; r[43]=145; r[44]=147; r[45]=153; r[46]=159; r[47]=164; 
	r[48]=169; r[49]=171; r[50]=174; r[51]=176; r[52]=178; r[53]=180; r[54]=182; r[55]=184; 
	r[56]=186; r[57]=191; r[58]=193; r[59]=195; r[60]=197; r[61]=199; r[62]=204; r[63]=206; 
	r[64]=208; r[65]=211; r[66]=213; r[67]=215; r[68]=218; r[69]=220; r[70]=222; r[71]=224; 
	r[72]=227; r[73]=230; r[74]=232; r[75]=234; r[76]=235; r[77]=239; r[78]=241; r[79]=242; 
	r[80]=251; r[81]=258; r[82]=261; r[83]=264; r[84]=270; r[85]=276; r[86]=282; r[87]=285; 
	r[88]=287; r[89]=292; r[90]=296; r[91]=299; r[92]=308; r[93]=315; r[94]=322; r[95]=330; 
	r[96]=337; r[97]=340; r[98]=342; r[99]=343; r[100]=350; r[101]=355; r[102]=361; r[103]=367; 
	r[104]=373; r[105]=380; r[106]=381; r[107]=382; 
}

private static short[] create__resolver_scanner_index_offsets( )
{
	short[] r = new short[108];
	init__resolver_scanner_index_offsets_0( r );
	return r;
}

private static final short _resolver_scanner_index_offsets[] = create__resolver_scanner_index_offsets();


private static void init__resolver_scanner_indicies_0( byte[] r )
{
	r[0]=0; r[1]=3; r[2]=4; r[3]=5; r[4]=7; r[5]=8; r[6]=9; r[7]=10; 
	r[8]=11; r[9]=12; r[10]=13; r[11]=14; r[12]=15; r[13]=16; r[14]=17; r[15]=18; 
	r[16]=0; r[17]=2; r[18]=6; r[19]=1; r[20]=3; r[21]=19; r[22]=20; r[23]=21; 
	r[24]=1; r[25]=3; r[26]=22; r[27]=3; r[28]=1; r[29]=23; r[30]=23; r[31]=1; 
	r[32]=24; r[33]=1; r[34]=25; r[35]=26; r[36]=22; r[37]=1; r[38]=27; r[39]=28; 
	r[40]=1; r[41]=29; r[42]=1; r[43]=29; r[44]=1; r[45]=28; r[46]=1; r[47]=3; 
	r[48]=22; r[49]=31; r[50]=30; r[51]=1; r[52]=32; r[53]=33; r[54]=1; r[55]=24; 
	r[56]=31; r[57]=33; r[58]=1; r[59]=24; r[60]=31; r[61]=1; r[62]=34; r[63]=34; 
	r[64]=1; r[65]=35; r[66]=35; r[67]=35; r[68]=35; r[69]=35; r[70]=1; r[71]=36; 
	r[72]=37; r[73]=1; r[74]=38; r[75]=39; r[76]=1; r[77]=25; r[78]=40; r[79]=26; 
	r[80]=41; r[81]=22; r[82]=1; r[83]=42; r[84]=42; r[85]=1; r[86]=29; r[87]=1; 
	r[88]=43; r[89]=1; r[90]=29; r[91]=1; r[92]=44; r[93]=1; r[94]=45; r[95]=46; 
	r[96]=1; r[97]=47; r[98]=1; r[99]=48; r[100]=48; r[101]=50; r[102]=50; r[103]=49; 
	r[104]=1; r[105]=48; r[106]=48; r[107]=51; r[108]=1; r[109]=53; r[110]=52; r[111]=1; 
	r[112]=53; r[113]=1; r[114]=54; r[115]=1; r[116]=55; r[117]=1; r[118]=56; r[119]=1; 
	r[120]=57; r[121]=1; r[122]=58; r[123]=1; r[124]=59; r[125]=59; r[126]=60; r[127]=60; 
	r[128]=61; r[129]=1; r[130]=62; r[131]=1; r[132]=63; r[133]=1; r[134]=61; r[135]=1; 
	r[136]=48; r[137]=48; r[138]=50; r[139]=50; r[140]=1; r[141]=51; r[142]=1; r[143]=64; 
	r[144]=1; r[145]=65; r[146]=1; r[147]=48; r[148]=48; r[149]=50; r[150]=50; r[151]=66; 
	r[152]=1; r[153]=3; r[154]=67; r[155]=22; r[156]=31; r[157]=30; r[158]=1; r[159]=3; 
	r[160]=22; r[161]=31; r[162]=68; r[163]=1; r[164]=3; r[165]=22; r[166]=31; r[167]=69; 
	r[168]=1; r[169]=70; r[170]=1; r[171]=71; r[172]=72; r[173]=1; r[174]=73; r[175]=1; 
	r[176]=74; r[177]=1; r[178]=75; r[179]=1; r[180]=76; r[181]=1; r[182]=77; r[183]=1; 
	r[184]=75; r[185]=1; r[186]=75; r[187]=78; r[188]=75; r[189]=79; r[190]=1; r[191]=80; 
	r[192]=1; r[193]=0; r[194]=1; r[195]=81; r[196]=1; r[197]=0; r[198]=1; r[199]=82; 
	r[200]=75; r[201]=83; r[202]=75; r[203]=1; r[204]=75; r[205]=1; r[206]=75; r[207]=1; 
	r[208]=84; r[209]=85; r[210]=1; r[211]=74; r[212]=1; r[213]=77; r[214]=1; r[215]=86; 
	r[216]=87; r[217]=1; r[218]=75; r[219]=1; r[220]=75; r[221]=1; r[222]=72; r[223]=1; 
	r[224]=75; r[225]=79; r[226]=1; r[227]=83; r[228]=75; r[229]=1; r[230]=85; r[231]=1; 
	r[232]=87; r[233]=1; r[234]=1; r[235]=88; r[236]=88; r[237]=22; r[238]=1; r[239]=24; 
	r[240]=1; r[241]=1; r[242]=3; r[243]=22; r[244]=31; r[245]=90; r[246]=91; r[247]=92; 
	r[248]=89; r[249]=30; r[250]=1; r[251]=3; r[252]=22; r[253]=31; r[254]=90; r[255]=89; 
	r[256]=30; r[257]=1; r[258]=90; r[259]=90; r[260]=1; r[261]=34; r[262]=34; r[263]=1; 
	r[264]=35; r[265]=35; r[266]=35; r[267]=35; r[268]=35; r[269]=1; r[270]=93; r[271]=22; 
	r[272]=94; r[273]=95; r[274]=21; r[275]=1; r[276]=93; r[277]=22; r[278]=96; r[279]=95; 
	r[280]=93; r[281]=1; r[282]=96; r[283]=37; r[284]=1; r[285]=96; r[286]=1; r[287]=95; 
	r[288]=96; r[289]=95; r[290]=95; r[291]=1; r[292]=24; r[293]=94; r[294]=39; r[295]=1; 
	r[296]=24; r[297]=94; r[298]=1; r[299]=3; r[300]=22; r[301]=31; r[302]=90; r[303]=91; 
	r[304]=92; r[305]=97; r[306]=98; r[307]=1; r[308]=3; r[309]=22; r[310]=31; r[311]=90; 
	r[312]=99; r[313]=69; r[314]=1; r[315]=3; r[316]=22; r[317]=31; r[318]=90; r[319]=100; 
	r[320]=68; r[321]=1; r[322]=3; r[323]=67; r[324]=22; r[325]=31; r[326]=90; r[327]=89; 
	r[328]=30; r[329]=1; r[330]=59; r[331]=59; r[332]=60; r[333]=60; r[334]=101; r[335]=61; 
	r[336]=1; r[337]=103; r[338]=102; r[339]=1; r[340]=103; r[341]=1; r[342]=1; r[343]=59; 
	r[344]=59; r[345]=60; r[346]=60; r[347]=61; r[348]=101; r[349]=1; r[350]=48; r[351]=48; 
	r[352]=50; r[353]=50; r[354]=1; r[355]=93; r[356]=22; r[357]=94; r[358]=95; r[359]=104; 
	r[360]=1; r[361]=93; r[362]=22; r[363]=94; r[364]=95; r[365]=105; r[366]=1; r[367]=93; 
	r[368]=22; r[369]=94; r[370]=95; r[371]=106; r[372]=1; r[373]=93; r[374]=67; r[375]=22; 
	r[376]=94; r[377]=95; r[378]=21; r[379]=1; r[380]=1; r[381]=1; r[382]=1; r[383]=0; 
}

private static byte[] create__resolver_scanner_indicies( )
{
	byte[] r = new byte[384];
	init__resolver_scanner_indicies_0( r );
	return r;
}

private static final byte _resolver_scanner_indicies[] = create__resolver_scanner_indicies();


private static void init__resolver_scanner_trans_targs_wi_0( byte[] r )
{
	r[0]=75; r[1]=0; r[2]=2; r[3]=3; r[4]=19; r[5]=91; r[6]=101; r[7]=48; 
	r[8]=106; r[9]=49; r[10]=56; r[11]=61; r[12]=64; r[13]=67; r[14]=70; r[15]=71; 
	r[16]=72; r[17]=73; r[18]=74; r[19]=6; r[20]=79; r[21]=84; r[22]=76; r[23]=5; 
	r[24]=77; r[25]=7; r[26]=10; r[27]=8; r[28]=9; r[29]=78; r[30]=11; r[31]=12; 
	r[32]=13; r[33]=14; r[34]=82; r[35]=83; r[36]=86; r[37]=87; r[38]=89; r[39]=90; 
	r[40]=20; r[41]=22; r[42]=21; r[43]=23; r[44]=25; r[45]=26; r[46]=42; r[47]=27; 
	r[48]=28; r[49]=40; r[50]=41; r[51]=29; r[52]=30; r[53]=31; r[54]=32; r[55]=33; 
	r[56]=34; r[57]=35; r[58]=95; r[59]=36; r[60]=37; r[61]=98; r[62]=96; r[63]=39; 
	r[64]=43; r[65]=44; r[66]=100; r[67]=24; r[68]=45; r[69]=46; r[70]=105; r[71]=50; 
	r[72]=53; r[73]=51; r[74]=52; r[75]=107; r[76]=54; r[77]=55; r[78]=57; r[79]=59; 
	r[80]=58; r[81]=60; r[82]=62; r[83]=63; r[84]=65; r[85]=66; r[86]=68; r[87]=69; 
	r[88]=4; r[89]=80; r[90]=81; r[91]=15; r[92]=16; r[93]=85; r[94]=18; r[95]=88; 
	r[96]=17; r[97]=92; r[98]=47; r[99]=93; r[100]=94; r[101]=99; r[102]=97; r[103]=38; 
	r[104]=102; r[105]=103; r[106]=104; 
}

private static byte[] create__resolver_scanner_trans_targs_wi( )
{
	byte[] r = new byte[107];
	init__resolver_scanner_trans_targs_wi_0( r );
	return r;
}

private static final byte _resolver_scanner_trans_targs_wi[] = create__resolver_scanner_trans_targs_wi();


private static void init__resolver_scanner_trans_actions_wi_0( byte[] r )
{
	r[0]=0; r[1]=0; r[2]=0; r[3]=0; r[4]=0; r[5]=0; r[6]=0; r[7]=0; 
	r[8]=0; r[9]=0; r[10]=0; r[11]=0; r[12]=0; r[13]=0; r[14]=0; r[15]=0; 
	r[16]=0; r[17]=0; r[18]=0; r[19]=0; r[20]=0; r[21]=0; r[22]=0; r[23]=0; 
	r[24]=0; r[25]=0; r[26]=0; r[27]=0; r[28]=0; r[29]=0; r[30]=0; r[31]=0; 
	r[32]=0; r[33]=0; r[34]=0; r[35]=0; r[36]=0; r[37]=0; r[38]=0; r[39]=0; 
	r[40]=0; r[41]=0; r[42]=0; r[43]=0; r[44]=0; r[45]=0; r[46]=0; r[47]=0; 
	r[48]=0; r[49]=0; r[50]=0; r[51]=0; r[52]=0; r[53]=0; r[54]=0; r[55]=0; 
	r[56]=0; r[57]=0; r[58]=0; r[59]=0; r[60]=0; r[61]=0; r[62]=0; r[63]=0; 
	r[64]=0; r[65]=0; r[66]=0; r[67]=0; r[68]=0; r[69]=0; r[70]=0; r[71]=0; 
	r[72]=0; r[73]=0; r[74]=0; r[75]=0; r[76]=0; r[77]=0; r[78]=0; r[79]=0; 
	r[80]=0; r[81]=0; r[82]=0; r[83]=0; r[84]=0; r[85]=0; r[86]=0; r[87]=0; 
	r[88]=0; r[89]=0; r[90]=0; r[91]=0; r[92]=0; r[93]=0; r[94]=0; r[95]=0; 
	r[96]=0; r[97]=0; r[98]=0; r[99]=0; r[100]=0; r[101]=0; r[102]=0; r[103]=0; 
	r[104]=0; r[105]=0; r[106]=0; 
}

private static byte[] create__resolver_scanner_trans_actions_wi( )
{
	byte[] r = new byte[107];
	init__resolver_scanner_trans_actions_wi_0( r );
	return r;
}

private static final byte _resolver_scanner_trans_actions_wi[] = create__resolver_scanner_trans_actions_wi();


private static void init__resolver_scanner_eof_actions_0( byte[] r )
{
	r[0]=0; r[1]=0; r[2]=0; r[3]=0; r[4]=0; r[5]=0; r[6]=0; r[7]=0; 
	r[8]=0; r[9]=0; r[10]=0; r[11]=0; r[12]=0; r[13]=0; r[14]=0; r[15]=0; 
	r[16]=0; r[17]=0; r[18]=0; r[19]=0; r[20]=0; r[21]=0; r[22]=0; r[23]=0; 
	r[24]=0; r[25]=0; r[26]=0; r[27]=0; r[28]=0; r[29]=0; r[30]=0; r[31]=0; 
	r[32]=0; r[33]=0; r[34]=0; r[35]=0; r[36]=0; r[37]=0; r[38]=0; r[39]=0; 
	r[40]=0; r[41]=0; r[42]=0; r[43]=0; r[44]=0; r[45]=0; r[46]=0; r[47]=0; 
	r[48]=0; r[49]=0; r[50]=0; r[51]=0; r[52]=0; r[53]=0; r[54]=0; r[55]=0; 
	r[56]=0; r[57]=0; r[58]=0; r[59]=0; r[60]=0; r[61]=0; r[62]=0; r[63]=0; 
	r[64]=0; r[65]=0; r[66]=0; r[67]=0; r[68]=0; r[69]=0; r[70]=0; r[71]=0; 
	r[72]=0; r[73]=0; r[74]=0; r[75]=5; r[76]=13; r[77]=13; r[78]=13; r[79]=15; 
	r[80]=15; r[81]=15; r[82]=15; r[83]=15; r[84]=15; r[85]=15; r[86]=15; r[87]=15; 
	r[88]=15; r[89]=15; r[90]=15; r[91]=15; r[92]=15; r[93]=15; r[94]=15; r[95]=9; 
	r[96]=9; r[97]=9; r[98]=9; r[99]=9; r[100]=7; r[101]=15; r[102]=15; r[103]=15; 
	r[104]=15; r[105]=3; r[106]=11; r[107]=1; 
}

private static byte[] create__resolver_scanner_eof_actions( )
{
	byte[] r = new byte[108];
	init__resolver_scanner_eof_actions_0( r );
	return r;
}

private static final byte _resolver_scanner_eof_actions[] = create__resolver_scanner_eof_actions();


static final int resolver_scanner_start = 1;
static final int resolver_scanner_error = 0;

static final int resolver_scanner_en_main = 1;

// line 55 "src/org/jvyamlb/resolver_scanner.rl"

   public String recognize(ByteList list) {
       String tag = null;
       int cs;
       int act;
       int have = 0;
       int nread = 0;
       int p=list.begin;
       int pe = p+list.realSize;
       int tokstart = -1;
       int tokend = -1;

       byte[] data = list.bytes;
       if(pe == 0) {
         data = new byte[]{(byte)'~'};
         pe = 1;
       }
              

// line 372 "src/org/jvyamlb/ResolverScanner.java"
	{
	cs = resolver_scanner_start;
	}
// line 74 "src/org/jvyamlb/resolver_scanner.rl"


// line 379 "src/org/jvyamlb/ResolverScanner.java"
	{
	int _klen;
	int _trans;
	int _keys;

	if ( p != pe ) {
	if ( cs != 0 ) {
	_resume: while ( true ) {
	_again: do {
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
	if ( cs == 0 )
		break _resume;
	if ( ++p == pe )
		break _resume;
	}
	}	}
	}
// line 76 "src/org/jvyamlb/resolver_scanner.rl"


// line 452 "src/org/jvyamlb/ResolverScanner.java"
	int _acts = _resolver_scanner_eof_actions[cs];
	int _nacts = (int) _resolver_scanner_actions[_acts++];
	while ( _nacts-- > 0 ) {
		switch ( _resolver_scanner_actions[_acts++] ) {
	case 0:
// line 10 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:bool"; }
	break;
	case 1:
// line 11 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:merge"; }
	break;
	case 2:
// line 12 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:null"; }
	break;
	case 3:
// line 13 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:timestamp#ymd"; }
	break;
	case 4:
// line 14 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:timestamp"; }
	break;
	case 5:
// line 15 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:value"; }
	break;
	case 6:
// line 16 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:float"; }
	break;
	case 7:
// line 17 "src/org/jvyamlb/resolver_scanner.rl"
	{ tag = "tag:yaml.org,2002:int"; }
	break;
// line 489 "src/org/jvyamlb/ResolverScanner.java"
		}
	}

// line 78 "src/org/jvyamlb/resolver_scanner.rl"
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
