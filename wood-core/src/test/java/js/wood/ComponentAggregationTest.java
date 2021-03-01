package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.EList;
import js.dom.Element;
import js.dom.NamespaceContext;
import js.wood.impl.FileType;
import js.wood.impl.IOperatorsHandler;
import js.wood.impl.Operator;
import js.wood.impl.XmlnsOperatorsHandler;

@RunWith(MockitoJUnitRunner.class)
public class ComponentAggregationTest {
	@Mock
	private Project project;

	@Mock
	private CompoPath compoPath;
	@Mock
	private FilePath compoLayout; // layout file for component
	@Mock
	private FilePath compoDescriptor;
	@Mock
	private FilePath compoStyle; // style file for component

	@Mock
	private IReferenceHandler referenceHandler;

	private IOperatorsHandler operatorsHandler;
	private NamespaceContext namespace;

	@Before
	public void beforeTest() {
		operatorsHandler = new XmlnsOperatorsHandler();
		namespace = new NamespaceContext() {
			@Override
			public String getNamespaceURI(String prefix) {
				return WOOD.NS;
			}
		};

		when(project.getDisplay()).thenReturn("Components");
		when(project.hasNamespace()).thenReturn(true);
		when(project.getOperatorsHandler()).thenReturn(operatorsHandler);

		when(compoPath.getLayoutPath()).thenReturn(compoLayout);

		when(compoLayout.getProject()).thenReturn(project);
		when(compoLayout.exists()).thenReturn(true);
		when(compoLayout.isLayout()).thenReturn(true);
		when(compoLayout.cloneTo(FileType.XML)).thenReturn(compoDescriptor);
		when(compoLayout.cloneTo(FileType.STYLE)).thenReturn(compoStyle);
	}

	/** Component without template but with child component. */
	@Test
	public void aggregation() {
		FilePath childLayoutPath = Mockito.mock(FilePath.class);
		when(childLayoutPath.exists()).thenReturn(true);
		when(childLayoutPath.isLayout()).thenReturn(true);
		when(childLayoutPath.cloneTo(FileType.XML)).thenReturn(Mockito.mock(FilePath.class));
		when(childLayoutPath.cloneTo(FileType.STYLE)).thenReturn(Mockito.mock(FilePath.class));

		String childHTML = "" + //
				"<div>" + //
				"	<h1>Child</h1>" + //
				"</div>";
		when(childLayoutPath.getReader()).thenReturn(new StringReader(childHTML));

		CompoPath childCompoPath = Mockito.mock(CompoPath.class);
		when(childCompoPath.getLayoutPath()).thenReturn(childLayoutPath);
		when(project.createCompoPath("res/child")).thenReturn(childCompoPath);

		String compoHTML = "" + //
				"<section xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Component</h1>" + //
				"	<div w:compo='res/child'></div>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		layout.getDocument().dump();

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Child"));
	}

	/** {@link Operator#COMPO} and {@link Operator#PARAM} should be erased from layout. */
	@Test
	public void operatorsErasure() {
		FilePath childLayoutPath = Mockito.mock(FilePath.class);
		when(childLayoutPath.exists()).thenReturn(true);
		when(childLayoutPath.isLayout()).thenReturn(true);
		when(childLayoutPath.cloneTo(FileType.XML)).thenReturn(Mockito.mock(FilePath.class));
		when(childLayoutPath.cloneTo(FileType.STYLE)).thenReturn(Mockito.mock(FilePath.class));

		String childHTML = "" + //
				"<div>" + //
				"	<h1>@param/title</h1>" + //
				"</div>";
		when(childLayoutPath.getReader()).thenReturn(new StringReader(childHTML));

		CompoPath childCompoPath = Mockito.mock(CompoPath.class);
		when(childCompoPath.getLayoutPath()).thenReturn(childLayoutPath);
		when(project.createCompoPath("res/child")).thenReturn(childCompoPath);

		String compoHTML = "" + //
				"<section xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Component</h1>" + //
				"	<div w:compo='res/child' w:param='title:Child'></div>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		layout.getDocument().dump();
		assertThat(layout.getByXPathNS(namespace, "//*[@w:compo]"), nullValue());
		assertThat(layout.getByXPathNS(namespace, "//*[@w:param]"), nullValue());
	}
}
