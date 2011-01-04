package json;

class JsonBoolLeaf implements LeafJsonValue
{
	private boolean bool;
	public JsonBoolLeaf(boolean b)
	{
		bool = b;
	}
	public double getNumber()
	{
		if(bool){
			return 1;
		};
		return 0;
	}

	public String getString()
	{
		if(bool){
			return "true";
		}
		return "false";
	}

	public boolean getBool()
	{
		return bool;
	}

	public JsonValue $(String k)
	{
		return this;
	}
	public JsonValue w(String k, JsonValue value)
	{
		return null;
	}
	public boolean isLeaf()
	{
		return true;
	}
	public JsonValue value()
	{
		return this;
	}
	
	public String toJson()
	{
		if(bool){
			return "true";
		}
		return "false";
	}
	public String toString()
	{
		return toJson();
				
	}
	public boolean isDelay()
	{
		return false;
	}

}
