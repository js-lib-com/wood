package js.wood.cli.icons;

import static com.jslib.tools.AbstractToolProcess.buildCommand;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.tools.IResultParser;
import com.jslib.tools.imagick.ConvertProcess;
import com.jslib.tools.imagick.ImageMagickProcess;

import js.util.Classes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "icons", description = "Create application icons.")
public class CreateIcons extends Task {
	private static int[] ICON_VARIANTS = new int[] { 128, 144, 152, 192, 256 };

	@Option(names = { "-o", "--overwrite" }, description = "Force overwrite existing icons.")
	private boolean overwrite;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	private final ImageMagickProcess convert;

	public CreateIcons() throws IOException {
		super();
		this.convert = new ConvertProcess();
	}

	@Override
	protected ExitCode exec() throws Exception {

		Path projectDir = files.getProjectDir();
		String projectName = files.getFileName(projectDir);
		Path assetDir = projectDir.resolve(config.getex("asset.dir"));

		if (!files.exists(assetDir)) {
			console.print("Bad asset dir configuration: %s.", assetDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path icon512 = assetDir.resolve("app-icon-512.png");
		if (overwrite || !files.exists(icon512)) {
			String backgroundColor = console.input("background color", randomColor());
			Path backgroundFile = projectDir.resolve("background.png");
			imagick("-size 512x512 xc:none -fill ${color} -draw \"circle 256,256 256,1\" ${file}", backgroundColor, backgroundFile);
			BrightnessResult brightness = imagick(BrightnessResult.class, "${file} -colorspace Gray -format \"%[mean]\" info:", backgroundFile);

			String textColor = console.input("text color", brightness.isLightColor() ? "black" : "white");
			boolean sphereEffect = console.input("sphere effect: yes | no", "no").equalsIgnoreCase("yes");

			if (sphereEffect) {
				Path lightEffectFile = projectDir.resolve("light-effect.png");
				imagick("-size 512x512 canvas:none -draw \"circle 256,256 256,146\" -negate -channel A -gaussian-blur 0x80 ${file}", lightEffectFile);
				imagick("-composite -compose atop -geometry -95-124 ${source} ${effect} ${target}", backgroundFile, lightEffectFile, backgroundFile);
				files.delete(lightEffectFile);
			}

			Path textFile = projectDir.resolve("text.png");
			String label = projectName.substring(0, 2).toUpperCase();
			if (sphereEffect) {
				imagick("-size 512x512 -background transparent -fill ${color} -pointsize 300 -wave 50x1024 -gravity center caption:${label} ${file}", textColor, label, textFile);
				imagick("-composite -geometry -0-50 ${background} ${text} ${icon}", backgroundFile, textFile, icon512);
			} else {
				imagick("-size 512x512 -background transparent -fill ${color} -pointsize 256 -gravity center caption:${label} ${file}", textColor, label, textFile);
				imagick("-composite ${background} ${text} ${icon}", backgroundFile, textFile, icon512);
			}

			files.delete(backgroundFile);
			files.delete(textFile);
		}

		for (int variant : ICON_VARIANTS) {
			Path icon = assetDir.resolve(format("app-icon-%d.png", variant));
			if (overwrite || !files.exists(icon)) {
				String w = Integer.toString(variant);
				String h = Integer.toString(variant);
				imagick("${imageFile} -resize ${width}x${height} ${targetFile}", icon512, w, h, icon);
			}
		}

		Path featured = assetDir.resolve("app-featured.png");
		if (overwrite || !files.exists(featured)) {
			Path backgroundFile = projectDir.resolve("background.png");
			// reverse order of width and height because of -rotate 90
			imagick("-size 500x952 xc:red -colorspace HSB gradient: -compose CopyRed -composite -colorspace RGB -rotate 90.0 ${file}", backgroundFile);
			imagick("${source} -brightness-contrast -30x-40 ${target}", backgroundFile, backgroundFile);

			Path iconFile = assetDir.resolve("app-icon-256.png");
			Path featuredFile = assetDir.resolve("app-featured.png");
			// offset +122 is adjusted for icon size of 256: (500 - 256) / 2
			imagick("-composite -compose atop -geometry +122+122 ${background} ${icon} ${featured}", backgroundFile, iconFile, featuredFile);

			files.delete(backgroundFile);
		}

		return ExitCode.SUCCESS;
	}

	private void imagick(String command, Object... args) throws IOException {
		imagick(null, command, args);
	}

	private <T extends IResultParser> T imagick(Class<T> resultType, String parameterizedCommand, Object... args) throws IOException {
		// cannot set binary path on constructor because config is not initialized there
		ConvertProcess.setPath(config.getex("imagick.convert.path"));

		String command = buildCommand(parameterizedCommand, args);
		if (verbose) {
			console.print(command);
			convert.setConsole(console);
		}
		convert.setTimeout(30000L);
		if (resultType == null) {
			convert.exec(command);
			return null;
		}
		return convert.exec(command, resultType);
	}

	private static final List<String> colors = new ArrayList<>();

	private static String randomColor() throws IOException {
		if (colors.isEmpty()) {
			try (BufferedReader reader = new BufferedReader(Classes.getResourceAsReader("imagick-colors"))) {
				String color;
				while ((color = reader.readLine()) != null) {
					colors.add(color);
				}
			}
		}
		Random random = new Random();
		return colors.get(random.nextInt(colors.size()));
	}

	private static class BrightnessResult implements IResultParser {
		// this value is determined heuristically
		private static final double LIGH_COLOR_THRESHOLD = 30000;

		private double brightness;

		public boolean isLightColor() {
			return brightness >= LIGH_COLOR_THRESHOLD;
		}

		@Override
		public void parse(String line) {
			brightness = Double.parseDouble(line);
		}
	}
}
