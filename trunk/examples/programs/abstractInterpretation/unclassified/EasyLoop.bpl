//#Safe
procedure EasyLoop() 
{
	var x, y: int;
	
	x := 0;
	y := 0;
	
	while(x < 5)
	{
		x := x + 1;
		y := y + 1;
	}
	
	assert ( x == y );
}