import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

@Mojo(name = "sync", defaultPhase = LifecyclePhase.PACKAGE)
public class SyncMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException {
        try {
            Properties properties = Commons.loadApplicationProperties();
            File appDir = new File(Commons.NATIVES +"/"+Commons.APPS);
            // Update capacitor.config.json
            Commons.syncMobile(appDir, properties);
            // Sync Electron
            Commons.syncApp(appDir, properties);
        } catch (IOException e) {
            throw new MojoExecutionException("Error during mobile sync", e);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Error during app sync", e);
        }
    }
}
