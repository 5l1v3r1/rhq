package org.rhq.core.domain.drift;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;

public class DriftConfigurationTest {
    @Test
    public void getCompareIgnoreIncludesExcludes() {
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(true);

        DriftConfiguration dc1 = new DriftConfiguration(new Configuration());
        DriftConfiguration dc2 = new DriftConfiguration(new Configuration());

        // make sure our comparator can deal with all the nulls that are in empty configs
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2;
        dc1.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.fileSystem, "/foo"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setEnabled(true);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setInterval(1000L);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setName("the-name");
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.fileSystem, "/foo"));
        dc2.setEnabled(true);
        dc2.setInterval(1000L);
        dc2.setName("the-name");

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2;

        dc1.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.pluginConfiguration, "/foo"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + "  should have different basedir than " + dc2;

        dc1.setBasedir(dc2.getBasedir()); // put them back to the same value
        dc1.setEnabled(!dc2.isEnabled());
        assert comparator.compare(dc1, dc2) < 0 : dc1 + "  should have different enabled than " + dc2;

        dc1.setEnabled(dc2.isEnabled()); // put them back to the same value
        dc1.setInterval(dc2.getInterval() + 2222L);
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different interval than " + dc2;

        dc1.setInterval(dc2.getInterval()); // put them back to the same value
        dc1.setName("zzzzz" + dc2.getName());
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different name than " + dc2;

        dc1.setName(dc2.getName()); // put them back to the same value
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check - we should be back to equals

        // add some includes and excludes that are different and test that they are ignored by our comparator
        dc1.addInclude(new DriftConfiguration.Filter("ipath1", "ipattern1"));
        dc2.addInclude(new DriftConfiguration.Filter("ipath2", "ipattern2"));
        dc1.addExclude(new DriftConfiguration.Filter("epath1", "epattern1"));
        dc2.addExclude(new DriftConfiguration.Filter("epath2", "epattern2"));

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal (ignoring includes/excludes) " + dc2;

        // now show that our non-ignoring comparator would detect a different
        comparator = new DriftConfigurationComparator(false);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal (not ignoring includes/excludes) " + dc2;
    }

    @Test
    public void getCompareIncludesExcludes() {
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(false);

        DriftConfiguration dc1 = new DriftConfiguration(new Configuration());
        DriftConfiguration dc2 = new DriftConfiguration(new Configuration());

        dc1.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.pluginConfiguration, "hello.world"));
        dc1.setEnabled(true);
        dc1.setInterval(1000L);
        dc1.setName("the-name");

        dc2.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.pluginConfiguration, "hello.world"));
        dc2.setEnabled(true);
        dc2.setInterval(1000L);
        dc2.setName("the-name");

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check

        dc1.addInclude(new DriftConfiguration.Filter("ipath1", "ipattern1"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addInclude(new DriftConfiguration.Filter("ipath1", "ipattern1"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // add a second include to see that we test multiple filters
        dc1.addInclude(new DriftConfiguration.Filter("ipath2", "ipattern2"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addInclude(new DriftConfiguration.Filter("ipath2", "ipattern2"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // side test just to see null patterns work
        dc1.addInclude(new DriftConfiguration.Filter("ipath3", null));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addInclude(new DriftConfiguration.Filter("ipath3", null));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // now test excludes

        dc1.addExclude(new DriftConfiguration.Filter("epath1", "epattern1"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addExclude(new DriftConfiguration.Filter("epath1", "epattern1"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // add a second exclude to see that we test multiple filters
        dc1.addExclude(new DriftConfiguration.Filter("epath2", "epattern2"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addExclude(new DriftConfiguration.Filter("epath2", "epattern2"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // now test that we have the same number of filters but they differ

        dc1.addInclude(new DriftConfiguration.Filter("ipathA", "ipatternA"));
        dc2.addInclude(new DriftConfiguration.Filter("ipathZ", "ipatternZ"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        // we don't provide an API to clear filters, so just create new drift configs and test different excludes
        dc1 = new DriftConfiguration(new Configuration());
        dc2 = new DriftConfiguration(new Configuration());
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check
        dc1.addExclude(new DriftConfiguration.Filter("epathA", "epatternA"));
        dc2.addExclude(new DriftConfiguration.Filter("epathZ", "epatternZ"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
    }

    @Test
    public void getName() {
        String name = "test";
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getName(), name, "Failed to get drift configuration name");
    }

    @Test
    public void getBasedirForFileSystemContext() {
        String basedir = "/opt/drift/test";
        Configuration config = new Configuration();

        PropertyMap map = new PropertyMap(PROP_BASEDIR);
        map.put(new PropertySimple(PROP_BASEDIR_VALUECONTEXT, fileSystem));
        map.put(new PropertySimple(PROP_BASEDIR_VALUENAME, basedir));

        config.put(map);

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getBasedir().getValueName(), basedir,
            "Failed to get drift configuration base directory");
    }

    @Test
    public void getInterval() {
        long interval = 3600L;
        Configuration config = new Configuration();
        config.put(new PropertySimple("interval", interval));

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getInterval(), interval, "Failed to get drift configuration interval");
    }

    @Test
    public void getIncludes() {
        String path1 = "lib";
        String pattern1 = "*.jar";

        String path2 = "conf";
        String pattern2 = "*.xml";

        Configuration config = new Configuration();

        PropertyList includes = new PropertyList("includes");
        includes.add(newInclude(path1, pattern1));
        includes.add(newInclude(path2, pattern2));

        config.put(includes);

        DriftConfiguration driftConfig = new DriftConfiguration(config);
        List<DriftConfiguration.Filter> actual = driftConfig.getIncludes();

        List<DriftConfiguration.Filter> expected = asList(new DriftConfiguration.Filter(path1, pattern1),
            new DriftConfiguration.Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two includes filters");
        assertEquals(actual, expected, "Failed to get drift configuration includes filters");
    }

    @Test
    public void getExcludes() {
        String path1 = "lib";
        String pattern1 = "*.jar";

        String path2 = "conf";
        String pattern2 = "*.xml";

        Configuration config = new Configuration();

        PropertyList excludes = new PropertyList("excludes");
        excludes.add(newExclude(path1, pattern1));
        excludes.add(newExclude(path2, pattern2));

        config.put(excludes);

        DriftConfiguration driftConfig = new DriftConfiguration(config);
        List<DriftConfiguration.Filter> actual = driftConfig.getExcludes();

        List<DriftConfiguration.Filter> expected = asList(new DriftConfiguration.Filter(path1, pattern1),
            new DriftConfiguration.Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two excludes filters");
        assertEquals(actual, expected, "Failed to get drift configuration excludes filters");
    }

    private PropertyMap newInclude(String path, String pattern) {
        return new PropertyMap("include", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }

    private PropertyMap newExclude(String path, String pattern) {
        return new PropertyMap("exclude", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }
}
