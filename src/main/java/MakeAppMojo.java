
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.*;
import java.util.Properties;

@Mojo(name = "make-app", defaultPhase = LifecyclePhase.PACKAGE)
public class MakeAppMojo extends AbstractMojo {
    public void execute() throws MojoExecutionException {
        try {
            System.out.println(System.getenv("PATH"));
            System.out.println(System.getenv("SHELL"));

            // Load properties
            Properties properties = Commons.loadApplicationProperties();
            String appName = properties.getProperty("spring.application.name", "DefaultAppName");
            String appId = properties.getProperty("quentity.app.id", "com.example.defaultapp");

            File mobileDir = new File(Commons.NATIVES);
            if (!mobileDir.exists() && !mobileDir.mkdir()) {
                throw new MojoExecutionException("Failed to create mobile directory");
            }

            // Step 1: Initialize Capacitor app
            Commons.executeCommand("npm" + Commons.CMD + " init @capacitor/app -- " + Commons.APPS + " --name " + appName + " --app-id " + appId, mobileDir);

            File appDir = new File(mobileDir, Commons.APPS);

            // Step 2: Install dependencies
            Commons.executeCommand("npm" + Commons.CMD + " i @capacitor/core", mobileDir);
            Commons.executeCommand("npm" + Commons.CMD + " i -D @capacitor/cli", mobileDir);
            Commons.executeCommand("npm" + Commons.CMD + " i @capacitor/android @capacitor/ios", mobileDir);

            // Step 3: Add platforms
            Commons.executeCommand("npx" + Commons.CMD + " cap add android", appDir);
            Commons.executeCommand("npx" + Commons.CMD + " cap add ios", appDir);

            // Step 4: Update capacitor.config.json
            Commons.syncMobile(appDir, properties);

            // Step 5: Install electron dependence
            Commons.executeCommand("npm" + Commons.CMD + " i @capacitor-community/electron", mobileDir);

            // Step 5: Make Symbolic Link
            if (!isRunningAsAdmin()) {
                try {
                    runAsAdmin();
                    return;
                } catch (IOException e) {
                    System.err.println("Failed to restart as Administrator: " + e.getMessage());
                    return;
                }
            }
            makeSymbolicLink(appDir);

            // Step 6: Add Capacitor/electron

            Commons.executeCommand("npx" + Commons.CMD + " cap add @capacitor-community/electron", appDir);

            Commons.syncApp(appDir, properties);


        } catch (IOException e) {
            throw new MojoExecutionException("Error during make-mobile goal", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }



    private static void makeSymbolicLink(File appDir) {

        Path link = Paths.get(appDir.getAbsolutePath(), "node_modules");
        Path target = Paths.get(appDir.getAbsolutePath(), "../node_modules");

        try {
            Files.createSymbolicLink(link, target);
            System.out.println("Symbolic link created: " + link + " -> " + target);
        } catch (UnsupportedOperationException e) {
            System.err.println("Symbolic links are not supported on this file system.");
        } catch (IOException e) {
            System.err.println("Error creating symbolic link: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Insufficient permissions to create symbolic link.");
        }
    }

    private static boolean isRunningAsAdmin() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                Process process = Runtime.getRuntime().exec("net session");
                process.waitFor();
                return process.exitValue() == 0;
            } catch (IOException | InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    private static void runAsAdmin() throws IOException {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String jarPath = MakeAppMojo.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        jarPath = jarPath.replaceFirst("^/(.:/)", "$1");

        String[] command = new String[]{
                "powershell.exe",
                "-Command",
                "Start-Process",
                "\"" + javaBin + "\"",
                "-ArgumentList '\"" + jarPath + "\"'",
                "-Verb RunAs"
        };

        System.out.println("Attempting to restart as Administrator...");
        new ProcessBuilder(command).inheritIO().start();
    }


}
