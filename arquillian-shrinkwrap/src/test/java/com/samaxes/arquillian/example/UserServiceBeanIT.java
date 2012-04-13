package com.samaxes.arquillian.example;

import static org.testng.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

public class UserServiceBeanIT extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(UserServiceBeanIT.class.getName());

    @Inject
    private UserServiceBean service;

    @Deployment
    public static JavaArchive createTestableDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "example.jar")
                .addClasses(User.class, UserServiceBean.class)
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml")
                // Enable CDI
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));

        LOGGER.info(jar.toString(Formatters.VERBOSE));

        return jar;
    }

    @Test
    public void callServiceToAddNewUserToDB() {
        final User user = new User("First Ike");
        service.addUser(user);
        assertNotNull(user.getId(), "User id should not be null!");
    }
}
