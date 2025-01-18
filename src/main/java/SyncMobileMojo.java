import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Mojo(name = "sync-mobile", defaultPhase = LifecyclePhase.PACKAGE)
public class SyncMobileMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException {
        try {
            Properties properties = Commons.loadApplicationProperties();
            File appDir = new File(Commons.MOBILE+"/"+Commons.MY_APP);
            // Step 4: Update capacitor.config.json
            Commons.sync(appDir, properties);
        } catch (IOException e) {
            throw new MojoExecutionException("Error during sync-mobile goal", e);
        }
    }
}
