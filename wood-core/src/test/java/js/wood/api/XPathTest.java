package js.wood.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.dom.NamespaceContext;
import js.util.Classes;
import js.wood.WOOD;

public class XPathTest {
	private DocumentBuilder builder;

	@Before
	public void beforeTest() {
		builder = Classes.loadService(DocumentBuilder.class);
	}

	@Test
	public void findByAttr_OnDocument() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		EList elist = doc.findByXPathNS(namespaceContext, "//*[@wood:editable]");
		
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByAttr_OnRootElement() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		EList elist = doc.getRoot().findByXPathNS(namespaceContext, "//*[@wood:editable]");
		
		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByAttr_OnNonRootElement() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");

		EList elist = div.findByXPathNS(namespaceContext, "//*[@wood:editable]");

		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByAttr_Descendant_OnDocument() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		EList elist = doc.findByXPathNS(namespaceContext, "descendant::node()[@wood:editable]");

		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByAttr_Descendant_OnElement() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");

		EList elist = div.findByXPathNS(namespaceContext, "descendant::node()[@wood:editable]");

		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(1));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByAttr_DescendantOrSelf_OnDocument() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);

		EList elist = doc.findByXPathNS(namespaceContext, "descendant-or-self::node()[@wood:editable]");

		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(2));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-1"));
		assertThat(elist.item(1).getAttr("w:editable"), equalTo("section-2"));
	}

	@Test
	public void findByAttr_DescendantOrSelf_OnElement() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<div>" + //
				"		<section w:editable='section-2'></section>" + //
				"	</div>" + //
				"</body>";
		Document doc = builder.parseXMLNS(xml);
		Element div = doc.getByTag("div");

		EList elist = div.findByXPathNS(namespaceContext, "descendant-or-self::node()[@wood:editable]");

		assertThat(elist, notNullValue());
		assertThat(elist.size(), equalTo(1));
		assertThat(elist.item(0).getAttr("w:editable"), equalTo("section-2"));
	}

	private static final NamespaceContext namespaceContext = new NamespaceContext() {
		@Override
		public String getNamespaceURI(String prefix) {
			return WOOD.NS;
		}
	};
}
