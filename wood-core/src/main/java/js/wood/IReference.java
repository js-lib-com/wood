package js.wood;

public interface IReference {
	String getName();

	boolean isVariable();

	boolean hasPath();

	String getPath();
}
