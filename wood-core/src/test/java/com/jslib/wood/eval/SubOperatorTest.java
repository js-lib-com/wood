package com.jslib.wood.eval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class SubOperatorTest {
	@Test
	public void opcodeInstance() throws Exception {
		assertThat(Opcode.SUB.instance().getClass(), equalTo(SubOperator.class));
	}

	@Test
	public void exec() {
		SubOperator sub = new SubOperator();
		assertThat(sub.exec("48px", "2px", "6px"), equalTo("40px"));
		assertThat(sub.exec("48.5px", "2px", "6px"), equalTo("40.5px"));
		assertThat(sub.exec("48px", "-2px"), equalTo("50px"));
		assertThat(sub.exec("48.5px", "-2px"), equalTo("50.5px"));
		assertThat(sub.exec("48px", "2px", "6"), equalTo("40px"));
		assertThat(sub.exec("48.5px", "2px", "6"), equalTo("40.5px"));
		assertThat(sub.exec("48", "2", "6"), equalTo("40"));
		assertThat(sub.exec("48.5", "2", "6"), equalTo("40.5"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_DifferentUnits() {
		SubOperator sub = new SubOperator();
		sub.exec("48px", "2px", "6rem");
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_NoArguments() {
		SubOperator sub = new SubOperator();
		sub.exec();
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_NullArgument() {
		SubOperator sub = new SubOperator();
		sub.exec((String[])null);
	}
}
