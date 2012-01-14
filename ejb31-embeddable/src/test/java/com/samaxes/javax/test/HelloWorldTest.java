/*
 * $Id$
 *
 * Copyright 2011 samaxes.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samaxes.javax.test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.samaxes.javax.test.HelloWorld;

public class HelloWorldTest {
    private static EJBContainer ejbContainer;

    private static Context ctx;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Instantiate an embeddable EJB container and search the
        // JVM class path for eligible EJB modules or directories
        ejbContainer = EJBContainer.createEJBContainer();

        // Get a naming context for session bean lookups
        ctx = ejbContainer.getContext();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Shutdown the embeddable container
        ejbContainer.close();
    }

    @Test
    public void hello() throws NamingException {
        // Retrieve a reference to the session bean using a portable
        // global JNDI name
        HelloWorld helloWorld = (HelloWorld)
                ctx.lookup("java:global/classes/HelloWorld");

        // Do your tests
        assertNotNull(helloWorld);
        String expected = "World";
        String hello = helloWorld.hello(expected);
        assertNotNull(hello);
        assertTrue(hello.endsWith(expected));
    }
}
