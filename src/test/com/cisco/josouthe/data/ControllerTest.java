package com.cisco.josouthe.data;

import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.data.model.Model;
import junit.framework.TestCase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ControllerTest extends TestCase {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Controller controller = null;
    private Properties testProperties = null;
    private boolean initialized = false;

    public ControllerTest() {
    }

    @Before
    public void setUp() throws Exception {
        Configurator.setAllLevels("", Level.ALL);
        try {
            testProperties = new Properties();
            testProperties.load(new FileInputStream(new File("./test-settings.properties")));
            this.controller = new Controller(testProperties.getProperty("controller.url"), testProperties.getProperty("controller.client.id"),
                    testProperties.getProperty("controller.client.secret"), null, false, null);
            this.initialized=true;
        } catch (Exception exception ) {
            throw exception;
        }
    }

    @Test
    public void testModel() throws Exception {
        Model model = controller.getModel();
    }

    @Test
    public void testApplicationId() throws Exception {
        controller.getApplicationId( testProperties.getProperty("controller.application.name") );
    }

    @Test
    public void testApplicationBaselines() throws Exception {
        controller.getAllBaselines( controller.getApplicationId( testProperties.getProperty("controller.application.name") ) );
    }

    @Test
    public void testSavedAnalSearches() throws Exception {
        Search[] searches = controller.getAllSavedSearchesFromController();
        for( Search search : searches ) {
            System.out.println(String.format("<Search name=\"%s\" visualization=\"%s\" >%s</Search>",search.getName(), search.visualization, search.getQuery()));
        }
    }
}