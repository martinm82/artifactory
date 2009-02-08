package org.artifactory.cli.test;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.cli.main.ArtifactoryCli;
import org.artifactory.cli.main.CliOption;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;

/**
 * A test class for the renewed CLI
 *
 * @author Noam Tenne
 */
public class TestNewCli {

    @BeforeClass
    public void dontExit() {
        ArtifactoryCli.DO_SYSTEM_EXIT = false;
    }

    @Test
    public void test() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{"help"});
        System.out.println("-----------------------------------------------------");
        cleanOptions();
        ArtifactoryCli.main(new String[]{"help", "import"});
        System.out.println("-----------------------------------------------------");
        cleanOptions();
        ArtifactoryCli.main(new String[]{"import"});
        System.out.println("-----------------------------------------------------");
        cleanOptions();
        ArtifactoryCli.main(new String[]{"help", "dump"});
        System.out.println("-----------------------------------------------------");
    }


    @Test
    public void testError() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "info",
                "--url", TestCli.API_ROOT,
                "--username",
                "--password", "password"
        });
    }

    @Test
    public void testUnknownOption() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "info",
                "--url", TestCli.API_ROOT,
                "--ssss"
        });
    }

    /**
     * Try to run the security command with both --destFile and --update flags. Should result in a warning printed to
     * System.err
     *
     * @throws Exception Exception
     */
    @Test
    public void testSecurityBothFlags() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "security",
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password",
                "--destFile", "fake path",
                "--update", "fake path"
        });
    }

    /**
     * Use the security command to try and print the security file to System.out
     *
     * @throws Exception Exception
     */
    @Test
    public void testSecurityDump() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "security",
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password"
        });
    }

    /**
     * Use the security command to save the security file locally
     *
     * @throws Exception Exception
     */
    @Test
    public void testSecuritySaveFile() throws Exception {
        testConfSaveFile("security");
    }

    /**
     * Use the security command to update the security file with a local version
     *
     * @throws Exception Exception
     */
    @Test
    public void testSecurityUpdate() throws Exception {
        String command = "security";
        testConfUpdate(command, command, command + ".orig");
    }

    @Test
    public void testConfigurationDump() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "configuration",
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password"
        });
    }

    @Test
    public void testConfigurationSaveFile() throws Exception {
        testConfSaveFile("configuration");
    }

    @Test
    public void testConfigurationUpdate() throws Exception {
        String fileName = "artifactory.config";
        testConfUpdate("configuration", fileName, fileName + ".orig");
    }

    private void testConfSaveFile(String command) {
        cleanOptions();
        File file = saveNewFile(command);
        long fileLastModified = file.lastModified();

        ArtifactoryCli.main(new String[]{
                command,
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password",
                "--destFile", file.getAbsolutePath()
        });
        cleanOptions();
        Assert.assertEquals(fileLastModified, file.lastModified(),
                "File should have not been modified without the --overwrite flag.");

        ArtifactoryCli.main(new String[]{
                command,
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password",
                "--destFile", file.getAbsolutePath(),
                "--overwrite"
        });

        Assert.assertNotSame(fileLastModified, file.lastModified(),
                "File should have been modified with the --overwrite flag.");
    }

    private void testConfUpdate(String command, String replacementFileName, String originalFileName)
            throws FileNotFoundException {
        cleanOptions();
        uploadFile(command, replacementFileName);
        File tempFile = saveNewFile(command);
        if ("security".equals(command)) {
            testSecurityFile(tempFile);
        } else if ("configuration".equals(command)) {
            testConfigurationFile(tempFile);
        }
        uploadFile(command, originalFileName);
    }

    private File saveNewFile(String command) {
        File file = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + ".xml");
        FileUtils.deleteQuietly(file);
        Assert.assertFalse(file.exists(), "New temp configuration file must not exist before the test has begun");
        ArtifactoryCli.main(new String[]{
                command,
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password",
                "--destFile", file.getAbsolutePath()
        });
        cleanOptions();
        Assert.assertNotNull(file, "Configuration file must be created.");
        Assert.assertTrue(file.exists(), "Configuration file must exist.");
        Assert.assertTrue(file.length() > 0, "Configuration file cannot be empty.");
        return file;
    }

    private void uploadFile(String command, String fileName) {
        URL templateURL = TestNewCli.class.getResource("/cli/" + fileName + ".xml");
        File templateFile = new File(templateURL.getFile());
        Assert.assertNotNull(templateFile);
        Assert.assertTrue(templateFile.exists());
        Assert.assertTrue(templateFile.length() > 0);
        ArtifactoryCli.main(new String[]{
                command,
                "--url", TestCli.API_ROOT,
                "--username", "admin",
                "--password", "password",
                "--update", templateFile.getAbsolutePath()
        });
        cleanOptions();
    }

    private void testSecurityFile(File tempFile) throws FileNotFoundException {
        XStream xStream = new XStream();
        xStream.processAnnotations(SecurityInfo.class);
        FileInputStream inputStream = new FileInputStream(tempFile);
        SecurityInfo securityInfo = (SecurityInfo) xStream.fromXML(inputStream);
        List<UserInfo> users = securityInfo.getUsers();
        boolean foundCLIUser = false;
        for (UserInfo user : users) {
            if ("clitest".equals(user.getUsername())) {
                foundCLIUser = true;
            }
        }

        Assert.assertTrue(foundCLIUser);
    }

    private void testConfigurationFile(File tempFile) {
        CentralConfigDescriptor centralConfigDescriptor = JaxbHelper.readConfig(tempFile);
        List<BackupDescriptor> backups = centralConfigDescriptor.getBackups();
        boolean foundCLIBackup = false;
        for (BackupDescriptor backup : backups) {
            if ("clibackup".equals(backup.getKey())) {
                foundCLIBackup = true;
            }
        }

        Assert.assertTrue(foundCLIBackup);
    }

    private static void cleanOptions() {
        CliOption[] cliOptions = CliOption.values();
        for (CliOption option : cliOptions) {
            option.setValue(null);
        }
    }
}