package com.cisco.josouthe.data;

import com.cisco.josouthe.data.analytic.Result;
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

public class AnalyticsTest extends TestCase {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Analytics analytics = null;
    private Properties testProperties = null;
    private boolean initialized = false;

    public AnalyticsTest() {

    }

    @Before
    public void setUp() throws Exception {
        Configurator.setAllLevels("", Level.ALL);
        try {
            testProperties = new Properties();
            testProperties.load(new FileInputStream(new File("./test-settings.properties")));
            analytics = new Analytics( testProperties.getProperty("analytics.url"), testProperties.getProperty("analytics.account"), testProperties.getProperty("analytics.apikey") , null, null);
            this.initialized=true;
        } catch (Exception exception ) {
            throw exception;
        }
    }

    @Test
    public void testRunAnalQuery() {
        Result[] results = analytics.runAnalyticsQuery("UniqueTransactionCount","SELECT transactionName, count(*) FROM transactions");
        for( Result result : results)
            logger.info(result);

        //SELECT transactionName, eventTimestamp, application, segments.tier FROM transactions
        results = analytics.runAnalyticsQuery("TransactionDetails","SELECT transactionName, eventTimestamp, application, segments.tier, count(*) FROM transactions");
        for( Result result : results)
            logger.info(result);
    }
}