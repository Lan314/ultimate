type ref;
type realVar;
type classConst;
// type Field x;
// var $HeapVar : <x>[ref, Field x]x;

const unique $null : ref ;
const unique $intArrNull : [int]int ;
const unique $realArrNull : [int]realVar ;
const unique $refArrNull : [int]ref ;

const unique $arrSizeIdx : int;
var $intArrSize : [int]int;
var $realArrSize : [realVar]int;
var $refArrSize : [ref]int;

var $stringSize : [ref]int;

//built-in axioms 
axiom ($arrSizeIdx == -1);

//note: new version doesn't put helpers in the perlude anymore//Prelude finished 
const unique List : classConst ;



var int$Random$index0 : int;
var java.lang.Object$List$value254 : Field ref;
var java.lang.String$lp$$rp$$Random$args256 : [int]ref;
var List$List$next255 : Field ref;


// procedure is generated by joogie.
function {:inline true} $neref(x : ref, y : ref) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $realarrtoref($param00 : [int]realVar) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $modreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// <java.lang.String: int length()>
procedure int$java.lang.String$length$59(__this : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $leref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $modint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $gtref($param00 : ref, $param11 : ref) returns (__ret : int);



// <CountMetaList: void <init>()>
procedure void$CountMetaList$$la$init$ra$$2228(__this : ref)  requires ($neref((__this), ($null))==1);
 {
var r01 : ref;
Block16:
	r01 := __this;
	 assert ($neref((r01), ($null))==1);
	 //  @line: 1
	 call void$java.lang.Object$$la$init$ra$$28((r01));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $eqrealarray($param00 : [int]realVar, $param11 : [int]realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $addint(x : int, y : int) returns (__ret : int) {
(x + y)
}


// procedure is generated by joogie.
function {:inline true} $subref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $inttoreal($param00 : int) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shrint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negReal($param00 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $ushrint($param00 : int, $param11 : int) returns (__ret : int);



	 //  @line: 24
// <CountMetaList: List createMetaList()>
procedure List$CountMetaList$createMetaList$2231() returns (__ret : ref) {
var i331 : int;
var r330 : ref;
var i123 : int;
var $r127 : ref;
var i019 : int;
var i229 : int;
var $r026 : ref;
var r228 : ref;
	 //  @line: 25
Block28:
	 //  @line: 25
	 call i019 := int$Random$random$2234();
	 //  @line: 26
	r228 := $null;
	 //  @line: 27
	i229 := 0;
	 goto Block29;
	 //  @line: 27
Block29:
	 goto Block30, Block32;
	 //  @line: 27
Block30:
	 assume ($geint((i229), (i019))==1);
	 goto Block31;
	 //  @line: 27
Block32:
	 //  @line: 27
	 assume ($negInt(($geint((i229), (i019))))==1);
	 //  @line: 28
	 call i123 := int$Random$random$2234();
	 //  @line: 29
	r330 := $null;
	 //  @line: 30
	i331 := i123;
	 goto Block33;
	 //  @line: 36
Block31:
	 //  @line: 36
	__ret := r228;
	 return;
	 //  @line: 30
Block33:
	 goto Block36, Block34;
	 //  @line: 30
Block36:
	 //  @line: 30
	 assume ($negInt(($leint((i331), (0))))==1);
	 //  @line: 31
	$r127 := $newvariable((37));
	 assume ($neref(($newvariable((37))), ($null))==1);
	 assert ($neref(($r127), ($null))==1);
	 //  @line: 31
	 call void$List$$la$init$ra$$2232(($r127), ($null), (r330));
	 //  @line: 31
	r330 := $r127;
	 //  @line: 30
	i331 := $addint((i331), (-1));
	 goto Block33;
	 //  @line: 30
Block34:
	 assume ($leint((i331), (0))==1);
	 goto Block35;
	 //  @line: 33
Block35:
	 //  @line: 33
	$r026 := $newvariable((38));
	 assume ($neref(($newvariable((38))), ($null))==1);
	 goto Block39;
	 //  @line: 33
Block39:
	 assert ($neref(($r026), ($null))==1);
	 //  @line: 33
	 call void$List$$la$init$ra$$2232(($r026), (r330), (r228));
	 //  @line: 33
	r228 := $r026;
	 //  @line: 27
	i229 := $addint((i229), (1));
	 goto Block29;
}


// procedure is generated by joogie.
function {:inline true} $refarrtoref($param00 : [int]ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $divref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $mulref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $neint(x : int, y : int) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ltreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftorefarr($param00 : ref) returns (__ret : [int]ref);



// procedure is generated by joogie.
function {:inline true} $gtint(x : int, y : int) returns (__ret : int) {
if (x > y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $reftoint($param00 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $addref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $xorreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $andref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $cmpreal(x : realVar, y : realVar) returns (__ret : int) {
if ($ltreal((x), (y)) == 1) then 1 else if ($eqreal((x), (y)) == 1) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $addreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $gtreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



	 //  @line: 43
// <List: void <init>(java.lang.Object,List)>
procedure void$List$$la$init$ra$$2232(__this : ref, $param_0 : ref, $param_1 : ref)
  modifies $HeapVar;
  requires ($neref((__this), ($null))==1);
 {
var r032 : ref;
var r234 : ref;
var r133 : ref;
Block40:
	r032 := __this;
	r133 := $param_0;
	r234 := $param_1;
	 assert ($neref((r032), ($null))==1);
	 //  @line: 44
	 call void$java.lang.Object$$la$init$ra$$28((r032));
	 assert ($neref((r032), ($null))==1);
	 //  @line: 45
	$HeapVar[r032, java.lang.Object$List$value254] := r133;
	 assert ($neref((r032), ($null))==1);
	 //  @line: 46
	$HeapVar[r032, List$List$next255] := r234;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $eqreal(x : realVar, y : realVar) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ltint(x : int, y : int) returns (__ret : int) {
if (x < y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $newvariable($param00 : int) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $divint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $geint(x : int, y : int) returns (__ret : int) {
if (x >= y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $mulint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $leint(x : int, y : int) returns (__ret : int) {
if (x <= y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $shlref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqrefarray($param00 : [int]ref, $param11 : [int]ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftointarr($param00 : ref) returns (__ret : [int]int);



// procedure is generated by joogie.
function {:inline true} $ltref($param00 : ref, $param11 : ref) returns (__ret : int);



// <java.lang.Object: void <init>()>
procedure void$java.lang.Object$$la$init$ra$$28(__this : ref);



// procedure is generated by joogie.
function {:inline true} $mulreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shrref($param00 : ref, $param11 : ref) returns (__ret : int);



	 //  @line: 5
// <Random: int random()>
procedure int$Random$random$2234() returns (__ret : int)
  modifies int$Random$index0, $stringSize;
 {
var $i341 : int;
var $i036 : int;
var r038 : ref;
var $i240 : int;
var $i139 : int;
var $r137 : [int]ref;
	 //  @line: 6
Block42:
	 //  @line: 6
	$r137 := java.lang.String$lp$$rp$$Random$args256;
	 //  @line: 6
	$i036 := int$Random$index0;
	 assert ($geint(($i036), (0))==1);
	 assert ($ltint(($i036), ($refArrSize[$r137[$arrSizeIdx]]))==1);
	 //  @line: 6
	r038 := $r137[$i036];
	 //  @line: 7
	$i139 := int$Random$index0;
	 //  @line: 7
	$i240 := $addint(($i139), (1));
	 //  @line: 7
	int$Random$index0 := $i240;
	$i341 := $stringSize[r038];
	 //  @line: 8
	__ret := $i341;
	 return;
}


// <Random: void <init>()>
procedure void$Random$$la$init$ra$$2233(__this : ref)  requires ($neref((__this), ($null))==1);
 {
var r035 : ref;
Block41:
	r035 := __this;
	 assert ($neref((r035), ($null))==1);
	 //  @line: 1
	 call void$java.lang.Object$$la$init$ra$$28((r035));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $ushrreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $shrreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $divreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $orint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftorealarr($param00 : ref) returns (__ret : [int]realVar);



// procedure is generated by joogie.
function {:inline true} $cmpref(x : ref, y : ref) returns (__ret : int) {
if ($ltref((x), (y)) == 1) then 1 else if ($eqref((x), (y)) == 1) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $realtoint($param00 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $geref($param00 : ref, $param11 : ref) returns (__ret : int);



	 //  @line: 2
// <Random: void <clinit>()>
procedure void$Random$$la$clinit$ra$$2235()
  modifies int$Random$index0;
 {
	 //  @line: 3
Block43:
	 //  @line: 3
	int$Random$index0 := 0;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $orreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqint(x : int, y : int) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ushrref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $modref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $eqintarray($param00 : [int]int, $param11 : [int]int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negRef($param00 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $lereal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $nereal(x : realVar, y : realVar) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $instanceof($param00 : ref, $param11 : classConst) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $xorref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $orref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $intarrtoref($param00 : [int]int) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $subreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shlreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negInt(x : int) returns (__ret : int) {
if (x == 0) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $gereal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqref(x : ref, y : ref) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $cmpint(x : int, y : int) returns (__ret : int) {
if (x < y) then 1 else if (x == y) then 0 else -1
}


	 //  @line: 9
// <CountMetaList: int countMetaList(List)>
procedure int$CountMetaList$countMetaList$2230($param_0 : ref) returns (__ret : int)
  modifies $HeapVar;
 {
var $r211 : ref;
var r616 : ref;
var r012 : ref;
var i017 : int;
var $r414 : ref;
var $r19 : ref;
var $r515 : ref;
var $z010 : int;
var $r313 : ref;
Block18:
	r616 := $param_0;
	 //  @line: 10
	i017 := 0;
	 goto Block19;
	 //  @line: 11
Block19:
	 goto Block20, Block22;
	 //  @line: 11
Block20:
	 assume ($eqref((r616), ($null))==1);
	 goto Block21;
	 //  @line: 11
Block22:
	 //  @line: 11
	 assume ($negInt(($eqref((r616), ($null))))==1);
	 assert ($neref((r616), ($null))==1);
	 //  @line: 12
	$r19 := $HeapVar[r616, java.lang.Object$List$value254];
	 //  @line: 12
	$z010 := $instanceof(($r19), (List));
	 goto Block23;
	 //  @line: 21
Block21:
	 //  @line: 21
	__ret := i017;
	 return;
	 //  @line: 12
Block23:
	 goto Block26, Block24;
	 //  @line: 12
Block26:
	 //  @line: 12
	 assume ($negInt(($eqint(($z010), (0))))==1);
	 assert ($neref((r616), ($null))==1);
	 //  @line: 13
	$r211 := $HeapVar[r616, java.lang.Object$List$value254];
	 //  @line: 13
	r012 := $r211;
	 assert ($neref((r012), ($null))==1);
	 //  @line: 14
	$r313 := $HeapVar[r012, List$List$next255];
	 assert ($neref((r616), ($null))==1);
	 //  @line: 14
	$HeapVar[r616, java.lang.Object$List$value254] := $r313;
	 //  @line: 15
	$r414 := $newvariable((27));
	 assume ($neref(($newvariable((27))), ($null))==1);
	 assert ($neref((r012), ($null))==1);
	 //  @line: 15
	$r515 := $HeapVar[r012, java.lang.Object$List$value254];
	 assert ($neref(($r414), ($null))==1);
	 //  @line: 15
	 call void$List$$la$init$ra$$2232(($r414), ($r515), (r616));
	 //  @line: 15
	r616 := $r414;
	 goto Block25;
	 //  @line: 12
Block24:
	 assume ($eqint(($z010), (0))==1);
	 goto Block25;
	 //  @line: 17
Block25:
	 assert ($neref((r616), ($null))==1);
	 //  @line: 17
	r616 := $HeapVar[r616, List$List$next255];
	 //  @line: 18
	i017 := $addint((i017), (1));
	 goto Block19;
}


// procedure is generated by joogie.
function {:inline true} $andint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $andreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



	 //  @line: 2
// <CountMetaList: void main(java.lang.String[])>
procedure void$CountMetaList$main$2229($param_0 : [int]ref)
  modifies java.lang.String$lp$$rp$$Random$args256, $stringSize;
 {
var r14 : ref;
var r02 : [int]ref;

 //temp local variables 
var $freshlocal0 : int;

Block17:
	r02 := $param_0;
	 //  @line: 3
	java.lang.String$lp$$rp$$Random$args256 := r02;
	 //  @line: 4
	 call r14 := List$CountMetaList$createMetaList$2231();
	 //  @line: 6
	 call $freshlocal0 := int$CountMetaList$countMetaList$2230((r14));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $shlint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $xorint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $subint(x : int, y : int) returns (__ret : int) {
(x - y)
}


