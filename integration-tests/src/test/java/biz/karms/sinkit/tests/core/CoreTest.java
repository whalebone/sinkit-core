package biz.karms.sinkit.tests.core;

import biz.karms.sinkit.ejb.ArchiveServiceEJB;
import biz.karms.sinkit.ejb.CoreServiceEJB;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.tests.util.IoCFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static org.testng.Assert.*;


/**
 * Created by tkozel on 29.8.15.
 */
public class CoreTest /*extends Arquillian*/ {

//    private static final Logger LOGGER = Logger.getLogger(CoreTest.class.getName());
//    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");
//
//    @Deployment(name = "ear", testable = true)
//    public static Archive<?> createTestArchive() {
//        EnterpriseArchive ear = ShrinkWrap.create(ZipImporter.class, "sinkit-ear2.ear").importFrom(new File("../ear/target/sinkit-ear.ear")).as(EnterpriseArchive.class);
//        ear.getAsType(JavaArchive.class, "sinkit-ejb.jar").addClass(CoreTest.class).addClass(IoCFactory.class);
//        return ear;
//    }
//
//    @Inject
//    CoreServiceEJB coreService;
//    @Inject
//    ArchiveServiceEJB archiveService;
//
}