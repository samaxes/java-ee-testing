In <a href="http://www.samaxes.com/2011/12/javaee-testing-ejb31-embeddable/">Java EE 6 Testing Part I</a> I briefly introduced the EJB 3.1 Embeddable API using Glassfish embedded container to demonstrate how to start the container, lookup a bean in the project classpath and run a very simple integration test.

This post focus on <a href="http://www.jboss.org/arquillian">Arquillian</a> and <a href="http://www.jboss.org/shrinkwrap">ShrinkWrap</a> and why they are awesome tools for integration testing of enterprise Java applications.

The source code used for this post is available on <a href="https://github.com/samaxes/java-ee-testing" target="_blank">GitHub</a> under the folder <code>arquillian-shrinkwrap</code>.

<h2>The tools</h2>

<dl>
<dt>Arquillian</dt>
<dd>
<blockquote cite="http://www.jboss.org/arquillian">
    Arquillian brings test execution to the target runtime, alleviating the burden on the developer of managing the runtime from within the test or project build. To invert this control, Arquillian wraps a lifecycle around test execution that does the following:
    <ul>
        <li>Manages the lifecycle of one or more containers</li>
        <li>Bundles the test case, dependent classes and resources as ShrinkWrap archives</li>
        <li>Deploys the archives to the containers</li>
        <li>Enriches the test case with dependency injection and other declarative services</li>
        <li>Executes the tests inside (or against) the containers</li>
        <li>Returns the results to the test runner for reporting</li>
    </ul>
</blockquote>
</dd>

<dt>ShrinkWrap</dt>
<dd>
<blockquote cite="http://www.jboss.org/shrinkwrap">
    ShrinkWrap, a central component of Arquillian, provides a simple mechanism to assemble archives like JARs, WARs, and EARs with a friendly, fluent API.
</blockquote>
</dd>
</dl>

One of the major benefits of using Arquillian is that you run the tests in a remote container (i.e. application server). That means you'll be testing the <em>real deal</em>. No mocks. Not even embedded runtimes!

<h2>Agenda</h2>

The following topics will be covered on this post:

<ul>
    <li>Configure the Arquillian infrastructure in a Maven-based Java project</li>
    <li>Inject EJBs and Managed Beans (CDI) directly in test instances</li>
    <li>Test Java Persistence API (JPA) layer</li>
    <li>Run Arquillian in client mode</li>
    <li>Run and debug Arquillian tests inside your IDE</li>
</ul>

<h2>Configure Maven to run integration tests</h2>

To run integration tests with Maven we need a different approach. By different approach I mean a different plugin: the <a href="http://maven.apache.org/plugins/maven-failsafe-plugin/" title="Maven Failsafe Plugin" target="_blank">Maven Failsafe Plugin</a>.

The Failsafe Plugin is a fork of the <a href="http://maven.apache.org/plugins/maven-surefire-plugin/" title="Maven Surefire Plugin" target="_blank">Maven Surefire Plugin</a> designed to run integration tests.

The Failsafe plugin goals are designed to run after the package phase, on the integration-test phase.

The Maven lifecycle has four phases for running integration tests:

<ul>
    <li><strong>pre-integration-test:</strong> on this phase we can start any required service or do any action, like starting a database, or starting a webserver, anything...</li>
    <li><strong>integration-test:</strong> failsafe will run the test on this phase, so after all required services are started.</li>
    <li><strong>post-integration-test:</strong> time to shutdown all services...</li>
    <li><strong>verify:</strong> failsafe runs another goal that interprets the results of tests here, if any tests didn't pass failsafe will display the results and exit the build.</li>
</ul>

Configuring Failsafe in the POM:

[xml title="pom.xml"]
<!-- clip -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>2.12</version>
    <configuration>
        <skipTests>true</skipTests>
    </configuration>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>2.12</version>
    <configuration>
        <encoding>UTF-8</encoding>
    </configuration>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>integration-test</goal>
            </goals>
        </execution>
        <execution>
            <id>verify</id>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<!-- clip -->
[/xml]

By default, the Surefire plugin executes <code>**/Test*.java</code>, <code>**/*Test.java</code>, and <code>**/*TestCase.java</code> test classes. The Failsafe plugin will look for <code>**/IT*.java</code>, <code>**/*IT.java</code>, and <code>**/*ITCase.java</code>. If you are using both the Surefire and Failsafe plugins, make sure that you use this naming convention to make it easier to identify which tests are being executed by which plugin.

<h2>Configure Arquillian infrastructure in Maven</h2>

Configure your Maven project descriptor to use Arquillian by appending the following XML fragment:

[xml title="pom.xml"]
<!-- clip -->
<repositories>
    <repository>
        <id>jboss-public-repository-group</id>
        <name>JBoss Public Repository Group</name>
        <url>http://repository.jboss.org/nexus/content/groups/public/</url>
    </repository>
</repositories>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.jboss.arquillian</groupId>
            <artifactId>arquillian-bom</artifactId>
            <version>1.0.0.CR8</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.jboss.arquillian.testng</groupId>
        <artifactId>arquillian-testng-container</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>6.4</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.jboss.spec</groupId>
        <artifactId>jboss-javaee-6.0</artifactId>
        <version>3.0.1.Final</version>
        <type>pom</type>
        <scope>provided</scope>
    </dependency>
</dependencies>

<profiles>
    <profile>
        <id>jbossas-remote-7</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.jboss.as</groupId>
                <artifactId>jboss-as-arquillian-container-remote</artifactId>
                <version>7.1.1.Final</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </profile>
</profiles>
<!-- clip -->
[/xml]

Arquillian has a vast list of <a href="https://docs.jboss.org/author/display/ARQ/Container+adapters" title="Arquillian container adapters" target="_blank">container adapters</a>. An Arquillian test can be executed in any container that is compatible with the programming model used in the test. However, throughout this post, only JBoss AS 7 is used.
Similarly to <a href="/2011/12/javaee-testing-ejb31-embeddable/" title="EJB 3.1 Embeddable API">Java EE 6 Testing Part I</a>, I chose to use <a href="http://testng.org/">TestNG</a> testing framework, but again, <a href="http://www.junit.org/">JUnit</a> should work just as well.

<h2>Create testable components</h2>

Before looking at how to write integration tests with Arquillian we first need to have a component to test.
A <a href="http://docs.oracle.com/javaee/6/tutorial/doc/gipjg.html" title="What Is a Session Bean?" target="_blank">Session Bean</a> is a common component in Java EE stack and will serve as test subject. In this post, I'll be creating a very basic backend for adding new users to a database.

[java title="src/main/java/com/samaxes/arquillian/example/UserServiceBean.java"]
@Stateless
public class UserServiceBean {

    @PersistenceContext
    private EntityManager em;

    public User addUser(User user) {
        em.persist(user);
        return user;
    }

    // Annotation says that we do not need to open a transaction
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findUserById(Long id) {
        return em.find(User.class, id);
    }
}
[/java]

In the code above I use <a href="http://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html" title="Java Persistence API" target="_blank">JPA</a> and so we need a persistence unit.
A <a href="http://docs.oracle.com/javaee/6/tutorial/doc/bnbqw.html#bnbrj" title="Persistence Units" target="_blank">persistence unit</a> defines a set of all entity classes that are managed by <code>EntityManager</code> instances in an application. This set of entity classes represents the data contained within a single data store.
Persistence units are defined by the <code>persistence.xml</code> configuration file:

[xml title="src/main/resources/META-INF/persistence.xml"]
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="http://java.sun.com/xml/ns/persistence"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://java.sun.com/xml/ns/persistence
        http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
    version="2.0">
    <persistence-unit name="example">
        <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>
        <properties>
            <property name="hibernate.hbm2ddl.auto" value="create-drop" />
            <property name="hibernate.show_sql" value="true" />
        </properties>
    </persistence-unit>
</persistence>
[/xml]

In this example I'm using an example data source that uses H2 database and comes already configured with JBoss AS 7.

Finally, we also need an entity that maps to a table in the database:

[java title="src/main/java/com/samaxes/arquillian/example/User.java"]
@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String name;

    // Removed constructors, getters and setters for brevity

    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + "]";
    }
}
[/java]

<h2>Test JPA with Arquillian</h2>

We are now all set to write our first Arquillian test.
An Arquillian test case looks just like a unit test with some extras. It must have three things:

<ul>
    <li>Extend Arquillian class (this is specific to TestNG, with JUnit you need a <code>@RunWith(Arquillian.class)</code> annotation on the class)</li>
    <li>A public static method annotated with @Deployment that returns a ShrinkWrap archive</li>
    <li>At least one method annotated with @Test</li>
</ul>

[java title="src/main/java/com/samaxes/arquillian/example/UserServiceBeanIT.java"]
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
        final User user = new User("Ike");
        service.addUser(user);
        assertNotNull(user.getId(), "User id should not be null!");
    }
}
[/java]

This test is straightforward, it inserts a new user and checks that the <code>id</code> property has been filled with the generated value from the database.
Since the test is enriched by Arquillian, you can inject EJBs and managed beans normally using <code>@EJB</code> or <code>@Inject</code> annotations.
The method annotated with <code>@Deployment</code> uses ShrinkWrap to build a JAR archive which will be deployed to the container and to which your tests will be run against. ShrinkWrap isolates the classes and resources which are needed by the test from the remainder of the classpath, you should include every component needed for the test to run inside the deployment archive.

<h2>Client mode</h2>

Arquillian supports three test run modes:
<ul>
    <li><strong>In-container mode</strong> is to test your application internals. This gives Arquillian the ability to communicate with the test, enrich the test and run the test remotely. In this mode, the test executes in the remote container; Arquillian uses this mode by default.</li>
    <li><strong>Client mode</strong> is to test how your application is used by clients. As opposed to in-container mode which repackages and overrides the test execution, the client mode does as little as possible. It does not repackage your <code>@Deployment</code> nor does it forward the test execution to a remote server. Your test case is running in your JVM as expected and you're free to test the container from the outside, as your clients see it. The only thing Arquillian does is to control the lifecycle of your <code>@Deployment</code>.</li>
    <li><strong>Mixed mode</strong> allows to mix the two run modes within the same test class.</li>
</ul>

To run Arquillian in client mode lets first build a servlet to be tested:

[java title="src/main/java/com/samaxes/arquillian/example/UserServlet.java"]
@WebServlet("/User")
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = -7125652220750352874L;

    @Inject
    private UserServiceBean service;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();
        out.println(service.addUser(new User("Ike")).toString());
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        doGet(request, response);
    }
}
[/java]

And now lets test it:

[java title="src/main/java/com/samaxes/arquillian/example/UserServletIT.java"]
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

    @RunAsClient // Same as @Deployment(testable = false), should only be used in mixed mode
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
[/java]

Although this test is very simple, it allows you to test multiple layers of you application with a single method call.

<h2>Run tests inside Eclipse</h2>

You can run an Arquillian test from inside your IDE just like a unit test.

<h3>Run an Arquillian test</h3>
<small>(Click on the images to enlarge)</small>

<ol>
    <li>
Install <a href="http://marketplace.eclipse.org/content/testng-eclipse" title="TestNG for Eclipse" target="_blank">TestNG</a> and <a href="http://marketplace.eclipse.org/content/jboss-tools-indigo" title="JBoss Tools" target="_blank">JBoss Tools</a> Eclipse plugins.
    </li>
    <li>
Add a new JBoss AS server to Eclipse:
<a href="http://samaxes.appspot.com/images/arquillian/add-jboss-server-big.png" title="Add JBoss AS server to Eclipse" target="_blank"><img src="http://samaxes.appspot.com/images/arquillian/add-jboss-server.png" alt="Add JBoss AS server to Eclipse" /></a>
    </li>
    <li>
Start JBoss AS server:
<a href="http://samaxes.appspot.com/images/arquillian/start-jboss-big.png" title="Start JBoss AS server" target="_blank"><img src="http://samaxes.appspot.com/images/arquillian/start-jboss.png" alt="Start JBoss AS server" /></a>
    </li>
    <li>
Run the test case from Eclipse, right click on the test file on the Project Explorer and select <code>Run As > TestNG Test</code>:
<a href="http://samaxes.appspot.com/images/arquillian/arquillian-testng-eclipse-test-big.png" title="Run TestNG Arquillian test" target="_blank"><img src="http://samaxes.appspot.com/images/arquillian/arquillian-testng-eclipse-test.png" alt="Run TestNG Arquillian test" /></a>
    </li>
</ol>

The result should look similar to this:
<a href="http://samaxes.appspot.com/images/arquillian/arquillian-testng-eclipse-result-big.png" title="TestNG Arquillian test result" target="_blank"><img src="http://samaxes.appspot.com/images/arquillian/arquillian-testng-eclipse-result.png" alt="TestNG Arquillian test result" /></a>

<h3>Debug an Arquillian test</h3>
<small>(Click on the images to enlarge)</small>

Since we are using a remote container <code>Debug As > TestNG Test</code> does not cause breakpoints to be activated.
Instead, we need to start the container in debug mode and attach the debugger. That's because the test is run in a different JVM than the original test runner.
The only change you need to make to debug your test is to start JBoss AS server in debug mode:

<ol>
    <li>
Start JBoss AS server debug mode:
<a href="http://samaxes.appspot.com/images/arquillian/debug-jboss-big.png" title="Debug JBoss AS server" target="_blank"><img src="http://samaxes.appspot.com/images/arquillian/debug-jboss.png" alt="Debug JBoss AS server" /></a>
    </li>
    <li>
Add the breakpoints you need to your code.
    </li>
    <li>
And debug it by right clicking on the test file on the Project Explorer and selecting <code>Run As > TestNG Test</code>:
<a href="http://samaxes.appspot.com/images/arquillian/debug-arquillian-test-big.png" title="Debug Arquillian test" target="_blank"><img src="http://samaxes.appspot.com/images/arquillian/debug-arquillian-test.png" alt="Debug Arquillian test" /></a>
    </li>
</ol>

<h2>More resources</h2>

I hope to have been able to highlight some of the benefits of Arquillian.
For more Arquillian awesomeness take a look at the following resources:

<ul>
    <li><a href="http://arquillian.org/guides/" title="Arquillian Guides" target="_blank">Arquillian Guides</a></li>
    <li><a href="http://arquillian.org/community/" title="Arquillian Community" target="_blank">Arquillian Community</a></li>
    <li><a href="https://github.com/arquillian" title="Arquillian Git Repository" target="_blank">Arquillian Git Repository</a></li>
</ul>