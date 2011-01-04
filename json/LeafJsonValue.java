package json;

public interface LeafJsonValue extends JsonValue
{
	double getNumber();
	String getString();
	boolean getBool();
	boolean isDelay();
}
