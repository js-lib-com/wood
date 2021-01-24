package js.wood.eval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

import js.lang.VarArgs;
import js.util.Classes;

public class AddOperatorTest {
	@Test
	public void opcodeInstance() throws Exception {
		assertThat(Opcode.ADD.instance().getClass(), equalTo(AddOperator.class));
	}

	@Test
	public void exec() {
		AddOperator add = new AddOperator();
		assertThat(add.exec("12.34", "56", "0.78"), equalTo("69.12"));
		assertThat(add.exec("12.34", "56", "-0.78"), equalTo("67.56"));
		assertThat(add.exec("12.34", "56", "0.78abc"), equalTo("69.12abc"));
		assertThat(add.exec("12.34", "56", "abc"), equalTo("12.3456abc"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_DifferentUnits() {
		AddOperator add = new AddOperator();
		add.exec("12.34px", "56rem");
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_NoArguments() {
		AddOperator add = new AddOperator();
		add.exec();
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_NullArgument() {
		AddOperator add = new AddOperator();
		add.exec((String[])null);
	}

	@Test
	public void argumentType() throws Exception {
		Class<?> typeClass = Classes.forName("js.wood.eval.AddOperator$Type");
		assertArgumentType("NUMBER", typeClass, "12.34");
		assertArgumentType("MEASURE", typeClass, "12.34abc");
		assertArgumentType("STRING", typeClass, "abc");
	}

	private static void assertArgumentType(String expectedType, Class<?> typeClass, String argument) throws Exception {
		assertThat(Classes.invoke(typeClass, "forArgument", argument).toString(), equalTo(expectedType));
	}

	@Test
	public void detectArgumentsType() throws Exception {
		assertArgumentsType("NUMBER", "12.34", "56", "0.78");
		assertArgumentsType("MEASURE", "12.34", "56", "0.78abc");
		assertArgumentsType("STRING", "abc", "12.34", "12.34abc");
	}

	private static void assertArgumentsType(String expectedType, String... arguments) throws Exception {
		assertThat(Classes.invoke(AddOperator.class, "detectArgumentsType", new VarArgs<String>(arguments)).toString(), equalTo(expectedType));
	}

	@Test
	public void addNumbers() throws Exception {
		assertThat(Classes.invoke(AddOperator.class, "addNumbers", new VarArgs<String>("12.34", "56", "0.78")), equalTo("69.12"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addNumbers_BadArguments() throws Exception {
		Classes.invoke(AddOperator.class, "addNumbers", new VarArgs<String>("12.34", "56", "0.78abc"));
	}

	@Test
	public void addMeasures() throws Exception {
		assertThat(Classes.invoke(AddOperator.class, "addMeasures", new VarArgs<String>("12.34", "56", "0.78abc")), equalTo("69.12abc"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addMeasures_BadArguments() throws Exception {
		Classes.invoke(AddOperator.class, "addMeasures", new VarArgs<String>("12.34", "56abc", "0.78def"));
	}

	@Test
	public void addStrings() throws Exception {
		assertThat(Classes.invoke(AddOperator.class, "addStrings", new VarArgs<String>("12.34", "56", "abc")), equalTo("12.3456abc"));
	}
}
