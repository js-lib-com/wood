package js.wood.eval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class MulOperatorTest {
	@Test
	public void opcodeInstance() throws Exception {
		assertThat(Opcode.MUL.instance().getClass(), equalTo(MulOperator.class));
	}

	@Test
	public void exec() {
		MulOperator mul = new MulOperator();
		assertThat(mul.exec("4px", "2"), equalTo("8px"));
		assertThat(mul.exec("4px", "0.5"), equalTo("2px"));
		assertThat(mul.exec("4px", "-2"), equalTo("-8px"));
		assertThat(mul.exec("4px", "-0.5"), equalTo("-2px"));
		assertThat(mul.exec("4", "2"), equalTo("8"));
		assertThat(mul.exec("4", "-2"), equalTo("-8"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_DifferentUnits() {
		MulOperator mul = new MulOperator();
		mul.exec("4px", "2rem");
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_NoArguments() {
		MulOperator mul = new MulOperator();
		mul.exec();
	}

	@Test(expected = IllegalArgumentException.class)
	public void exec_NullArgument() {
		MulOperator mul = new MulOperator();
		mul.exec((String[])null);
	}
}
