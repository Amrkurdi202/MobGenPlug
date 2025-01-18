import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

@Mojo(name = "make-mobile", defaultPhase = LifecyclePhase.PACKAGE)
public class MakeMobileMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException {
        try {
            // Load properties
            Properties properties = Commons.loadApplicationProperties();
            String appName = properties.getProperty("spring.application.name", "DefaultAppName");
            String appId = properties.getProperty("quentity.app.id", "com.example.defaultapp");

            File mobileDir = new File(Commons.MOBILE);
            if (!mobileDir.exists() && !mobileDir.mkdir()) {
                throw new MojoExecutionException("Failed to create mobile directory");
            }

            // Step 1: Initialize Capacitor app
            Commons.executeCommand("npm init @capacitor/app -- " + Commons.MY_APP + " --name " + appName + " --app-id " + appId, mobileDir);

            File appDir = new File(mobileDir, Commons.MY_APP);

            // Step 2: Install dependencies
            Commons.executeCommand("npm i @capacitor/core", mobileDir);
            Commons.executeCommand("npm i -D @capacitor/cli", mobileDir);
            Commons.executeCommand("npm i @capacitor/android @capacitor/ios", mobileDir);

            // Step 3: Add platforms
            Commons.executeCommand("npx cap add android", appDir);
            Commons.executeCommand("npx cap add ios", appDir);

            // Step 4: Update capacitor.config.json
            Commons.sync(appDir, properties);

        } catch (IOException e) {
            throw new MojoExecutionException("Error during make-mobile goal", e);
        }
    }


}
