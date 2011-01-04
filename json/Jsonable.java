package json;

public interface Jsonable<E>
{

	/**
	 * output object infomation to json String;
	 * must make sure "obj.fromJson(obj.toJson())" hold same
	 * information with obj.
	 * @return
	 */
	String toJson();
}
