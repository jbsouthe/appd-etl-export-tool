package com.cisco.josouthe.util;

import junit.framework.TestCase;
import org.junit.Test;

public class UtilityTest extends TestCase {

    public UtilityTest() {}

    @Test
    public void testIsAPIKeyValidFormat() {
        assert Utility.isAPIKeyValidFormat("ETLClient@southerland-test", "southerland-test");
        assert !Utility.isAPIKeyValidFormat("ETLClient", "southerland-test");
        assert !Utility.isAPIKeyValidFormat("ETLClient@sutherland-test", "southerland-test");
    }
}