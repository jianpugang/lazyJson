package json;


public interface JsonValue extends Jsonable<JsonValue>
{
	
	boolean isLeaf();
	JsonValue $(String k);
	JsonValue w(String k,JsonValue v);
}
