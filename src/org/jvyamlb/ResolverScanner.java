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
	r[32]=111; r[33]=113; r[34]=114; r[35]=116; r[36]=118; r[37]=124; r[38]=129; r[39]=131; 
	r[40]=133; r[41]=135; r[42]=142; r[43]=146; r[44]=148; r[45]=149; r[46]=151; r[47]=157; 
	r[48]=163; r[49]=168; r[50]=173; r[51]=174; r[52]=176; r[53]=177; r[54]=178; r[55]=179; 
	r[56]=180; r[57]=181; r[58]=182; r[59]=186; r[60]=187; r[61]=188; r[62]=189; r[63]=190; 
	r[64]=194; r[65]=195; r[66]=196; r[67]=198; r[68]=199; r[69]=200; r[70]=202; r[71]=203; 
	r[72]=204; r[73]=205; r[74]=207; r[75]=209; r[76]=210; r[77]=211; r[78]=211; r[79]=215; 
	r[80]=217; r[81]=217; r[82]=227; r[83]=235; r[84]=238; r[85]=241; r[86]=249; r[87]=255; 
	r[88]=261; r[89]=264; r[90]=265; r[91]=270; r[92]=274; r[93]=276; r[94]=286; r[95]=294; 
	r[96]=302; r[97]=311; r[98]=314; r[99]=315; r[100]=315; r[101]=319; r[102]=325; r[103]=331; 
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
	r[120]=43; r[121]=45; r[122]=46; r[123]=90; r[124]=9; r[125]=32; r[126]=43; r[127]=45; 
	r[128]=90; r[129]=48; r[130]=57; r[131]=48; r[132]=57; r[133]=48; r[134]=57; r[135]=9; 
	r[136]=32; r[137]=43; r[138]=45; r[139]=90; r[140]=48; r[141]=57; r[142]=9; r[143]=32; 
	r[144]=84; r[145]=116; r[146]=48; r[147]=57; r[148]=45; r[149]=48; r[150]=57; r[151]=9; 
	r[152]=32; r[153]=84; r[154]=116; r[155]=48; r[156]=57; r[157]=44; r[158]=45; r[159]=46; 
	r[160]=58; r[161]=48; r[162]=57; r[163]=44; r[164]=46; r[165]=58; r[166]=48; r[167]=57; 
	r[168]=44; r[169]=46; r[170]=58; r[171]=48; r[172]=57; r[173]=60; r[174]=65; r[175]=97; 
	r[176]=76; r[177]=83; r[178]=69; r[179]=108; r[180]=115; r[181]=101; r[182]=79; r[183]=85; 
	r[184]=111; r[185]=117; r[186]=76; r[187]=76; r[188]=108; r[189]=108; r[190]=70; r[191]=78; 
	r[192]=102; r[193]=110; r[194]=70; r[195]=102; r[196]=82; r[197]=114; r[198]=85; r[199]=117; 
	r[200]=69; r[201]=101; r[202]=83; r[203]=115; r[204]=97; r[205]=111; r[206]=117; r[207]=102; 
	r[208]=110; r[209]=114; r[210]=101; r[211]=69; r[212]=101; r[213]=48; r[214]=57; r[215]=48; 
	r[216]=57; r[217]=44; r[218]=46; r[219]=58; r[220]=95; r[221]=98; r[222]=120; r[223]=48; 
	r[224]=55; r[225]=56; r[226]=57; r[227]=44; r[228]=46; r[229]=58; r[230]=95; r[231]=48; 
	r[232]=55; r[233]=56; r[234]=57; r[235]=95; r[236]=48; r[237]=55; r[238]=95; r[239]=48; 
	r[240]=49; r[241]=44; r[242]=95; r[243]=48; r[244]=57; r[245]=65; r[246]=70; r[247]=97; 
	r[248]=102; r[249]=44; r[250]=46; r[251]=58; r[252]=95; r[253]=48; r[254]=57; r[255]=44; 
	r[256]=46; r[257]=58; r[258]=95; r[259]=48; r[260]=57; r[261]=58; r[262]=48; r[263]=57; 
	r[264]=58; r[265]=44; r[266]=58; r[267]=95; r[268]=48; r[269]=57; r[270]=46; r[271]=58; 
	r[272]=48; r[273]=57; r[274]=46; r[275]=58; r[276]=44; r[277]=46; r[278]=58; r[279]=95; 
	r[280]=98; r[281]=120; r[282]=48; r[283]=55; r[284]=56; r[285]=57; r[286]=44; r[287]=46; 
	r[288]=58; r[289]=95; r[290]=48; r[291]=55; r[292]=56; r[293]=57; r[294]=44; r[295]=46; 
	r[296]=58; r[297]=95; r[298]=48; r[299]=55; r[300]=56; r[301]=57; r[302]=44; r[303]=45; 
	r[304]=46; r[305]=58; r[306]=95; r[307]=48; r[308]=55; r[309]=56; r[310]=57; r[311]=58; 
	r[312]=48; r[313]=57; r[314]=58; r[315]=9; r[316]=32; r[317]=84; r[318]=116; r[319]=44; 
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
	r[32]=0; r[33]=1; r[34]=0; r[35]=0; r[36]=6; r[37]=5; r[38]=0; r[39]=0; 
	r[40]=0; r[41]=5; r[42]=4; r[43]=0; r[44]=1; r[45]=0; r[46]=4; r[47]=4; 
	r[48]=3; r[49]=3; r[50]=1; r[51]=2; r[52]=1; r[53]=1; r[54]=1; r[55]=1; 
	r[56]=1; r[57]=1; r[58]=4; r[59]=1; r[60]=1; r[61]=1; r[62]=1; r[63]=4; 
	r[64]=1; r[65]=1; r[66]=2; r[67]=1; r[68]=1; r[69]=2; r[70]=1; r[71]=1; 
	r[72]=1; r[73]=2; r[74]=2; r[75]=1; r[76]=1; r[77]=0; r[78]=2; r[79]=0; 
	r[80]=0; r[81]=6; r[82]=4; r[83]=1; r[84]=1; r[85]=2; r[86]=4; r[87]=4; 
	r[88]=1; r[89]=1; r[90]=3; r[91]=2; r[92]=2; r[93]=6; r[94]=4; r[95]=4; 
	r[96]=5; r[97]=1; r[98]=1; r[99]=0; r[100]=4; r[101]=4; r[102]=4; r[103]=4; 
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
	r[32]=1; r[33]=0; r[34]=1; r[35]=1; r[36]=0; r[37]=0; r[38]=1; r[39]=1; 
	r[40]=1; r[41]=1; r[42]=0; r[43]=1; r[44]=0; r[45]=1; r[46]=1; r[47]=1; 
	r[48]=1; r[49]=1; r[50]=0; r[51]=0; r[52]=0; r[53]=0; r[54]=0; r[55]=0; 
	r[56]=0; r[57]=0; r[58]=0; r[59]=0; r[60]=0; r[61]=0; r[62]=0; r[63]=0; 
	r[64]=0; r[65]=0; r[66]=0; r[67]=0; r[68]=0; r[69]=0; r[70]=0; r[71]=0; 
	r[72]=0; r[73]=0; r[74]=0; r[75]=0; r[76]=0; r[77]=0; r[78]=1; r[79]=1; 
	r[80]=0; r[81]=2; r[82]=2; r[83]=1; r[84]=1; r[85]=3; r[86]=1; r[87]=1; 
	r[88]=1; r[89]=0; r[90]=1; r[91]=1; r[92]=0; r[93]=2; r[94]=2; r[95]=2; 
	r[96]=2; r[97]=1; r[98]=0; r[99]=0; r[100]=0; r[101]=1; r[102]=1; r[103]=1; 
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
	r[32]=116; r[33]=118; r[34]=120; r[35]=122; r[36]=124; r[37]=131; r[38]=137; r[39]=139; 
	r[40]=141; r[41]=143; r[42]=150; r[43]=155; r[44]=157; r[45]=159; r[46]=161; r[47]=167; 
	r[48]=173; r[49]=178; r[50]=183; r[51]=185; r[52]=188; r[53]=190; r[54]=192; r[55]=194; 
	r[56]=196; r[57]=198; r[58]=200; r[59]=205; r[60]=207; r[61]=209; r[62]=211; r[63]=213; 
	r[64]=218; r[65]=220; r[66]=222; r[67]=225; r[68]=227; r[69]=229; r[70]=232; r[71]=234; 
	r[72]=236; r[73]=238; r[74]=241; r[75]=244; r[76]=246; r[77]=248; r[78]=249; r[79]=253; 
	r[80]=255; r[81]=256; r[82]=265; r[83]=272; r[84]=275; r[85]=278; r[86]=284; r[87]=290; 
	r[88]=296; r[89]=299; r[90]=301; r[91]=306; r[92]=310; r[93]=313; r[94]=322; r[95]=329; 
	r[96]=336; r[97]=344; r[98]=347; r[99]=349; r[100]=350; r[101]=355; r[102]=361; r[103]=367; 
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
	r[128]=61; r[129]=62; r[130]=1; r[131]=59; r[132]=59; r[133]=60; r[134]=60; r[135]=62; 
	r[136]=1; r[137]=63; r[138]=1; r[139]=64; r[140]=1; r[141]=62; r[142]=1; r[143]=59; 
	r[144]=59; r[145]=60; r[146]=60; r[147]=62; r[148]=61; r[149]=1; r[150]=48; r[151]=48; 
	r[152]=50; r[153]=50; r[154]=1; r[155]=51; r[156]=1; r[157]=65; r[158]=1; r[159]=66; 
	r[160]=1; r[161]=48; r[162]=48; r[163]=50; r[164]=50; r[165]=67; r[166]=1; r[167]=3; 
	r[168]=68; r[169]=22; r[170]=31; r[171]=30; r[172]=1; r[173]=3; r[174]=22; r[175]=31; 
	r[176]=69; r[177]=1; r[178]=3; r[179]=22; r[180]=31; r[181]=70; r[182]=1; r[183]=71; 
	r[184]=1; r[185]=72; r[186]=73; r[187]=1; r[188]=74; r[189]=1; r[190]=75; r[191]=1; 
	r[192]=76; r[193]=1; r[194]=77; r[195]=1; r[196]=78; r[197]=1; r[198]=76; r[199]=1; 
	r[200]=76; r[201]=79; r[202]=76; r[203]=80; r[204]=1; r[205]=81; r[206]=1; r[207]=0; 
	r[208]=1; r[209]=82; r[210]=1; r[211]=0; r[212]=1; r[213]=83; r[214]=76; r[215]=84; 
	r[216]=76; r[217]=1; r[218]=76; r[219]=1; r[220]=76; r[221]=1; r[222]=85; r[223]=86; 
	r[224]=1; r[225]=75; r[226]=1; r[227]=78; r[228]=1; r[229]=87; r[230]=88; r[231]=1; 
	r[232]=76; r[233]=1; r[234]=76; r[235]=1; r[236]=73; r[237]=1; r[238]=76; r[239]=80; 
	r[240]=1; r[241]=84; r[242]=76; r[243]=1; r[244]=86; r[245]=1; r[246]=88; r[247]=1; 
	r[248]=1; r[249]=89; r[250]=89; r[251]=22; r[252]=1; r[253]=24; r[254]=1; r[255]=1; 
	r[256]=3; r[257]=22; r[258]=31; r[259]=91; r[260]=92; r[261]=93; r[262]=90; r[263]=30; 
	r[264]=1; r[265]=3; r[266]=22; r[267]=31; r[268]=91; r[269]=90; r[270]=30; r[271]=1; 
	r[272]=91; r[273]=91; r[274]=1; r[275]=34; r[276]=34; r[277]=1; r[278]=35; r[279]=35; 
	r[280]=35; r[281]=35; r[282]=35; r[283]=1; r[284]=94; r[285]=22; r[286]=95; r[287]=96; 
	r[288]=21; r[289]=1; r[290]=94; r[291]=22; r[292]=97; r[293]=96; r[294]=94; r[295]=1; 
	r[296]=97; r[297]=37; r[298]=1; r[299]=97; r[300]=1; r[301]=96; r[302]=97; r[303]=96; 
	r[304]=96; r[305]=1; r[306]=24; r[307]=95; r[308]=39; r[309]=1; r[310]=24; r[311]=95; 
	r[312]=1; r[313]=3; r[314]=22; r[315]=31; r[316]=91; r[317]=92; r[318]=93; r[319]=98; 
	r[320]=99; r[321]=1; r[322]=3; r[323]=22; r[324]=31; r[325]=91; r[326]=100; r[327]=70; 
	r[328]=1; r[329]=3; r[330]=22; r[331]=31; r[332]=91; r[333]=101; r[334]=69; r[335]=1; 
	r[336]=3; r[337]=68; r[338]=22; r[339]=31; r[340]=91; r[341]=90; r[342]=30; r[343]=1; 
	r[344]=103; r[345]=102; r[346]=1; r[347]=103; r[348]=1; r[349]=1; r[350]=48; r[351]=48; 
	r[352]=50; r[353]=50; r[354]=1; r[355]=94; r[356]=22; r[357]=95; r[358]=96; r[359]=104; 
	r[360]=1; r[361]=94; r[362]=22; r[363]=95; r[364]=96; r[365]=105; r[366]=1; r[367]=94; 
	r[368]=22; r[369]=95; r[370]=96; r[371]=106; r[372]=1; r[373]=94; r[374]=68; r[375]=22; 
	r[376]=95; r[377]=96; r[378]=21; r[379]=1; r[380]=1; r[381]=1; r[382]=1; r[383]=0; 
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
	r[0]=77; r[1]=0; r[2]=2; r[3]=3; r[4]=19; r[5]=93; r[6]=101; r[7]=50; 
	r[8]=106; r[9]=51; r[10]=58; r[11]=63; r[12]=66; r[13]=69; r[14]=72; r[15]=73; 
	r[16]=74; r[17]=75; r[18]=76; r[19]=6; r[20]=81; r[21]=86; r[22]=78; r[23]=5; 
	r[24]=79; r[25]=7; r[26]=10; r[27]=8; r[28]=9; r[29]=80; r[30]=11; r[31]=12; 
	r[32]=13; r[33]=14; r[34]=84; r[35]=85; r[36]=88; r[37]=89; r[38]=91; r[39]=92; 
	r[40]=20; r[41]=22; r[42]=21; r[43]=23; r[44]=25; r[45]=26; r[46]=44; r[47]=27; 
	r[48]=28; r[49]=42; r[50]=43; r[51]=29; r[52]=30; r[53]=31; r[54]=32; r[55]=33; 
	r[56]=34; r[57]=35; r[58]=36; r[59]=37; r[60]=38; r[61]=41; r[62]=99; r[63]=97; 
	r[64]=40; r[65]=45; r[66]=46; r[67]=100; r[68]=24; r[69]=47; r[70]=48; r[71]=105; 
	r[72]=52; r[73]=55; r[74]=53; r[75]=54; r[76]=107; r[77]=56; r[78]=57; r[79]=59; 
	r[80]=61; r[81]=60; r[82]=62; r[83]=64; r[84]=65; r[85]=67; r[86]=68; r[87]=70; 
	r[88]=71; r[89]=4; r[90]=82; r[91]=83; r[92]=15; r[93]=16; r[94]=87; r[95]=18; 
	r[96]=90; r[97]=17; r[98]=94; r[99]=49; r[100]=95; r[101]=96; r[102]=98; r[103]=39; 
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
	r[72]=0; r[73]=0; r[74]=0; r[75]=0; r[76]=0; r[77]=5; r[78]=13; r[79]=13; 
	r[80]=13; r[81]=15; r[82]=15; r[83]=15; r[84]=15; r[85]=15; r[86]=15; r[87]=15; 
	r[88]=15; r[89]=15; r[90]=15; r[91]=15; r[92]=15; r[93]=15; r[94]=15; r[95]=15; 
	r[96]=15; r[97]=9; r[98]=9; r[99]=9; r[100]=7; r[101]=15; r[102]=15; r[103]=15; 
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
