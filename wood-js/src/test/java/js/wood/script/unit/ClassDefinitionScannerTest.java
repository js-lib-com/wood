package js.wood.script.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import js.wood.script.ClassDefinitionScanner;

public class ClassDefinitionScannerTest {
	@Test
	public void classDefinitionScannerLocalClasses() throws IOException {
		File scriptFile = new File("src/test/resources/ClassDefinition.js");
		Set<String> classes = ClassDefinitionScanner.getClasses(scriptFile);

		assertThat(classes, hasSize(5));
		assertThat(classes, hasItem("test.format.RichText"));
		assertThat(classes, hasItem("test.widget.Paging"));
		assertThat(classes, hasItem("test.widget.RichText.Description"));
		assertThat(classes, hasItem("test.wood.GeoMap"));
		assertThat(classes, hasItem("test.wood.Index"));
	}

	@Test
	public void classDefinitionScannerThirdPartyClasses() throws IOException {
		File scriptFile = new File("src/test/resources/google-maps-api.js");
		Set<String> classes = ClassDefinitionScanner.getClasses(scriptFile);

		assertThat(classes, hasSize(5));
		assertThat(classes, hasItem("google.maps.LatLng"));
		assertThat(classes, hasItem("google.maps.LatLngBounds"));
		assertThat(classes, hasItem("google.maps.Map"));
		assertThat(classes, hasItem("google.maps.MapTypeId"));
		assertThat(classes, hasItem("google.maps.Marker"));
	}
}
