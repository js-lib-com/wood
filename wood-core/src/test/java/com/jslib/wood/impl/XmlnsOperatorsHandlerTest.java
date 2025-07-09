package com.jslib.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.dom.EList;
import com.jslib.wood.dom.Element;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.jslib.wood.WOOD;

public class XmlnsOperatorsHandlerTest {
	private DocumentBuilder builder;
	private IOperatorsHandler operators;

	@Before
	public void beforeTest() {
		builder = DocumentBuilder.getInstance();
		operators = new XmlnsOperatorsHandler();
	}

	@Test
	public void GivenTemplateDocument_WhenDocumentFindByEditable_ThenEditablesList() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		// WHEN
		EList elist = operators.findByOperator(doc, Operator.EDITABLE);

		// THEN
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void GivenTemplateDocument_WhenRootElementFindByEditable_ThenEditablesList() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element body = doc.getRoot();

		// WHEN
		EList elist = operators.findByOperator(body, Operator.EDITABLE);

		// THEN
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void GivenTemplateDocument_WhenNotRootElementFindByEditable_ThenEditable() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");

		// WHEN
		EList elist = operators.findByOperator(div, Operator.EDITABLE);

		// THEN
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(1));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void GivenTemplateDocument_WhenDocumentGetByEditable_ThenEditable() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		// WHEN
		Element section = operators.getByOperator(doc, Operator.EDITABLE, "section-2");

		// THEN
		assertThat(section, notNullValue());
		assertThat(section.getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void GivenTemplateDocument_WhenRootElementGetByEditable_ThenEditable() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element body = doc.getRoot();

		// WHEN
		Element section = operators.getByOperator(body, Operator.EDITABLE);

		//THEN
		assertThat(section, notNullValue());
		assertThat(section.getAttr("w:editable"), equalTo("section-1"));
	}

	@Test
	public void GivenTemplateDocument_WhenGetExistingOperand_ThenValue() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element section = doc.getByTag("section");

		// WHEN
		String operand = operators.getOperand(section, Operator.EDITABLE);

		// THEN
		assertThat(operand, equalTo("section"));
	}

	@Test
	public void GivenTemplateDocument_WhenGetMissingOperand_ThenNull() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element body = doc.getByTag("body");

		// WHEN
		String operand = operators.getOperand(body, Operator.EDITABLE);

		// THEN
		assertThat(operand, nullValue());
	}

	@Test
	public void GivenCompoDocument_WhenExistingGetOperand_ThenValue() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<div w:compo='res/dialog'></div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");

		// WHEN
		String operator = operators.getOperand(div, Operator.COMPO);

		// THEN
		assertThat(operator, equalTo("res/dialog"));
	}

	@Test
	public void GivenCompoDocument_WhenMissingGetOperand_ThenNull() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<div w:compo='res/dialog'></div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element body = doc.getByTag("body");

		// WHEN
		String operand = operators.getOperand(body, Operator.COMPO);

		// THEN
		assertThat(operand, nullValue());
	}

	@Test
	public void GivenCompoDocument_WhenRemoveOperator_ThenOperatorNotLongerExisting() throws SAXException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<div w:compo='res/dialog'></div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");
		assertTrue(div.hasAttrNS(WOOD.NS, "compo"));

		// WHEN
		operators.removeOperator(div, Operator.COMPO);

		// THEN
		assertFalse(div.hasAttrNS(WOOD.NS, "compo"));
	}
}
