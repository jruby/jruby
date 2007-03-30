// line 1 "src/org/jvyamlb/resolver_scanner.rl"

package org.jvyamlb;

import org.jruby.util.ByteList;

public class ResolverScanner {
// line 51 "src/org/jvyamlb/resolver_scanner.rl"



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
	r[0]=0; r[1]=0; r[2]=20; r[3]=24; r[4]=28; r[5]=30; r[6]=32; r[7]=34; 
	r[8]=35; r[9]=36; r[10]=37; r[11]=41; r[12]=45; r[13]=49; r[14]=51; r[15]=54; 
	r[16]=61; r[17]=65; r[18]=69; r[19]=75; r[20]=77; r[21]=78; r[22]=79; r[23]=80; 
	r[24]=82; r[25]=85; r[26]=87; r[27]=93; r[28]=97; r[29]=100; r[30]=101; r[31]=103; 
	r[32]=105; r[33]=106; r[34]=108; r[35]=110; r[36]=115; r[37]=117; r[38]=119; r[39]=121; 
	r[40]=125; r[41]=127; r[42]=128; r[43]=130; r[44]=136; r[45]=141; r[46]=145; r[47]=149; 
	r[48]=150; r[49]=152; r[50]=153; r[51]=154; r[52]=155; r[53]=156; r[54]=157; r[55]=158; 
	r[56]=162; r[57]=163; r[58]=164; r[59]=165; r[60]=166; r[61]=170; r[62]=171; r[63]=172; 
	r[64]=174; r[65]=175; r[66]=176; r[67]=178; r[68]=179; r[69]=180; r[70]=181; r[71]=183; 
	r[72]=185; r[73]=186; r[74]=187; r[75]=187; r[76]=191; r[77]=193; r[78]=193; r[79]=202; 
	r[80]=209; r[81]=212; r[82]=215; r[83]=222; r[84]=227; r[85]=231; r[86]=233; r[87]=237; 
	r[88]=240; r[89]=241; r[90]=250; r[91]=257; r[92]=264; r[93]=272; r[94]=278; r[95]=281; 
	r[96]=282; r[97]=282; r[98]=289; r[99]=293; r[100]=298; r[101]=303; r[102]=308; r[103]=314; 
	r[104]=314; r[105]=314; 
}

private static short[] create__resolver_scanner_key_offsets( )
{
	short[] r = new short[106];
	init__resolver_scanner_key_offsets_0( r );
	return r;
}

private static final short _resolver_scanner_key_offsets[] = create__resolver_scanner_key_offsets();


private static void init__resolver_scanner_trans_keys_0( char[] r )
{
	r[0]=32; r[1]=43; r[2]=45; r[3]=46; r[4]=48; r[5]=60; r[6]=61; r[7]=70; 
	r[8]=78; r[9]=79; r[10]=84; r[11]=89; r[12]=102; r[13]=110; r[14]=111; r[15]=116; 
	r[16]=121; r[17]=126; r[18]=49; r[19]=57; r[20]=46; r[21]=48; r[22]=49; r[23]=57; 
	r[24]=73; r[25]=105; r[26]=48; r[27]=57; r[28]=43; r[29]=45; r[30]=48; r[31]=57; 
	r[32]=78; r[33]=110; r[34]=70; r[35]=102; r[36]=110; r[37]=46; r[38]=58; r[39]=48; 
	r[40]=57; r[41]=48; r[42]=53; r[43]=54; r[44]=57; r[45]=46; r[46]=58; r[47]=48; 
	r[48]=57; r[49]=46; r[50]=58; r[51]=95; r[52]=48; r[53]=49; r[54]=95; r[55]=48; 
	r[56]=57; r[57]=65; r[58]=70; r[59]=97; r[60]=102; r[61]=48; r[62]=53; r[63]=54; 
	r[64]=57; r[65]=48; r[66]=53; r[67]=54; r[68]=57; r[69]=73; r[70]=78; r[71]=105; 
	r[72]=110; r[73]=48; r[74]=57; r[75]=65; r[76]=97; r[77]=78; r[78]=97; r[79]=110; 
	r[80]=48; r[81]=57; r[82]=45; r[83]=48; r[84]=57; r[85]=48; r[86]=57; r[87]=9; 
	r[88]=32; r[89]=84; r[90]=116; r[91]=48; r[92]=57; r[93]=9; r[94]=32; r[95]=48; 
	r[96]=57; r[97]=58; r[98]=48; r[99]=57; r[100]=58; r[101]=48; r[102]=57; r[103]=48; 
	r[104]=57; r[105]=58; r[106]=48; r[107]=57; r[108]=48; r[109]=57; r[110]=9; r[111]=32; 
	r[112]=43; r[113]=45; r[114]=90; r[115]=48; r[116]=57; r[117]=48; r[118]=57; r[119]=48; 
	r[120]=57; r[121]=9; r[122]=32; r[123]=84; r[124]=116; r[125]=48; r[126]=57; r[127]=45; 
	r[128]=48; r[129]=57; r[130]=9; r[131]=32; r[132]=84; r[133]=116; r[134]=48; r[135]=57; 
	r[136]=45; r[137]=46; r[138]=58; r[139]=48; r[140]=57; r[141]=46; r[142]=58; r[143]=48; 
	r[144]=57; r[145]=46; r[146]=58; r[147]=48; r[148]=57; r[149]=60; r[150]=65; r[151]=97; 
	r[152]=76; r[153]=83; r[154]=69; r[155]=108; r[156]=115; r[157]=101; r[158]=79; r[159]=85; 
	r[160]=111; r[161]=117; r[162]=76; r[163]=76; r[164]=108; r[165]=108; r[166]=70; r[167]=78; 
	r[168]=102; r[169]=110; r[170]=70; r[171]=102; r[172]=82; r[173]=114; r[174]=85; r[175]=117; 
	r[176]=69; r[177]=101; r[178]=83; r[179]=115; r[180]=97; r[181]=111; r[182]=117; r[183]=102; 
	r[184]=110; r[185]=114; r[186]=101; r[187]=69; r[188]=101; r[189]=48; r[190]=57; r[191]=48; 
	r[192]=57; r[193]=46; r[194]=58; r[195]=95; r[196]=98; r[197]=120; r[198]=48; r[199]=55; 
	r[200]=56; r[201]=57; r[202]=46; r[203]=58; r[204]=95; r[205]=48; r[206]=55; r[207]=56; 
	r[208]=57; r[209]=95; r[210]=48; r[211]=55; r[212]=95; r[213]=48; r[214]=49; r[215]=95; 
	r[216]=48; r[217]=57; r[218]=65; r[219]=70; r[220]=97; r[221]=102; r[222]=46; r[223]=58; 
	r[224]=95; r[225]=48; r[226]=57; r[227]=46; r[228]=58; r[229]=48; r[230]=57; r[231]=46; 
	r[232]=58; r[233]=58; r[234]=95; r[235]=48; r[236]=57; r[237]=58; r[238]=48; r[239]=57; 
	r[240]=58; r[241]=46; r[242]=58; r[243]=95; r[244]=98; r[245]=120; r[246]=48; r[247]=55; 
	r[248]=56; r[249]=57; r[250]=46; r[251]=58; r[252]=95; r[253]=48; r[254]=55; r[255]=56; 
	r[256]=57; r[257]=46; r[258]=58; r[259]=95; r[260]=48; r[261]=55; r[262]=56; r[263]=57; 
	r[264]=45; r[265]=46; r[266]=58; r[267]=95; r[268]=48; r[269]=55; r[270]=56; r[271]=57; 
	r[272]=9; r[273]=32; r[274]=43; r[275]=45; r[276]=46; r[277]=90; r[278]=58; r[279]=48; 
	r[280]=57; r[281]=58; r[282]=9; r[283]=32; r[284]=43; r[285]=45; r[286]=90; r[287]=48; 
	r[288]=57; r[289]=9; r[290]=32; r[291]=84; r[292]=116; r[293]=46; r[294]=58; r[295]=95; 
	r[296]=48; r[297]=57; r[298]=46; r[299]=58; r[300]=95; r[301]=48; r[302]=57; r[303]=46; 
	r[304]=58; r[305]=95; r[306]=48; r[307]=57; r[308]=45; r[309]=46; r[310]=58; r[311]=95; 
	r[312]=48; r[313]=57; r[314]=0; 
}

private static char[] create__resolver_scanner_trans_keys( )
{
	char[] r = new char[315];
	init__resolver_scanner_trans_keys_0( r );
	return r;
}

private static final char _resolver_scanner_trans_keys[] = create__resolver_scanner_trans_keys();


private static void init__resolver_scanner_single_lengths_0( byte[] r )
{
	r[0]=0; r[1]=18; r[2]=2; r[3]=2; r[4]=2; r[5]=0; r[6]=2; r[7]=1; 
	r[8]=1; r[9]=1; r[10]=2; r[11]=0; r[12]=2; r[13]=2; r[14]=1; r[15]=1; 
	r[16]=0; r[17]=0; r[18]=4; r[19]=2; r[20]=1; r[21]=1; r[22]=1; r[23]=0; 
	r[24]=1; r[25]=0; r[26]=4; r[27]=2; r[28]=1; r[29]=1; r[30]=0; r[31]=0; 
	r[32]=1; r[33]=0; r[34]=0; r[35]=5; r[36]=0; r[37]=0; r[38]=0; r[39]=4; 
	r[40]=0; r[41]=1; r[42]=0; r[43]=4; r[44]=3; r[45]=2; r[46]=2; r[47]=1; 
	r[48]=2; r[49]=1; r[50]=1; r[51]=1; r[52]=1; r[53]=1; r[54]=1; r[55]=4; 
	r[56]=1; r[57]=1; r[58]=1; r[59]=1; r[60]=4; r[61]=1; r[62]=1; r[63]=2; 
	r[64]=1; r[65]=1; r[66]=2; r[67]=1; r[68]=1; r[69]=1; r[70]=2; r[71]=2; 
	r[72]=1; r[73]=1; r[74]=0; r[75]=2; r[76]=0; r[77]=0; r[78]=5; r[79]=3; 
	r[80]=1; r[81]=1; r[82]=1; r[83]=3; r[84]=2; r[85]=2; r[86]=2; r[87]=1; 
	r[88]=1; r[89]=5; r[90]=3; r[91]=3; r[92]=4; r[93]=6; r[94]=1; r[95]=1; 
	r[96]=0; r[97]=5; r[98]=4; r[99]=3; r[100]=3; r[101]=3; r[102]=4; r[103]=0; 
	r[104]=0; r[105]=0; 
}

private static byte[] create__resolver_scanner_single_lengths( )
{
	byte[] r = new byte[106];
	init__resolver_scanner_single_lengths_0( r );
	return r;
}

private static final byte _resolver_scanner_single_lengths[] = create__resolver_scanner_single_lengths();


private static void init__resolver_scanner_range_lengths_0( byte[] r )
{
	r[0]=0; r[1]=1; r[2]=1; r[3]=1; r[4]=0; r[5]=1; r[6]=0; r[7]=0; 
	r[8]=0; r[9]=0; r[10]=1; r[11]=2; r[12]=1; r[13]=0; r[14]=1; r[15]=3; 
	r[16]=2; r[17]=2; r[18]=1; r[19]=0; r[20]=0; r[21]=0; r[22]=0; r[23]=1; 
	r[24]=1; r[25]=1; r[26]=1; r[27]=1; r[28]=1; r[29]=0; r[30]=1; r[31]=1; 
	r[32]=0; r[33]=1; r[34]=1; r[35]=0; r[36]=1; r[37]=1; r[38]=1; r[39]=0; 
	r[40]=1; r[41]=0; r[42]=1; r[43]=1; r[44]=1; r[45]=1; r[46]=1; r[47]=0; 
	r[48]=0; r[49]=0; r[50]=0; r[51]=0; r[52]=0; r[53]=0; r[54]=0; r[55]=0; 
	r[56]=0; r[57]=0; r[58]=0; r[59]=0; r[60]=0; r[61]=0; r[62]=0; r[63]=0; 
	r[64]=0; r[65]=0; r[66]=0; r[67]=0; r[68]=0; r[69]=0; r[70]=0; r[71]=0; 
	r[72]=0; r[73]=0; r[74]=0; r[75]=1; r[76]=1; r[77]=0; r[78]=2; r[79]=2; 
	r[80]=1; r[81]=1; r[82]=3; r[83]=1; r[84]=1; r[85]=0; r[86]=1; r[87]=1; 
	r[88]=0; r[89]=2; r[90]=2; r[91]=2; r[92]=2; r[93]=0; r[94]=1; r[95]=0; 
	r[96]=0; r[97]=1; r[98]=0; r[99]=1; r[100]=1; r[101]=1; r[102]=1; r[103]=0; 
	r[104]=0; r[105]=0; 
}

private static byte[] create__resolver_scanner_range_lengths( )
{
	byte[] r = new byte[106];
	init__resolver_scanner_range_lengths_0( r );
	return r;
}

private static final byte _resolver_scanner_range_lengths[] = create__resolver_scanner_range_lengths();


private static void init__resolver_scanner_index_offsets_0( short[] r )
{
	r[0]=0; r[1]=0; r[2]=20; r[3]=24; r[4]=28; r[5]=31; r[6]=33; r[7]=36; 
	r[8]=38; r[9]=40; r[10]=42; r[11]=46; r[12]=49; r[13]=53; r[14]=56; r[15]=59; 
	r[16]=64; r[17]=67; r[18]=70; r[19]=76; r[20]=79; r[21]=81; r[22]=83; r[23]=85; 
	r[24]=87; r[25]=90; r[26]=92; r[27]=98; r[28]=102; r[29]=105; r[30]=107; r[31]=109; 
	r[32]=111; r[33]=113; r[34]=115; r[35]=117; r[36]=123; r[37]=125; r[38]=127; r[39]=129; 
	r[40]=134; r[41]=136; r[42]=138; r[43]=140; r[44]=146; r[45]=151; r[46]=155; r[47]=159; 
	r[48]=161; r[49]=164; r[50]=166; r[51]=168; r[52]=170; r[53]=172; r[54]=174; r[55]=176; 
	r[56]=181; r[57]=183; r[58]=185; r[59]=187; r[60]=189; r[61]=194; r[62]=196; r[63]=198; 
	r[64]=201; r[65]=203; r[66]=205; r[67]=208; r[68]=210; r[69]=212; r[70]=214; r[71]=217; 
	r[72]=220; r[73]=222; r[74]=224; r[75]=225; r[76]=229; r[77]=231; r[78]=232; r[79]=240; 
	r[80]=246; r[81]=249; r[82]=252; r[83]=257; r[84]=262; r[85]=266; r[86]=269; r[87]=273; 
	r[88]=276; r[89]=278; r[90]=286; r[91]=292; r[92]=298; r[93]=305; r[94]=312; r[95]=315; 
	r[96]=317; r[97]=318; r[98]=325; r[99]=330; r[100]=335; r[101]=340; r[102]=345; r[103]=351; 
	r[104]=352; r[105]=353; 
}

private static short[] create__resolver_scanner_index_offsets( )
{
	short[] r = new short[106];
	init__resolver_scanner_index_offsets_0( r );
	return r;
}

private static final short _resolver_scanner_index_offsets[] = create__resolver_scanner_index_offsets();


private static void init__resolver_scanner_indicies_0( byte[] r )
{
	r[0]=0; r[1]=2; r[2]=2; r[3]=3; r[4]=4; r[5]=6; r[6]=7; r[7]=8; 
	r[8]=9; r[9]=10; r[10]=11; r[11]=12; r[12]=13; r[13]=14; r[14]=15; r[15]=16; 
	r[16]=17; r[17]=0; r[18]=5; r[19]=1; r[20]=18; r[21]=19; r[22]=20; r[23]=1; 
	r[24]=22; r[25]=23; r[26]=21; r[27]=1; r[28]=24; r[29]=24; r[30]=1; r[31]=25; 
	r[32]=1; r[33]=26; r[34]=27; r[35]=1; r[36]=28; r[37]=1; r[38]=28; r[39]=1; 
	r[40]=27; r[41]=1; r[42]=21; r[43]=30; r[44]=29; r[45]=1; r[46]=31; r[47]=32; 
	r[48]=1; r[49]=25; r[50]=30; r[51]=32; r[52]=1; r[53]=25; r[54]=30; r[55]=1; 
	r[56]=33; r[57]=33; r[58]=1; r[59]=34; r[60]=34; r[61]=34; r[62]=34; r[63]=1; 
	r[64]=35; r[65]=36; r[66]=1; r[67]=37; r[68]=38; r[69]=1; r[70]=22; r[71]=39; 
	r[72]=23; r[73]=40; r[74]=21; r[75]=1; r[76]=41; r[77]=41; r[78]=1; r[79]=28; 
	r[80]=1; r[81]=42; r[82]=1; r[83]=28; r[84]=1; r[85]=43; r[86]=1; r[87]=44; 
	r[88]=45; r[89]=1; r[90]=46; r[91]=1; r[92]=47; r[93]=47; r[94]=49; r[95]=49; 
	r[96]=48; r[97]=1; r[98]=47; r[99]=47; r[100]=50; r[101]=1; r[102]=52; r[103]=51; 
	r[104]=1; r[105]=52; r[106]=1; r[107]=53; r[108]=1; r[109]=54; r[110]=1; r[111]=55; 
	r[112]=1; r[113]=56; r[114]=1; r[115]=57; r[116]=1; r[117]=58; r[118]=58; r[119]=59; 
	r[120]=59; r[121]=60; r[122]=1; r[123]=61; r[124]=1; r[125]=62; r[126]=1; r[127]=60; 
	r[128]=1; r[129]=47; r[130]=47; r[131]=49; r[132]=49; r[133]=1; r[134]=50; r[135]=1; 
	r[136]=63; r[137]=1; r[138]=64; r[139]=1; r[140]=47; r[141]=47; r[142]=49; r[143]=49; 
	r[144]=65; r[145]=1; r[146]=66; r[147]=21; r[148]=30; r[149]=29; r[150]=1; r[151]=21; 
	r[152]=30; r[153]=67; r[154]=1; r[155]=21; r[156]=30; r[157]=68; r[158]=1; r[159]=69; 
	r[160]=1; r[161]=70; r[162]=71; r[163]=1; r[164]=72; r[165]=1; r[166]=73; r[167]=1; 
	r[168]=74; r[169]=1; r[170]=75; r[171]=1; r[172]=76; r[173]=1; r[174]=74; r[175]=1; 
	r[176]=74; r[177]=77; r[178]=74; r[179]=78; r[180]=1; r[181]=79; r[182]=1; r[183]=0; 
	r[184]=1; r[185]=80; r[186]=1; r[187]=0; r[188]=1; r[189]=81; r[190]=74; r[191]=82; 
	r[192]=74; r[193]=1; r[194]=74; r[195]=1; r[196]=74; r[197]=1; r[198]=83; r[199]=84; 
	r[200]=1; r[201]=73; r[202]=1; r[203]=76; r[204]=1; r[205]=85; r[206]=86; r[207]=1; 
	r[208]=74; r[209]=1; r[210]=74; r[211]=1; r[212]=71; r[213]=1; r[214]=74; r[215]=78; 
	r[216]=1; r[217]=82; r[218]=74; r[219]=1; r[220]=84; r[221]=1; r[222]=86; r[223]=1; 
	r[224]=1; r[225]=87; r[226]=87; r[227]=21; r[228]=1; r[229]=25; r[230]=1; r[231]=1; 
	r[232]=21; r[233]=30; r[234]=89; r[235]=90; r[236]=91; r[237]=88; r[238]=29; r[239]=1; 
	r[240]=21; r[241]=30; r[242]=89; r[243]=88; r[244]=29; r[245]=1; r[246]=89; r[247]=89; 
	r[248]=1; r[249]=33; r[250]=33; r[251]=1; r[252]=34; r[253]=34; r[254]=34; r[255]=34; 
	r[256]=1; r[257]=21; r[258]=92; r[259]=93; r[260]=20; r[261]=1; r[262]=25; r[263]=92; 
	r[264]=36; r[265]=1; r[266]=25; r[267]=92; r[268]=1; r[269]=94; r[270]=93; r[271]=93; 
	r[272]=1; r[273]=94; r[274]=38; r[275]=1; r[276]=94; r[277]=1; r[278]=21; r[279]=30; 
	r[280]=89; r[281]=90; r[282]=91; r[283]=95; r[284]=96; r[285]=1; r[286]=21; r[287]=30; 
	r[288]=89; r[289]=97; r[290]=68; r[291]=1; r[292]=21; r[293]=30; r[294]=89; r[295]=98; 
	r[296]=67; r[297]=1; r[298]=66; r[299]=21; r[300]=30; r[301]=89; r[302]=88; r[303]=29; 
	r[304]=1; r[305]=58; r[306]=58; r[307]=59; r[308]=59; r[309]=99; r[310]=60; r[311]=1; 
	r[312]=101; r[313]=100; r[314]=1; r[315]=101; r[316]=1; r[317]=1; r[318]=58; r[319]=58; 
	r[320]=59; r[321]=59; r[322]=60; r[323]=99; r[324]=1; r[325]=47; r[326]=47; r[327]=49; 
	r[328]=49; r[329]=1; r[330]=21; r[331]=92; r[332]=93; r[333]=102; r[334]=1; r[335]=21; 
	r[336]=92; r[337]=93; r[338]=103; r[339]=1; r[340]=21; r[341]=92; r[342]=93; r[343]=104; 
	r[344]=1; r[345]=66; r[346]=21; r[347]=92; r[348]=93; r[349]=20; r[350]=1; r[351]=1; 
	r[352]=1; r[353]=1; r[354]=0; 
}

private static byte[] create__resolver_scanner_indicies( )
{
	byte[] r = new byte[355];
	init__resolver_scanner_indicies_0( r );
	return r;
}

private static final byte _resolver_scanner_indicies[] = create__resolver_scanner_indicies();


private static void init__resolver_scanner_trans_targs_wi_0( byte[] r )
{
	r[0]=74; r[1]=0; r[2]=2; r[3]=18; r[4]=89; r[5]=99; r[6]=47; r[7]=104; 
	r[8]=48; r[9]=55; r[10]=60; r[11]=63; r[12]=66; r[13]=69; r[14]=70; r[15]=71; 
	r[16]=72; r[17]=73; r[18]=3; r[19]=78; r[20]=83; r[21]=75; r[22]=6; r[23]=9; 
	r[24]=5; r[25]=76; r[26]=7; r[27]=8; r[28]=77; r[29]=10; r[30]=11; r[31]=12; 
	r[32]=13; r[33]=81; r[34]=82; r[35]=84; r[36]=85; r[37]=87; r[38]=88; r[39]=19; 
	r[40]=21; r[41]=20; r[42]=22; r[43]=24; r[44]=25; r[45]=41; r[46]=26; r[47]=27; 
	r[48]=39; r[49]=40; r[50]=28; r[51]=29; r[52]=30; r[53]=31; r[54]=32; r[55]=33; 
	r[56]=34; r[57]=93; r[58]=35; r[59]=36; r[60]=96; r[61]=94; r[62]=38; r[63]=42; 
	r[64]=43; r[65]=98; r[66]=23; r[67]=44; r[68]=45; r[69]=103; r[70]=49; r[71]=52; 
	r[72]=50; r[73]=51; r[74]=105; r[75]=53; r[76]=54; r[77]=56; r[78]=58; r[79]=57; 
	r[80]=59; r[81]=61; r[82]=62; r[83]=64; r[84]=65; r[85]=67; r[86]=68; r[87]=4; 
	r[88]=79; r[89]=80; r[90]=14; r[91]=15; r[92]=16; r[93]=86; r[94]=17; r[95]=90; 
	r[96]=46; r[97]=91; r[98]=92; r[99]=97; r[100]=95; r[101]=37; r[102]=100; r[103]=101; 
	r[104]=102; 
}

private static byte[] create__resolver_scanner_trans_targs_wi( )
{
	byte[] r = new byte[105];
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
	r[104]=0; 
}

private static byte[] create__resolver_scanner_trans_actions_wi( )
{
	byte[] r = new byte[105];
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
	r[72]=0; r[73]=0; r[74]=5; r[75]=13; r[76]=13; r[77]=13; r[78]=15; r[79]=15; 
	r[80]=15; r[81]=15; r[82]=15; r[83]=15; r[84]=15; r[85]=15; r[86]=15; r[87]=15; 
	r[88]=15; r[89]=15; r[90]=15; r[91]=15; r[92]=15; r[93]=9; r[94]=9; r[95]=9; 
	r[96]=9; r[97]=9; r[98]=7; r[99]=15; r[100]=15; r[101]=15; r[102]=15; r[103]=3; 
	r[104]=11; r[105]=1; 
}

private static byte[] create__resolver_scanner_eof_actions( )
{
	byte[] r = new byte[106];
	init__resolver_scanner_eof_actions_0( r );
	return r;
}

private static final byte _resolver_scanner_eof_actions[] = create__resolver_scanner_eof_actions();


static final int resolver_scanner_start = 1;

static final int resolver_scanner_error = 0;

// line 54 "src/org/jvyamlb/resolver_scanner.rl"

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
              

// line 364 "src/org/jvyamlb/ResolverScanner.java"
	{
	cs = resolver_scanner_start;
	}
// line 73 "src/org/jvyamlb/resolver_scanner.rl"


// line 371 "src/org/jvyamlb/ResolverScanner.java"
	{
	int _klen;
	int _trans;
	int _keys;

	if ( p != pe ) {
	_resume: while ( true ) {
	_again: do {
	if ( cs == 0 )
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
// line 75 "src/org/jvyamlb/resolver_scanner.rl"


// line 443 "src/org/jvyamlb/ResolverScanner.java"
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
// line 480 "src/org/jvyamlb/ResolverScanner.java"
		}
	}

// line 77 "src/org/jvyamlb/resolver_scanner.rl"
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
