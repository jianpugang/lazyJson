package json;

class JsonNumberLeaf implements LeafJsonValue
{
	private double num;

	public JsonNumberLeaf(double n)
	{
		num = n;
	}
	public double put(double newv)
	{
		double oldv = num;
		num = newv;
		return oldv;
	}

	public double getNumber()
	{
		return num;
	}
	public String getString()
	{
		return num+"";
	}
	public boolean getBool()
	{
		if(num==0){
			return false;
		}
		return true;
	}
	
	public boolean isLeaf()
	{
		return true;
	}
	
	public JsonValue $(String k)
	{
		// TODO Auto-generated method stub
		return null;
	}
	public JsonValue w(String k, JsonValue v)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public String toJson()
	{
		return num+"";
	}
	public String toString()
	{
		return num+"";
				
	}
	public boolean isDelay()
	{
	
		return false;
	}
}
