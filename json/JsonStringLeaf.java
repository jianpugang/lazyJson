package json;

class JsonStringLeaf implements LeafJsonValue
{
	private String string;
	// not include quote
	private String stringValue;
	public JsonStringLeaf(String str)
	{
		string = str;
		stringValue = str.substring(1, str.length()-1);
	}
	public double getNumber()
	{
		return Double.parseDouble(string);
	}

	public String getString()
	{
		
		return string;
	}

	public boolean getBool()
	{
		if("true".equals(string)){
			return true;
		}
		return false;
	}

	public boolean isLeaf()
	{
		return true;
	}
	public JsonValue $(String k)
	{
		return null;
	}
	public JsonValue w(String k, JsonValue v)
	{
		return null;
	}

	public String toJson()
	{
			return string;
	}
	public String toString()
	{
		return stringValue;
	}
	public boolean isDelay()
	{
		return false;
	}

}
