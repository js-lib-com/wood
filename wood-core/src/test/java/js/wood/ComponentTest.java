package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.Element;
import js.wood.impl.FileType;
import js.wood.impl.IOperatorsHandler;
import js.wood.impl.XmlnsOperatorsHandler;

@RunWith(MockitoJUnitRunner.class)
public class ComponentTest {
	@Mock
	private Project project;
	@Mock
	private DirPath compoDir;
	@Mock
	private CompoPath compoPath;
	@Mock
	private FilePath layoutPath;
	@Mock
	private FilePath stylePath;
	@Mock
	private FilePath descriptorPath;
	@Mock
	private IReferenceHandler referenceHandler;

	private IOperatorsHandler operatorsHandler;

	@Before
	public void beforeTest() {
		operatorsHandler = new XmlnsOperatorsHandler();

		when(project.getDisplay()).thenReturn("Components");
		when(project.hasNamespace()).thenReturn(true);
		when(project.getOperatorsHandler()).thenReturn(operatorsHandler);

		when(compoPath.getLayoutPath()).thenReturn(layoutPath);

		when(layoutPath.getName()).thenReturn("layout.htm");
		when(layoutPath.getBaseName()).thenReturn("layout");
		when(layoutPath.getProject()).thenReturn(project);
		when(layoutPath.getParentDirPath()).thenReturn(compoDir);
		when(layoutPath.exists()).thenReturn(true);
		when(layoutPath.isLayout()).thenReturn(true);
		when(layoutPath.cloneTo(FileType.XML)).thenReturn(descriptorPath);
		when(layoutPath.cloneTo(FileType.STYLE)).thenReturn(stylePath);

		when(compoDir.getFilePath(any())).thenReturn(Mockito.mock(FilePath.class));
	}

	@Test
	public void constructor() {
		String htm = "<h1>Compo</h1>";
		when(layoutPath.getReader()).thenReturn(new StringReader(htm));

		Component compo = new Component(compoPath, referenceHandler);

		assertThat(compo.getProject(), equalTo(project));
		assertThat(compo.getBaseLayoutPath(), equalTo(layoutPath));
		assertThat(compo.getName(), equalTo("layout"));
		assertThat(compo.getDisplay(), equalTo("Components / Layout"));
		assertThat(compo.getDescription(), equalTo("Components / Layout"));
		assertThat(compo.getLayoutFileName(), equalTo("layout.htm"));
		assertThat(compo.toString(), equalTo("Components / Layout"));
		
		assertThat(compo.getScriptDescriptor(CT.PREVIEW_SCRIPT), nullValue());
		assertThat(compo.getScriptDescriptors(), empty());
		assertThat(compo.getLinkDescriptors(), empty());
		assertThat(compo.getMetaDescriptors(), empty());
		assertThat(compo.getSecurityRole(), nullValue());
		assertTrue(compo.getStyleFiles().isEmpty());
	}

	@Test
	public void descriptor() {
		String descriptor = "<compo>" + //
				"<display>Page Compo</display>" + //
				"<description>Page description.</description>" + //
				"<security-role>admin</security-role>"+//
				"<scripts>" + //
				"	<script src='lib/js-lib.js'></script>" + //
				"	<script src='script/js/wood/Compo.js'></script>" + //
				"</scripts>" + //
				"</compo>";
		when(descriptorPath.exists()).thenReturn(true);
		when(descriptorPath.getReader()).thenReturn(new StringReader(descriptor));

		String layout = "<h1>Compo</h1>";
		when(layoutPath.getReader()).thenReturn(new StringReader(layout));

		Component compo = new Component(compoPath, referenceHandler);

		assertThat(compo.getProject(), equalTo(project));
		assertThat(compo.getBaseLayoutPath(), equalTo(layoutPath));
		assertThat(compo.getName(), equalTo("layout"));
		assertThat(compo.getDisplay(), equalTo("Page Compo"));
		assertThat(compo.getDescription(), equalTo("Page description."));
		assertThat(compo.getLayoutFileName(), equalTo("layout.htm"));
		assertThat(compo.toString(), equalTo("Page Compo"));
		
		assertThat(compo.getScriptDescriptor(CT.PREVIEW_SCRIPT), nullValue());
		assertThat(compo.getLinkDescriptors(), empty());
		assertThat(compo.getMetaDescriptors(), empty());
		assertThat(compo.getSecurityRole(), equalTo("admin"));
		assertTrue(compo.getStyleFiles().isEmpty());
		
		assertThat(compo.getScriptDescriptors(), hasSize(2));
		assertThat(compo.getScriptDescriptors().get(0).getSource(), equalTo("lib/js-lib.js"));
		assertThat(compo.getScriptDescriptors().get(1).getSource(), equalTo("script/js/wood/Compo.js"));
	}

	@Test
	public void standalone() {
		String htm = "" + //
				"<body>" + //
				"	<h1>Simple Layout</h1>" + //
				"</body>";
		when(layoutPath.getReader()).thenReturn(new StringReader(htm));

		Component compo = new Component(compoPath, referenceHandler);

		Element layout = compo.getLayout();
		assertThat(layout, notNullValue());
		assertThat(layout.getTag(), equalTo("body"));
		assertThat(layout.getByTag("h1").getText(), equalTo("Simple Layout"));
	}

	/** {@link Component#clean()} should remove editable elements and namespace declarations. */
	@Test
	public void clean() {
		String htm = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='editable'></section>" + //
				"</body>";
		when(layoutPath.getReader()).thenReturn(new StringReader(htm));

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		assertTrue(layout.hasAttr("xmlns:w"));
		assertThat(layout.getByTag("section"), notNullValue());
		compo.clean();
		assertFalse(layout.hasAttr("xmlns:w"));
		assertThat(layout.getByTag("section"), nullValue());
	}
	
	/**
	 * Create component from CompoPath. If {@link CompoPath#getLayoutPath()} fails to find layout file, component constructor
	 * should throw {@link WoodException}.
	 */
	@Test(expected = WoodException.class)
	public void missingLayoutFile() {
		when(compoPath.getLayoutPath()).thenThrow(WoodException.class);
		new Component(compoPath, referenceHandler);
	}
}
