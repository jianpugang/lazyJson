package json;

import java.util.Map.Entry;

class JsonDelayLeaf implements LeafJsonValue
{
	Json parent;
	String jsonStr;
	public JsonDelayLeaf(String v)
	{
		jsonStr = v;
	}
	public boolean isLeaf()
	{
		return true;
	}
	/**
	 * read or write inner struct will cause replace current "delay josn leaf"
	 * parsed new jsonLeaf refer to the 'key'
	 */
	public JsonValue $(String k)
	{
		return replaceThis().$(k);
	}
	/**
	 * 
	 */
	public JsonValue w(String k, JsonValue v)
	{
		return replaceThis().w(k,v);
	}
	private JsonValue replaceThis()
	{
		Json j = new Json(jsonStr);
		for(Entry<String,JsonValue> e : parent.data.entrySet()){
			
			if (e.getValue() == this){
				e.setValue(j);
				return j;
			}
		}
		return null;
	}
	public double getNumber()
	{
		return Double.parseDouble(jsonStr);
	}
	public String getString()
	{
		return jsonStr;
	}
	public boolean getBool()
	{
		return "true".equals(jsonStr);
	}

	public String toJson()
	{
		return jsonStr;
	}
	public String toString()
	{
		return jsonStr;
	}
	public boolean isDelay()
	{
		return true;
	}
	
}
