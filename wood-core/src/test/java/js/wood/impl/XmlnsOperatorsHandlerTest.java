package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.util.Classes;
import js.wood.WOOD;

public class XmlnsOperatorsHandlerTest {
	private DocumentBuilder builder;
	private IOperatorsHandler operators;

	@Before
	public void beforeTest() {
		builder = Classes.loadService(DocumentBuilder.class);
		operators = new XmlnsOperatorsHandler();
	}

	@Test
	public void findByOperator_Document() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		EList elist = operators.findByOperator(doc, Operator.EDITABLE);
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByOperator_Element() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element body = doc.getRoot();

		EList elist = operators.findByOperator(body, Operator.EDITABLE);
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void getByOperator_Document() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		Element section = operators.getByOperator(doc, Operator.EDITABLE, "section-2");
		assertThat(section, notNullValue());
		assertThat(section.getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void getByOperator_Element() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element body = doc.getRoot();

		Element section = operators.getByOperator(body, Operator.EDITABLE);
		assertThat(section, notNullValue());
		assertThat(section.getAttr("w:editable"), equalTo("section-1"));
	}

	@Test
	public void getOperand_Editable() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		Element body = doc.getByTag("body");
		assertThat(operators.getOperand(body, Operator.EDITABLE), nullValue());

		Element section = doc.getByTag("section");
		assertThat(operators.getOperand(section, Operator.EDITABLE), equalTo("section"));
	}

	@Test
	public void getOperand_Compo() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<div w:compo='res/dialog'></div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		Element body = doc.getByTag("body");
		assertThat(operators.getOperand(body, Operator.COMPO), nullValue());

		Element div = doc.getByTag("div");
		assertThat(operators.getOperand(div, Operator.COMPO), equalTo("res/dialog"));
	}

	@Test
	public void removeOperator() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<div w:compo='res/dialog'></div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");

		assertTrue(div.hasAttrNS(WOOD.NS, "compo"));
		operators.removeOperator(div, Operator.COMPO);
		assertFalse(div.hasAttrNS(WOOD.NS, "compo"));
	}
}
