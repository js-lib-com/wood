package com.jslib.wood;

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

import com.jslib.api.dom.Element;
import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.IOperatorsHandler;
import com.jslib.wood.impl.XmlnsOperatorsHandler;

@RunWith(MockitoJUnitRunner.class)
public class ComponentTest {
	@Mock
	private Project project;
	@Mock
	private FilePath compoDir;
	@Mock
	private CompoPath compoPath;
	@Mock
	private FilePath layoutPath;
	@Mock
	private FilePath stylePath;
	@Mock
	private FilePath scriptPath;
	@Mock
	private FilePath descriptorFile;
	@Mock
	private IReferenceHandler referenceHandler;

	private IOperatorsHandler operatorsHandler;

	@Before
	public void beforeTest() {
		operatorsHandler = new XmlnsOperatorsHandler();

		when(project.getTitle()).thenReturn("Components");
		when(project.hasNamespace()).thenReturn(true);
		when(project.getOperatorsHandler()).thenReturn(operatorsHandler);

		when(compoPath.getLayoutPath()).thenReturn(layoutPath);

		when(layoutPath.getName()).thenReturn("layout.htm");
		when(layoutPath.getBasename()).thenReturn("layout");
		when(layoutPath.getProject()).thenReturn(project);
		when(layoutPath.getParentDir()).thenReturn(compoDir);
		when(layoutPath.exists()).thenReturn(true);
		when(layoutPath.isLayout()).thenReturn(true);
		when(layoutPath.cloneTo(FileType.XML)).thenReturn(descriptorFile);
		when(layoutPath.cloneTo(FileType.STYLE)).thenReturn(stylePath);

		when(descriptorFile.cloneTo(FileType.SCRIPT)).thenReturn(scriptPath);

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
		assertThat(compo.getTitle(), equalTo("Components"));
		assertThat(compo.getDescription(), nullValue());
		assertThat(compo.getLayoutFileName(), equalTo("layout.htm"));
		assertThat(compo.toString(), equalTo("Components"));

		assertThat(compo.getScriptDescriptor(CT.PREVIEW_SCRIPT), nullValue());
		assertThat(compo.getScriptDescriptors(), empty());
		assertThat(compo.getLinkDescriptors(), empty());
		assertThat(compo.getMetaDescriptors(), empty());
		assertThat(compo.getResourcesGroup(), nullValue());
		assertTrue(compo.getStyleFiles().isEmpty());
	}

	@Test
	public void descriptor() {
		String descriptor = "<compo>" + //
				"<title>Page Compo</title>" + //
				"<description>Page description.</description>" + //
				"<group>admin</group>" + //
				"<scripts>" + //
				"	<script src='lib/js-lib.js'></script>" + //
				"	<script src='script/js/wood/Compo.js'></script>" + //
				"</scripts>" + //
				"</compo>";
		when(descriptorFile.exists()).thenReturn(true);
		when(descriptorFile.getReader()).thenReturn(new StringReader(descriptor));

		String layout = "<h1>Compo</h1>";
		when(layoutPath.getReader()).thenReturn(new StringReader(layout));

		Component compo = new Component(compoPath, referenceHandler);

		assertThat(compo.getProject(), equalTo(project));
		assertThat(compo.getBaseLayoutPath(), equalTo(layoutPath));
		assertThat(compo.getName(), equalTo("layout"));
		assertThat(compo.getTitle(), equalTo("Components - Page Compo"));
		assertThat(compo.getDescription(), equalTo("Page description."));
		assertThat(compo.getLayoutFileName(), equalTo("layout.htm"));
		assertThat(compo.toString(), equalTo("Components - Page Compo"));

		assertThat(compo.getScriptDescriptor(CT.PREVIEW_SCRIPT), nullValue());
		assertThat(compo.getLinkDescriptors(), empty());
		assertThat(compo.getMetaDescriptors(), empty());
		assertThat(compo.getResourcesGroup(), equalTo("admin"));
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
	 * Create component from CompoPath. If {@link CompoPath#getLayoutPathEx()} fails to find layout file, component constructor
	 * should throw {@link WoodException}.
	 */
	@Test(expected = WoodException.class)
	public void missingLayoutFile() {
		when(compoPath.getLayoutPath()).thenThrow(WoodException.class);
		new Component(compoPath, referenceHandler);
	}
}
