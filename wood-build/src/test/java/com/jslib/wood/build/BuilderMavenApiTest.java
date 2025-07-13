package com.jslib.wood.build;

import com.jslib.wood.FilePath;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for methods used by WOOD Maven plugin. These tests ensure API required by plugin is implemented.
 */
@RunWith(MockitoJUnitRunner.class)
public class BuilderMavenApiTest {
    @Test
    public void GivenProjectDir_WhenSetProjectDir_ThenGetIt() {
        // GIVEN
        BuilderConfig config = new BuilderConfig();
        File projectDir = mock(File.class);

        // WHEN
        config.setProjectDir(projectDir);

        // THEN
        File dir = config.getProjectDir();
        assertThat(dir, notNullValue());
    }

    @Test
    public void GivenBuildNumber_WhenSetBuildNumber_ThenGetIt() {
        // GIVEN
        BuilderConfig config = new BuilderConfig();
        int buildNumber = 4;

        // WHEN
        config.setBuildNumber(buildNumber);

        // THEN
        assertThat(config.getBuildNumber(), equalTo(buildNumber));
    }

    @Test
    public void GivenPluginSimulation_WhenCreateBuilder_ThenConfigGettersInvoked() throws IOException {
        // GIVEN
        BuilderConfig config = mock(BuilderConfig.class);
        when(config.getProjectDir()).thenReturn(new File("src/test/resources/build-fs"));
        when(config.getBuildNumber()).thenReturn(1);

        // WHEN
        Builder builder = new Builder(config);

        // THEN
        assertThat(builder, notNullValue());
        verify(config, times(1)).getProjectDir();
        verify(config, times(1)).getBuildNumber();
    }

    @Test
    public void GivenProjectBuildDir_WhenGetBuildDirFromMavenPlugin_ThenReturnIt() throws IOException {
        // GIVEN
        BuilderConfig config = mock(BuilderConfig.class);
        when(config.getProjectDir()).thenReturn(new File("src/test/resources/build-fs"));
        when(config.getBuildNumber()).thenReturn(1);
        Builder builder = new Builder(config);

        // WHEN
        FilePath buildDir = builder.getBuildDir();

        // THEN
        assertThat(buildDir, Matchers.notNullValue());
        assertThat(buildDir.getName(), Matchers.equalTo("build"));
    }
}
