package com.jslib.wood.eval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.jslib.util.Classes;
import com.jslib.wood.WoodException;

public class EvaluatorTest {
	@Test
	public void evaluate() {
		Interpreter interpreter = new Interpreter();
		assertThat(interpreter.evaluate("(add 1 2)", null), equalTo("3.0"));
		assertThat(interpreter.evaluate("(add 2 abc)", null), equalTo("2abc"));
		assertThat(interpreter.evaluate("(add 1em 2)", null), equalTo("3.0em"));
		assertThat(interpreter.evaluate("(sub 48px 8px)", null), equalTo("40px"));
		assertThat(interpreter.evaluate("(sub 48px 2 6)", null), equalTo("40px"));
		assertThat(interpreter.evaluate("(mul 2 4px)", null), equalTo("8px"));
		assertThat(interpreter.evaluate("(add 1 (add 2 3))", null), equalTo("6.0"));
		assertThat(interpreter.evaluate("(sub 48px (mul 2 4px))", null), equalTo("40px"));
	}

	@Test
	public void measure() {
		assertMeasure("12.34px", 12.34, "px");
		assertMeasure("12.34 px", 12.34, "px");
		assertMeasure("12.34 px ", 12.34, "px");
		assertMeasure("12.34 px\r\n", 12.34, "px");
	}

	private static void assertMeasure(String measureValue, double value, String units) {
		Measure measure = new Measure(measureValue);
		assertThat(measure.getValue(), equalTo(value));
		assertThat(measure.getUnits(), equalTo(units));
	}

	@Test
	public void argumentsBuilder() {
		for (String argumentsValue : new String[] { "first second third", "first   second   third", "  first   second   third  ", "\tfirst   second   third\r\n" }) {
			ArgumentsBuilder builder = new ArgumentsBuilder();
			for (int i = 0; i < argumentsValue.length(); ++i) {
				builder.addChar(argumentsValue.charAt(i));
			}
			builder.flush();

			Object[] arguments = builder.getValue();
			assertThat(arguments.length, equalTo(3));
			assertThat(arguments[0], equalTo("first"));
			assertThat(arguments[1], equalTo("second"));
			assertThat(arguments[2], equalTo("third"));
		}
	}

	@Test
	public void parser() throws EvalException {
		Parser parser = new Parser("(add 1 (add 2 3))");
		Expression expression = parser.parse();

		assertThat(expression, notNullValue());
		assertThat(expression.getOpcode(), equalTo(Opcode.ADD));

		Object[] arguments = expression.getArguments();
		assertThat(arguments, notNullValue());
		assertThat(arguments.length, equalTo(2));
		assertThat(arguments[0], equalTo("1"));
		assertThat(arguments[1].getClass(), equalTo(Expression.class));

		expression = (Expression) arguments[1];
		assertThat(expression, notNullValue());
		assertThat(expression.getOpcode(), equalTo(Opcode.ADD));

		arguments = expression.getArguments();
		assertThat(arguments, notNullValue());
		assertThat(arguments.length, equalTo(2));
		assertThat(arguments[0], equalTo("2"));
		assertThat(arguments[1], equalTo("3"));
	}

	@Test(expected = WoodException.class)
	public void notRecognizedOpcode() {
		Interpreter interpreter = new Interpreter();
		interpreter.evaluate("(xyz 1 2 3)", null);
	}

	@Test(expected = WoodException.class)
	public void tooLongOpcode() {
		Interpreter interpreter = new Interpreter();
		interpreter.evaluate("(a-long-opcode-value 1 2 3)", null);
	}

	@Test
	public void valuePattern() {
		Pattern pattern = Classes.getFieldValue(Value.class, "PATTERN");

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
		assertThat(Double.parseDouble(matcher.group(1)), equalTo(quantity));
		assertThat(matcher.group(2), equalTo(units));
	}
}
