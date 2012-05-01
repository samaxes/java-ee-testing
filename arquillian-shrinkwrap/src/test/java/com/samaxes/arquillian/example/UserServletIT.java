package com.samaxes.arquillian.example;

import static org.testng.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class UserServletIT extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(UserServletIT.class.getName());

    // Not managed, should be used for external calls (e.g. HTTP)
    @Deployment(testable = false)
    public static WebArchive createNotTestableDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "example.war")
                .addClasses(User.class, UserServiceBean.class, UserServlet.class)
                .addAsResource("META-INF/persistence.xml")
                // Enable CDI
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));

        LOGGER.info(war.toString(Formatters.VERBOSE));

        return war;
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void callServletToAddNewUserToDB(@ArquillianResource URL baseURL) throws IOException {
        // Servlet is listening at <context_path>/User
        final URL url = new URL(baseURL, "User");
        final User user = new User(1L, "Ike");

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();

        assertEquals(builder.toString(), user.toString());
    }
}
