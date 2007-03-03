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
	r[56]=161; r[57]=162; r[58]=163; r[59]=167; r[60]=168; r[61]=169; r[62]=171; r[63]=172; 
	r[64]=173; r[65]=175; r[66]=176; r[67]=177; r[68]=178; r[69]=180; r[70]=182; r[71]=183; 
	r[72]=184; r[73]=184; r[74]=188; r[75]=190; r[76]=190; r[77]=199; r[78]=206; r[79]=209; 
	r[80]=212; r[81]=219; r[82]=224; r[83]=228; r[84]=230; r[85]=234; r[86]=237; r[87]=238; 
	r[88]=247; r[89]=254; r[90]=261; r[91]=269; r[92]=275; r[93]=278; r[94]=279; r[95]=279; 
	r[96]=286; r[97]=290; r[98]=295; r[99]=300; r[100]=305; r[101]=311; r[102]=311; r[103]=311; 
}

private static short[] create__resolver_scanner_key_offsets( )
{
	short[] r = new short[104];
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
	r[152]=76; r[153]=83; r[154]=69; r[155]=108; r[156]=115; r[157]=101; r[158]=79; r[159]=111; 
	r[160]=117; r[161]=108; r[162]=108; r[163]=70; r[164]=78; r[165]=102; r[166]=110; r[167]=70; 
	r[168]=102; r[169]=82; r[170]=114; r[171]=85; r[172]=117; r[173]=69; r[174]=101; r[175]=83; 
	r[176]=115; r[177]=97; r[178]=111; r[179]=117; r[180]=102; r[181]=110; r[182]=114; r[183]=101; 
	r[184]=69; r[185]=101; r[186]=48; r[187]=57; r[188]=48; r[189]=57; r[190]=46; r[191]=58; 
	r[192]=95; r[193]=98; r[194]=120; r[195]=48; r[196]=55; r[197]=56; r[198]=57; r[199]=46; 
	r[200]=58; r[201]=95; r[202]=48; r[203]=55; r[204]=56; r[205]=57; r[206]=95; r[207]=48; 
	r[208]=55; r[209]=95; r[210]=48; r[211]=49; r[212]=95; r[213]=48; r[214]=57; r[215]=65; 
	r[216]=70; r[217]=97; r[218]=102; r[219]=46; r[220]=58; r[221]=95; r[222]=48; r[223]=57; 
	r[224]=46; r[225]=58; r[226]=48; r[227]=57; r[228]=46; r[229]=58; r[230]=58; r[231]=95; 
	r[232]=48; r[233]=57; r[234]=58; r[235]=48; r[236]=57; r[237]=58; r[238]=46; r[239]=58; 
	r[240]=95; r[241]=98; r[242]=120; r[243]=48; r[244]=55; r[245]=56; r[246]=57; r[247]=46; 
	r[248]=58; r[249]=95; r[250]=48; r[251]=55; r[252]=56; r[253]=57; r[254]=46; r[255]=58; 
	r[256]=95; r[257]=48; r[258]=55; r[259]=56; r[260]=57; r[261]=45; r[262]=46; r[263]=58; 
	r[264]=95; r[265]=48; r[266]=55; r[267]=56; r[268]=57; r[269]=9; r[270]=32; r[271]=43; 
	r[272]=45; r[273]=46; r[274]=90; r[275]=58; r[276]=48; r[277]=57; r[278]=58; r[279]=9; 
	r[280]=32; r[281]=43; r[282]=45; r[283]=90; r[284]=48; r[285]=57; r[286]=9; r[287]=32; 
	r[288]=84; r[289]=116; r[290]=46; r[291]=58; r[292]=95; r[293]=48; r[294]=57; r[295]=46; 
	r[296]=58; r[297]=95; r[298]=48; r[299]=57; r[300]=46; r[301]=58; r[302]=95; r[303]=48; 
	r[304]=57; r[305]=45; r[306]=46; r[307]=58; r[308]=95; r[309]=48; r[310]=57; r[311]=0; 
}

private static char[] create__resolver_scanner_trans_keys( )
{
	char[] r = new char[312];
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
	r[48]=2; r[49]=1; r[50]=1; r[51]=1; r[52]=1; r[53]=1; r[54]=1; r[55]=3; 
	r[56]=1; r[57]=1; r[58]=4; r[59]=1; r[60]=1; r[61]=2; r[62]=1; r[63]=1; 
	r[64]=2; r[65]=1; r[66]=1; r[67]=1; r[68]=2; r[69]=2; r[70]=1; r[71]=1; 
	r[72]=0; r[73]=2; r[74]=0; r[75]=0; r[76]=5; r[77]=3; r[78]=1; r[79]=1; 
	r[80]=1; r[81]=3; r[82]=2; r[83]=2; r[84]=2; r[85]=1; r[86]=1; r[87]=5; 
	r[88]=3; r[89]=3; r[90]=4; r[91]=6; r[92]=1; r[93]=1; r[94]=0; r[95]=5; 
	r[96]=4; r[97]=3; r[98]=3; r[99]=3; r[100]=4; r[101]=0; r[102]=0; r[103]=0; 
}

private static byte[] create__resolver_scanner_single_lengths( )
{
	byte[] r = new byte[104];
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
	r[72]=0; r[73]=1; r[74]=1; r[75]=0; r[76]=2; r[77]=2; r[78]=1; r[79]=1; 
	r[80]=3; r[81]=1; r[82]=1; r[83]=0; r[84]=1; r[85]=1; r[86]=0; r[87]=2; 
	r[88]=2; r[89]=2; r[90]=2; r[91]=0; r[92]=1; r[93]=0; r[94]=0; r[95]=1; 
	r[96]=0; r[97]=1; r[98]=1; r[99]=1; r[100]=1; r[101]=0; r[102]=0; r[103]=0; 
}

private static byte[] create__resolver_scanner_range_lengths( )
{
	byte[] r = new byte[104];
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
	r[56]=180; r[57]=182; r[58]=184; r[59]=189; r[60]=191; r[61]=193; r[62]=196; r[63]=198; 
	r[64]=200; r[65]=203; r[66]=205; r[67]=207; r[68]=209; r[69]=212; r[70]=215; r[71]=217; 
	r[72]=219; r[73]=220; r[74]=224; r[75]=226; r[76]=227; r[77]=235; r[78]=241; r[79]=244; 
	r[80]=247; r[81]=252; r[82]=257; r[83]=261; r[84]=264; r[85]=268; r[86]=271; r[87]=273; 
	r[88]=281; r[89]=287; r[90]=293; r[91]=300; r[92]=307; r[93]=310; r[94]=312; r[95]=313; 
	r[96]=320; r[97]=325; r[98]=330; r[99]=335; r[100]=340; r[101]=346; r[102]=347; r[103]=348; 
}

private static short[] create__resolver_scanner_index_offsets( )
{
	short[] r = new short[104];
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
	r[176]=74; r[177]=74; r[178]=77; r[179]=1; r[180]=78; r[181]=1; r[182]=0; r[183]=1; 
	r[184]=79; r[185]=74; r[186]=80; r[187]=74; r[188]=1; r[189]=74; r[190]=1; r[191]=74; 
	r[192]=1; r[193]=81; r[194]=82; r[195]=1; r[196]=73; r[197]=1; r[198]=76; r[199]=1; 
	r[200]=83; r[201]=84; r[202]=1; r[203]=74; r[204]=1; r[205]=74; r[206]=1; r[207]=71; 
	r[208]=1; r[209]=74; r[210]=77; r[211]=1; r[212]=80; r[213]=74; r[214]=1; r[215]=82; 
	r[216]=1; r[217]=84; r[218]=1; r[219]=1; r[220]=85; r[221]=85; r[222]=21; r[223]=1; 
	r[224]=25; r[225]=1; r[226]=1; r[227]=21; r[228]=30; r[229]=87; r[230]=88; r[231]=89; 
	r[232]=86; r[233]=29; r[234]=1; r[235]=21; r[236]=30; r[237]=87; r[238]=86; r[239]=29; 
	r[240]=1; r[241]=87; r[242]=87; r[243]=1; r[244]=33; r[245]=33; r[246]=1; r[247]=34; 
	r[248]=34; r[249]=34; r[250]=34; r[251]=1; r[252]=21; r[253]=90; r[254]=91; r[255]=20; 
	r[256]=1; r[257]=25; r[258]=90; r[259]=36; r[260]=1; r[261]=25; r[262]=90; r[263]=1; 
	r[264]=92; r[265]=91; r[266]=91; r[267]=1; r[268]=92; r[269]=38; r[270]=1; r[271]=92; 
	r[272]=1; r[273]=21; r[274]=30; r[275]=87; r[276]=88; r[277]=89; r[278]=93; r[279]=94; 
	r[280]=1; r[281]=21; r[282]=30; r[283]=87; r[284]=95; r[285]=68; r[286]=1; r[287]=21; 
	r[288]=30; r[289]=87; r[290]=96; r[291]=67; r[292]=1; r[293]=66; r[294]=21; r[295]=30; 
	r[296]=87; r[297]=86; r[298]=29; r[299]=1; r[300]=58; r[301]=58; r[302]=59; r[303]=59; 
	r[304]=97; r[305]=60; r[306]=1; r[307]=99; r[308]=98; r[309]=1; r[310]=99; r[311]=1; 
	r[312]=1; r[313]=58; r[314]=58; r[315]=59; r[316]=59; r[317]=60; r[318]=97; r[319]=1; 
	r[320]=47; r[321]=47; r[322]=49; r[323]=49; r[324]=1; r[325]=21; r[326]=90; r[327]=91; 
	r[328]=100; r[329]=1; r[330]=21; r[331]=90; r[332]=91; r[333]=101; r[334]=1; r[335]=21; 
	r[336]=90; r[337]=91; r[338]=102; r[339]=1; r[340]=66; r[341]=21; r[342]=90; r[343]=91; 
	r[344]=20; r[345]=1; r[346]=1; r[347]=1; r[348]=1; r[349]=0; 
}

private static byte[] create__resolver_scanner_indicies( )
{
	byte[] r = new byte[350];
	init__resolver_scanner_indicies_0( r );
	return r;
}

private static final byte _resolver_scanner_indicies[] = create__resolver_scanner_indicies();


private static void init__resolver_scanner_trans_targs_wi_0( byte[] r )
{
	r[0]=72; r[1]=0; r[2]=2; r[3]=18; r[4]=87; r[5]=97; r[6]=47; r[7]=102; 
	r[8]=48; r[9]=55; r[10]=58; r[11]=61; r[12]=64; r[13]=67; r[14]=68; r[15]=69; 
	r[16]=70; r[17]=71; r[18]=3; r[19]=76; r[20]=81; r[21]=73; r[22]=6; r[23]=9; 
	r[24]=5; r[25]=74; r[26]=7; r[27]=8; r[28]=75; r[29]=10; r[30]=11; r[31]=12; 
	r[32]=13; r[33]=79; r[34]=80; r[35]=82; r[36]=83; r[37]=85; r[38]=86; r[39]=19; 
	r[40]=21; r[41]=20; r[42]=22; r[43]=24; r[44]=25; r[45]=41; r[46]=26; r[47]=27; 
	r[48]=39; r[49]=40; r[50]=28; r[51]=29; r[52]=30; r[53]=31; r[54]=32; r[55]=33; 
	r[56]=34; r[57]=91; r[58]=35; r[59]=36; r[60]=94; r[61]=92; r[62]=38; r[63]=42; 
	r[64]=43; r[65]=96; r[66]=23; r[67]=44; r[68]=45; r[69]=101; r[70]=49; r[71]=52; 
	r[72]=50; r[73]=51; r[74]=103; r[75]=53; r[76]=54; r[77]=56; r[78]=57; r[79]=59; 
	r[80]=60; r[81]=62; r[82]=63; r[83]=65; r[84]=66; r[85]=4; r[86]=77; r[87]=78; 
	r[88]=14; r[89]=15; r[90]=16; r[91]=84; r[92]=17; r[93]=88; r[94]=46; r[95]=89; 
	r[96]=90; r[97]=95; r[98]=93; r[99]=37; r[100]=98; r[101]=99; r[102]=100; 
}

private static byte[] create__resolver_scanner_trans_targs_wi( )
{
	byte[] r = new byte[103];
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
	r[96]=0; r[97]=0; r[98]=0; r[99]=0; r[100]=0; r[101]=0; r[102]=0; 
}

private static byte[] create__resolver_scanner_trans_actions_wi( )
{
	byte[] r = new byte[103];
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
	r[72]=5; r[73]=13; r[74]=13; r[75]=13; r[76]=15; r[77]=15; r[78]=15; r[79]=15; 
	r[80]=15; r[81]=15; r[82]=15; r[83]=15; r[84]=15; r[85]=15; r[86]=15; r[87]=15; 
	r[88]=15; r[89]=15; r[90]=15; r[91]=9; r[92]=9; r[93]=9; r[94]=9; r[95]=9; 
	r[96]=7; r[97]=15; r[98]=15; r[99]=15; r[100]=15; r[101]=3; r[102]=11; r[103]=1; 
}

private static byte[] create__resolver_scanner_eof_actions( )
{
	byte[] r = new byte[104];
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
              

// line 355 "src/org/jvyamlb/ResolverScanner.java"
	{
	cs = resolver_scanner_start;
	}
// line 73 "src/org/jvyamlb/resolver_scanner.rl"


// line 362 "src/org/jvyamlb/ResolverScanner.java"
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


// line 434 "src/org/jvyamlb/ResolverScanner.java"
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
// line 471 "src/org/jvyamlb/ResolverScanner.java"
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
