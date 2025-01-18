import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class Commons {
    public static final String MY_APP = "my-app";
    public static final String MOBILE = "mobile";

    public static void executeCommand(String command, File workingDir) throws IOException, MojoExecutionException {
        // Check if npm exists
        if (!isCommandAvailable("npm")) {
            throw new MojoExecutionException("Error: 'npm' is not installed or not available in the system PATH. Please install Node.js and npm.");
        }

        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDir);
        executor.execute(cmdLine);
    }

    public static boolean isCommandAvailable(String command) {
        try {
            CommandLine cmdLine = CommandLine.parse(command + " --version");
            DefaultExecutor executor = new DefaultExecutor();
            executor.execute(cmdLine);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void copyDirectory(File source, File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }
        for (File file : source.listFiles()) {
            File destFile = new File(destination, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, destFile);
            } else {
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static void updateCapacitorConfig(File configFile, String webDir, String serverUrl) throws IOException {
        String content = Files.readString(configFile.toPath());
        JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
        jsonObject.addProperty("webDir", webDir);

        JsonObject server = new JsonObject();
        server.addProperty("url", serverUrl);
        jsonObject.add("server", server);

        Files.writeString(configFile.toPath(), jsonObject.toString(), StandardCharsets.UTF_8);
    }

    public static Properties loadApplicationProperties() throws MojoExecutionException {
        Properties properties = new Properties();
        File propertiesFile = new File("src/main/resources/application.properties");

        if (!propertiesFile.exists()) {
            throw new MojoExecutionException("application.properties file not found at " + propertiesFile.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading application.properties file", e);
        }

        return properties;
    }

    public static void sync(File appDir, Properties properties) throws IOException, MojoExecutionException {
        String appUrl = properties.getProperty("quentity.app.url");
        File configFile = new File(appDir, "capacitor.config.json");
        if (configFile.exists()) {
            Commons.updateCapacitorConfig(configFile, "src", appUrl);
        } else {
            throw new MojoExecutionException("capacitor.config.json not found in " + appDir.getAbsolutePath());
        }

        // Step 5: Copy frontend files
        File frontendSrc = new File("src/main", "frontend");
        File mobileSrc = new File(appDir, "src");
        Commons.copyDirectory(frontendSrc, mobileSrc);

        // Step 6: Sync Capacitor
        Commons.executeCommand("npx cap sync", appDir);
    }

}
