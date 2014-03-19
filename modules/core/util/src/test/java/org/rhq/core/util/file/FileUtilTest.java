/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.util.file;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.io.FileUtils.touch;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;

@Test
public class FileUtilTest {
    public void testCopyDirectory() throws Exception {
        try {
            FileUtil.copyDirectory(new File("this.does.not.exist"), new File("dummy"));
            assert false : "the source directory did not exist, this should have failed because of that";
        } catch (Exception ok) {
        }

        // create a source directory and a destination directory. Make sure we start off
        // with a non-existent destination directory - we want the copyDirectory to create it for us.
        File outDir = FileUtil.createTempDirectory("fileUtilTestCopyDir", ".dest", null);
        assert outDir.delete() : "failed to start out with a non-existent dest directory";
        assert !outDir.exists() : "dest directory should not exist"; // yes, I am paranoid

        File inDir = FileUtil.createTempDirectory("fileUtilTestCopyDir", ".src", null);
        try {
            // create some test files in our source directory
            String testFilename0 = "file0.txt";
            String testFilename1 = "subdir" + File.separatorChar + "subfile1.txt";
            String testFilename2 = "subdir" + File.separatorChar + "subfile2.txt";

            File testFile = new File(inDir, testFilename0);
            StreamUtil.copy(new ByteArrayInputStream("0".getBytes()), new FileOutputStream(testFile));
            assert "0".equals(new String(StreamUtil.slurp(new FileInputStream(testFile)))); // sanity check, make sure its there

            testFile = new File(inDir, testFilename1);
            testFile.getParentFile().mkdirs();
            StreamUtil.copy(new ByteArrayInputStream("1".getBytes()), new FileOutputStream(testFile));
            assert "1".equals(new String(StreamUtil.slurp(new FileInputStream(testFile)))); // sanity check, make sure its there
            testFile = new File(inDir, testFilename2);
            testFile.getParentFile().mkdirs();
            StreamUtil.copy(new ByteArrayInputStream("2".getBytes()), new FileOutputStream(testFile));
            assert "2".equals(new String(StreamUtil.slurp(new FileInputStream(testFile)))); // sanity check, make sure its there

            // copy our source directory and confirm the copies are correct
            FileUtil.copyDirectory(inDir, outDir);

            testFile = new File(outDir, testFilename0);
            assert testFile.exists() : "file did not get created: " + testFile;
            assert "0".equals(new String(StreamUtil.slurp(new FileInputStream(testFile))));
            testFile = new File(outDir, testFilename1);
            assert testFile.exists() : "file did not get created: " + testFile;
            assert "1".equals(new String(StreamUtil.slurp(new FileInputStream(testFile))));
            testFile = new File(outDir, testFilename2);
            assert testFile.exists() : "file did not get created: " + testFile;
            assert "2".equals(new String(StreamUtil.slurp(new FileInputStream(testFile))));

            // let's test getDirectoryFiles while we are here
            List<File> outFiles = FileUtil.getDirectoryFiles(outDir);
            assert outFiles != null : outFiles;
            assert outFiles.size() == 3 : outFiles;
            assert outFiles.contains(new File(testFilename0)) : outFiles;
            assert outFiles.contains(new File(testFilename1)) : outFiles;
            assert outFiles.contains(new File(testFilename2)) : outFiles;
        } finally {
            // clean up our test
            try {
                FileUtil.purge(inDir, true);
            } catch (Exception ignore) {
            }
            try {
                FileUtil.purge(outDir, true);
            } catch (Exception ignore) {
            }
        }
    }

    public void testStripDriveLetter() {
        StringBuilder str;

        str = new StringBuilder("");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("");

        str = new StringBuilder("\\");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("\\");

        str = new StringBuilder("foo");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("foo");

        str = new StringBuilder("foo\\bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("foo\\bar");

        str = new StringBuilder("\\foo\\bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("\\foo\\bar");

        str = new StringBuilder("C:");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("");

        str = new StringBuilder("C:\\");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("\\");

        str = new StringBuilder("C:foo");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("foo");

        str = new StringBuilder("C:foo\\bar");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("foo\\bar");

        str = new StringBuilder("C:\\foo");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("\\foo");

        str = new StringBuilder("C:\\foo\\bar");
        assert FileUtil.stripDriveLetter(str).equals("C");
        assert str.toString().equals("\\foo\\bar");

        // test all the valid drive letters
        String driveLetters = "abcdefghijklmnopqrstuvwxyz";
        String testPath = "\\foo\\bar";
        for (int i = 0; i < driveLetters.length(); i++) {
            String lowerLetter = String.valueOf(driveLetters.charAt(i));
            String upperLetter = String.valueOf(Character.toUpperCase(driveLetters.charAt(i)));
            StringBuilder lowerPath = new StringBuilder(lowerLetter + ':' + testPath);
            StringBuilder upperPath = new StringBuilder(upperLetter + ':' + testPath);

            assert FileUtil.stripDriveLetter(lowerPath).equals(lowerLetter);
            assert lowerPath.toString().equals(testPath);
            assert FileUtil.stripDriveLetter(upperPath).equals(upperLetter);
            assert upperPath.toString().equals(testPath);
        }

        // unix paths should not be affected
        str = new StringBuilder("/");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("/");

        str = new StringBuilder("/foo");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("/foo");

        str = new StringBuilder("foo/bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("foo/bar");

        str = new StringBuilder("/foo/bar");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("/foo/bar");

        str = new StringBuilder("hello:world/hello");
        assert FileUtil.stripDriveLetter(str) == null;
        assert str.toString().equals("hello:world/hello");
    }

    @Test
    public void visitEachFileInDir() throws Exception {
        File dir = new File(toFile(getClass().getResource(".")), "visit-each-file-in-dir");
        deleteDirectory(dir);
        dir.mkdirs();

        File file1 = new File(dir, "file-1");
        touch(file1);
        File file2 = new File(dir, "file-2");
        touch(file2);

        List<File> expectedFiles = asList(file1, file2);
        final List<File> actualFiles = new ArrayList<File>();

        FileUtil.forEachFile(dir, new FileVisitor() {
            @Override
            public void visit(File file) {
                actualFiles.add(file);
            }
        });

        assertCollectionEqualsNoOrder(expectedFiles, actualFiles, "Expected to visit all files in directory");
    }

    @Test
    public void visitFilesInSubdirectories() throws Exception {
        File dir = new File(toFile(getClass().getResource(".")), "visit-files-in-sub-dirs");
        deleteDirectory(dir);
        dir.mkdirs();

        File file1 = new File(dir, "file-1");
        touch(file1);

        File subdir1 = new File(dir, "subdir-1");
        subdir1.mkdir();

        File file2 = new File(subdir1, "file-2");
        touch(file2);

        File subdir2 = new File(dir, "subdir-2");

        File file3 = new File(subdir2, "file-3");
        touch(file3);

        List<File> expectedFiles = asList(file1, file2, file3);
        final List<File> actualFiles = new ArrayList<File>();

        FileUtil.forEachFile(dir, new FileVisitor() {
            @Override
            public void visit(File file) {
                actualFiles.add(file);
            }
        });

        assertCollectionEqualsNoOrder(expectedFiles, actualFiles, "Expected to visit files in sub directories");
    }

    @Test
    public void visitFilesInNestedSubDirectories() throws Exception {
        File dir = new File(toFile(getClass().getResource(".")), "visit-files-in-nested-sub-dirs");
        deleteDirectory(dir);
        dir.mkdirs();

        File file1 = new File(dir, "file-1");
        touch(file1);

        File subdir1 = new File(dir, "subdir-1");
        subdir1.mkdir();

        File file2 = new File(subdir1, "file-2");
        touch(file2);

        File subdir2 = new File(subdir1, "subdir-2");

        File file3 = new File(subdir2, "file-3");
        touch(file3);

        List<File> expectedFiles = asList(file1, file2, file3);
        final List<File> actualFiles = new ArrayList<File>();

        FileUtil.forEachFile(dir, new FileVisitor() {
            @Override
            public void visit(File file) {
                actualFiles.add(file);
            }
        });

        assertCollectionEqualsNoOrder(expectedFiles, actualFiles, "Expected to visit files in nested sub directories");
    }

    public void testGetPattern() {
        Pattern regex;

        regex = assertPatternsRegex("(" + translateAbsoluteUnixPathToActualAsRegex("/basedir/(test1\\.txt)") + ")",
            new PathFilter("/basedir", "test1.txt"));

        assert regex.matcher(translateAbsoluteUnixPathToActual("/basedir/test1.txt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/test2.txt")).matches();

        regex = assertPatternsRegex("(" + translateAbsoluteUnixPathToActualAsRegex("/basedir/easy\\.txt") + ")|(" +
            translateAbsoluteUnixPathToActualAsRegex("/basedir/test\\.txt") + ")", new PathFilter("/basedir/easy.txt",
            null), new PathFilter("/basedir/test.txt", null));

        assert regex.matcher(translateAbsoluteUnixPathToActual("/basedir/easy.txt")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/basedir/test.txt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/easyXtxt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/testXtxt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/easy.txtX")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/test.txtX")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedirX/easy.txt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedirX/test.txt")).matches();
        assert !regex.matcher("easy.txt").matches() : "missing basedir";
        assert !regex.matcher("test.txt").matches() : "missing basedir";

        regex = assertPatternsRegex("(" + translateAbsoluteUnixPathToActualAsRegex("/basedir/([^/]*\\.txt)") + ")",
            new PathFilter("/basedir", "*.txt"));

        assert regex.matcher(translateAbsoluteUnixPathToActual("/basedir/foo.txt")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/basedir/file with spaces.txt")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/basedir/123.txt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/subdir/foo.txt")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/basedir/foo.txt.swp")).matches();

        regex = assertPatternsRegex("(" + translateAbsoluteUnixPathToActualAsRegex("/var/lib/([^/]*\\.war)") + ")|(" +
            translateAbsoluteUnixPathToActualAsRegex("/var/lib/([^/]*\\.ear)") + ")", new PathFilter("/var/lib",
            "*.war"), new PathFilter("/var/lib", "*.ear"));

        assert regex.matcher(translateAbsoluteUnixPathToActual("/var/lib/myapp.war")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/var/lib/myapp.ear")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/var/lib/my-app.war")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/var/lib/myapp.War")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/var/libs/myapp.war")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("myapp.ear")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/var/lib/myapp.ear.rej")).matches();

        regex = assertPatternsRegex("(" + translateAbsoluteUnixPathToActualAsRegex("/conf/(server-.\\.conf)") + ")",
            new PathFilter("/conf", "server-?.conf"));

        assert regex.matcher(translateAbsoluteUnixPathToActual("/conf/server-1.conf")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/conf/server-X.conf")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/conf/subconf/server-1.conf")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/conf/server.conf")).matches();

        regex = assertPatternsRegex("(" + translateAbsoluteUnixPathToActualAsRegex("/etc/(.*[^/]*\\.conf)") + ")",
            new PathFilter("/etc", "**/*.conf"));

        assert regex.matcher(translateAbsoluteUnixPathToActual("/etc/yum.conf")).matches();
        assert regex.matcher(translateAbsoluteUnixPathToActual("/etc/httpd/httpd.conf")).matches();
        assert !regex.matcher(translateAbsoluteUnixPathToActual("/etc/foo.conf/foo")).matches();
    }

    public void testNormalizePath() throws Exception {
        if (File.separatorChar == '\\') {
            //windows
            checkNormalization("\\\\server\\share\\bar", "\\\\server\\share\\path\\..\\bar");
            //we just consider the ".." the name of the share of the UNC path
            checkNormalization("\\\\server\\..\\bar", "\\\\server\\..\\bar");
            checkNormalization(null, "\\\\server\\share\\..\\bar");
            checkNormalization("C:\\bar", "C:\\foo\\..\\bar");
            checkNormalization(null, "C:\\..\\bar");

            checkNormalization("\\foo", "/foo//");
            checkNormalization("\\foo", "/foo/./");
            checkNormalization("\\bar", "/foo/../bar");
            checkNormalization("\\bar", "/foo/../bar/");
            checkNormalization("\\baz", "/foo/../bar/../baz");
            //we just consider the "." the name of the share of the UNC path
            checkNormalization("\\\\foo\\.\\bar", "//foo//./bar");
            checkNormalization(null, "/../");
            checkNormalization(null, "../foo");
            checkNormalization("foo", "foo/bar/..");
            checkNormalization(null, "foo/../../bar");
            checkNormalization("bar", "foo/../bar");
        } else {
            checkNormalization("/foo", "/foo//");
            checkNormalization("/foo", "/foo/./");
            checkNormalization("/bar", "/foo/../bar");
            checkNormalization("/bar", "/foo/../bar/");
            checkNormalization("/baz", "/foo/../bar/../baz");
            checkNormalization("/foo/bar", "//foo//./bar");
            checkNormalization(null, "/../");
            checkNormalization(null, "../foo");
            checkNormalization("foo", "foo/bar/..");
            checkNormalization(null, "foo/../../bar");
            checkNormalization("bar", "foo/../bar");
            checkNormalization("~/bar", "~/foo/../bar/");
        }
    }

    private void checkNormalization(String expectedResult, String path) {
        File result = FileUtil.normalizePath(new File(path));
        assert
            expectedResult == null ? result == null : result != null && expectedResult.equals(result.getPath()) :
            expectedResult + " failed. Should have been [" + expectedResult + "] but was [" + result + "]";
    }

    private Pattern assertPatternsRegex(String expectedPattern, PathFilter... filters) {
        Pattern regex = FileUtil.generateRegex(asList(filters));

        assert regex != null : "The regex was not able to be produced - it was null";
        assert expectedPattern.equals(regex.pattern()) : "The expected pattern [" + expectedPattern
            + "] did not match the actual pattern [" + regex.pattern() + "]";

        return regex;
    }

    private static String translateAbsoluteUnixPathToActualAsRegex(String path) {
        return translateAbsoluteUnixPathToActual(path, true);
    }

    private static String translateAbsoluteUnixPathToActual(String path) {
        return translateAbsoluteUnixPathToActual(path, false);
    }

    private static String translateAbsoluteUnixPathToActual(String path, boolean asRegex) {
        if (File.separatorChar == '\\') {
            //get the current drive letter
            //leave out the trailing "\" - we have an absolute unix path on input, so we "use" the "/" of it
            String driveLetter = new File(".").getAbsoluteFile().toPath().getRoot().toString().substring(0, 2);

            path = driveLetter + path.replace("/", asRegex? "\\\\" : "\\");
        }

        return path;
    }
}
