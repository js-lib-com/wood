package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.lang.VarArgs;
import js.util.Classes;
import js.wood.WoodException;
import js.wood.eval.Interpreter;

import org.junit.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EvaluatorTest extends WoodTestCase {
	@Test
	public void evaluate() {
		Interpreter interpreter = new Interpreter();
		assertEquals("3.0", interpreter.evaluate("(add 1 2)", null));
		assertEquals("2abc", interpreter.evaluate("(add 2 abc)", null));
		assertEquals("3.0em", interpreter.evaluate("(add 1em 2)", null));
		assertEquals("40px", interpreter.evaluate("(sub 48px 8px)", null));
		assertEquals("40px", interpreter.evaluate("(sub 48px 2 6)", null));
		assertEquals("8px", interpreter.evaluate("(mul 2 4px)", null));
		assertEquals("6.0", interpreter.evaluate("(add 1 (add 2 3))", null));
		assertEquals("40px", interpreter.evaluate("(sub 48px (mul 2 4px))", null));
	}

	@Test
	public void opcodeInstances() {
		Class opcodeClass = Classes.forName("js.wood.eval.Opcode");
		// TODO: update next assertions list when change operator codes
		assertOpcodeInstance(opcodeClass, "ADD", "js.wood.eval.AddOperator");
	}

	private static void assertOpcodeInstance(Class opcodeClass, String opcodeName, String operatorClass) {
		assertEquals(Classes.forName(operatorClass), invoke(Enum.valueOf(opcodeClass, opcodeName), "instance").getClass());
	}

	@Test
	public void measure() {
		assertMeasure("12.34px", 12.34, "px");
		assertMeasure("12.34 px", 12.34, "px");
		assertMeasure("12.34 px ", 12.34, "px");
		assertMeasure("12.34 px\r\n", 12.34, "px");
	}

	private static void assertMeasure(String measureValue, double value, String units) {
		Object measure = Classes.newInstance("js.wood.eval.Measure", measureValue);
		assertEquals(value, (double)field(measure, "value"), 0);
		assertEquals(units, field(measure, "units"));
	}

	@Test
	public void argumentsBuilder() {
		for (String argumentsValue : new String[] { "first second third", "first   second   third", "  first   second   third  ", "\tfirst   second   third\r\n" }) {
			Object builder = Classes.newInstance("js.wood.eval.ArgumentsBuilder");
			for (int i = 0; i < argumentsValue.length(); ++i) {
				invoke(builder, "addChar", argumentsValue.charAt(i));
			}
			invoke(builder, "flush");

			Object[] arguments = invoke(builder, "getValue");
			assertEquals(3, arguments.length);
			assertEquals("first", arguments[0]);
			assertEquals("second", arguments[1]);
			assertEquals("third", arguments[2]);
		}
	}

	@Test
	public void parser() {
		Object parser = Classes.newInstance("js.wood.eval.Parser", "(add 1 (add 2 3))");
		Object expression = invoke(parser, "parse");

		assertNotNull(expression);
		assertEquals("ADD", invoke(expression, "getOpcode").toString());

		Object[] arguments = invoke(expression, "getArguments");
		assertNotNull(arguments);
		assertEquals(2, arguments.length);
		assertEquals("1", arguments[0]);
		assertEquals("js.wood.eval.Expression", arguments[1].getClass().getName());

		expression = arguments[1];
		assertNotNull(expression);
		assertEquals("ADD", invoke(expression, "getOpcode").toString());

		arguments = invoke(expression, "getArguments");
		assertNotNull(arguments);
		assertEquals(2, arguments.length);
		assertEquals("2", arguments[0]);
		assertEquals("3", arguments[1]);
	}

	@Test
	public void addOperatorExec() {
		Object addOperator = Classes.newInstance("js.wood.eval.AddOperator");
		assertAddOperator("69.12", addOperator, "12.34", "56", "0.78");
		assertAddOperator("69.12abc", addOperator, "12.34", "56", "0.78abc");
		assertAddOperator("12.3456abc", addOperator, "12.34", "56", "abc");
	}

	private static void assertAddOperator(String expected, Object addOperator, String... arguments) {
		assertEquals(expected, invoke(addOperator, "exec", new VarArgs<String>(arguments)));
	}

	@Test
	public void addOperatorArgumentType() throws Exception {
		Class typeClass = Classes.forName("js.wood.eval.AddOperator$Type");
		assertArgumentType("NUMBER", typeClass, "12.34");
		assertArgumentType("MEASURE", typeClass, "12.34abc");
		assertArgumentType("STRING", typeClass, "abc");
	}

	private static void assertArgumentType(String expectedType, Class typeClass, String argument) throws Exception {
		assertEquals(expectedType, Classes.invoke(typeClass, "forArgument", argument).toString());
	}

	@Test
	public void addOperatorDetectArgumentsType() throws Exception {
		Class operatorClass = Classes.forName("js.wood.eval.AddOperator");
		assertArgumentsType("NUMBER", operatorClass, "12.34", "56", "0.78");
		assertArgumentsType("MEASURE", operatorClass, "12.34", "56", "0.78abc");
		assertArgumentsType("STRING", operatorClass, "abc", "12.34", "12.34abc");
	}

	private static void assertArgumentsType(String expectedType, Class operatorClass, String... arguments) throws Exception {
		assertEquals(expectedType, Classes.invoke(operatorClass, "detectArgumentsType", new VarArgs<String>(arguments)).toString());
	}

	@Test
	public void addOperatorAddNumbers() throws Exception {
		Class operatorClass = Classes.forName("js.wood.eval.AddOperator");
		assertEquals("69.12", Classes.invoke(operatorClass, "addNumbers", new VarArgs<String>("12.34", "56", "0.78")));
	}

	@Test
	public void addOperatorIllegalAddNumbers() {
		try {
			Class operatorClass = Classes.forName("js.wood.eval.AddOperator");
			Classes.invoke(operatorClass, "addNumbers", new VarArgs<String>("12.34", "56", "0.78abc"));
			fail("Not numeric argument should rise exception");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
			assertTrue(e.getMessage().contains("Bad numeric argument"));
		}
	}

	@Test
	public void addOperatorAddMeasures() throws Exception {
		Class operatorClass = Classes.forName("js.wood.eval.AddOperator");
		assertEquals("69.12abc", Classes.invoke(operatorClass, "addMeasures", new VarArgs<String>("12.34", "56", "0.78abc")));
	}

	@Test
	public void addOperatorIllegalAddMeasures() {
		try {
			Class operatorClass = Classes.forName("js.wood.eval.AddOperator");
			Classes.invoke(operatorClass, "addMeasures", new VarArgs<String>("12.34", "56abc", "0.78def"));
			fail("Different measure units should rise exception");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
			assertTrue(e.getMessage().contains("Different measure units"));
		}
	}

	@Test
	public void notRecognizedOpcode() {
		try {
			Interpreter interpreter = new Interpreter();
			interpreter.evaluate("(xyz 1 2 3)", null);
			fail("Not recognized opcode should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Not implemented opcode"));
		}
	}

	@Test
	public void addOperatorAddStrings() throws Exception {
		Class operatorClass = Classes.forName("js.wood.eval.AddOperator");
		assertEquals("12.3456abc", Classes.invoke(operatorClass, "addStrings", new VarArgs<String>("12.34", "56", "abc")));
	}

	@Test
	public void tooLongOpcode() {
		try {
			Interpreter interpreter = new Interpreter();
			interpreter.evaluate("(a-long-opcode-value 1 2 3)", null);
			fail("Too long opcode should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Opcode too long"));
		}
	}

	@Test
	public void valuePattern() {
		Class<?> valueClass = Classes.forName("js.wood.eval.Value");
		Pattern pattern = Classes.getFieldValue(valueClass, "PATTERN");

		for (String argument : new String[] { "48px", "-48px", "+48px", "48", "-48", "+48", "48.5", "-48.5", "+48.5", "48.5px", "-48.5px", "+48.5px" }) {
			assertTrue(pattern.matcher(argument).find());
		}

		assertValuePattern(pattern, "48px", 48, "px");
		assertValuePattern(pattern, "-48px", -48, "px");
		assertValuePattern(pattern, "+48px", 48, "px");
		assertValuePattern(pattern, "48", 48, "");
		assertValuePattern(pattern, "-48", -48, "");
		assertValuePattern(pattern, "+48", 48, "");
		assertValuePattern(pattern, "48.5px", 48.5, "px");
		assertValuePattern(pattern, "-48.5px", -48.5, "px");
		assertValuePattern(pattern, "+48.5px", 48.5, "px");
	}

	private static void assertValuePattern(Pattern pattern, String argument, double quantity, String units) {
		Matcher matcher = pattern.matcher(argument);
		assertTrue(matcher.find());
		assertEquals(quantity, Double.parseDouble(matcher.group(1)), 0);
		assertEquals(units, matcher.group(2));
	}

	@Test
	public void subOperatorExec() {
		Object subOperator = Classes.newInstance("js.wood.eval.SubOperator");
		assertSubOperator("40px", subOperator, "48px", "2px", "6px");
		assertSubOperator("40.5px", subOperator, "48.5px", "2px", "6px");
	}

	private static void assertSubOperator(String expected, Object subOperator, String... arguments) {
		assertEquals(expected, invoke(subOperator, "exec", new VarArgs<String>(arguments)));
	}
}
