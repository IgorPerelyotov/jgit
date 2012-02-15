import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
		write(new File(db.getWorkTree(), "test.txt"), "test");
		File folder = new File(db.getWorkTree(), "folder");
		OutputStream out = new ByteArrayOutputStream();
		List<DiffEntry> entries = git.diff().setOutputStream(out).call();

		String actual = out.toString();
		String expected = "diff --git a/folder/folder.txt b/folder/folder.txt\n"
				+ "index 0119635..95c4c65 100644\n"
				+ "--- a/folder/folder.txt\n"
				+ "+++ b/folder/folder.txt\n"
				+ "@@ -1 +1 @@\n"
				+ "-folder\n"
				+ "\\ No newline at end of file\n"
				+ "+folder change\n"
				+ "\\ No newline at end of file\n";
		assertEquals(expected.toString(), actual);
		write(new File(db.getWorkTree(), "test.txt"), "test");
		File folder = new File(db.getWorkTree(), "folder");
		OutputStream out = new ByteArrayOutputStream();
		List<DiffEntry> entries = git.diff().setOutputStream(out)
				.setCached(true).call();

		String actual = out.toString();
		String expected = "diff --git a/folder/folder.txt b/folder/folder.txt\n"
				+ "new file mode 100644\n"
				+ "index 0000000..0119635\n"
				+ "--- /dev/null\n"
				+ "+++ b/folder/folder.txt\n"
				+ "@@ -0,0 +1 @@\n"
				+ "+folder\n"
				+ "\\ No newline at end of file\n";
		assertEquals(expected.toString(), actual);
	}

	@Test
	public void testDiffTwoCommits() throws Exception {
		write(new File(db.getWorkTree(), "test.txt"), "test");
		File folder = new File(db.getWorkTree(), "folder");
		folder.mkdir();
		write(new File(folder, "folder.txt"), "folder");
		Git git = new Git(db);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();
		write(new File(folder, "folder.txt"), "folder change");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("second commit").call();
		write(new File(folder, "folder.txt"), "second folder change");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("third commit").call();

		// bad filter
		DiffCommand diff = git.diff().setShowNameAndStatusOnly(true)
				.setPathFilter(PathFilter.create("test.txt"))
				.setOldTree(getTreeIterator("HEAD^^"))
				.setNewTree(getTreeIterator("HEAD^"));
		List<DiffEntry> entries = diff.call();
		assertEquals(0, entries.size());

		// no filter, two commits
		OutputStream out = new ByteArrayOutputStream();
		diff = git.diff().setOutputStream(out)
				.setOldTree(getTreeIterator("HEAD^^"))
				.setNewTree(getTreeIterator("HEAD^"));
		entries = diff.call();
		assertEquals(1, entries.size());
		assertEquals(ChangeType.MODIFY, entries.get(0).getChangeType());
		assertEquals("folder/folder.txt", entries.get(0).getOldPath());
		assertEquals("folder/folder.txt", entries.get(0).getNewPath());

		String actual = out.toString();
		String expected = "diff --git a/folder/folder.txt b/folder/folder.txt\n"
				+ "index 0119635..95c4c65 100644\n"
				+ "--- a/folder/folder.txt\n"
				+ "+++ b/folder/folder.txt\n"
				+ "@@ -1 +1 @@\n"
				+ "-folder\n"
				+ "\\ No newline at end of file\n"
				+ "+folder change\n"
				+ "\\ No newline at end of file\n";
		assertEquals(expected.toString(), actual);
	}

	@Test
	public void testDiffWithPrefixes() throws Exception {
		write(new File(db.getWorkTree(), "test.txt"), "test");
		Git git = new Git(db);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();
		write(new File(db.getWorkTree(), "test.txt"), "test change");

		OutputStream out = new ByteArrayOutputStream();
		git.diff().setOutputStream(out).setSourcePrefix("old/")
				.setDestinationPrefix("new/")
				.call();

		String actual = out.toString();
		String expected = "diff --git old/test.txt new/test.txt\n"
				+ "index 30d74d2..4dba797 100644\n" + "--- old/test.txt\n"
				+ "+++ new/test.txt\n" + "@@ -1 +1 @@\n" + "-test\n"
				+ "\\ No newline at end of file\n" + "+test change\n"
				+ "\\ No newline at end of file\n";
		assertEquals(expected.toString(), actual);
	}

	@Test
	public void testDiffWithNegativeLineCount() throws Exception {
		write(new File(db.getWorkTree(), "test.txt"),
				"0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
		Git git = new Git(db);
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();
		write(new File(db.getWorkTree(), "test.txt"),
				"0\n1\n2\n3\n4a\n5\n6\n7\n8\n9");

		OutputStream out = new ByteArrayOutputStream();
		git.diff().setOutputStream(out).setContextLines(1)
				.call();

		String actual = out.toString();
		String expected = "diff --git a/test.txt b/test.txt\n"
				+ "index f55b5c9..c5ec8fd 100644\n" + "--- a/test.txt\n"
				+ "+++ b/test.txt\n" + "@@ -4,3 +4,3 @@\n" + " 3\n" + "-4\n"
				+ "+4a\n" + " 5\n";
		assertEquals(expected.toString(), actual);
	}

	private AbstractTreeIterator getTreeIterator(String name)
			throws IOException {
		final ObjectId id = db.resolve(name);
		if (id == null)
			throw new IllegalArgumentException(name);
		final CanonicalTreeParser p = new CanonicalTreeParser();
		final ObjectReader or = db.newObjectReader();
		try {
			p.reset(or, new RevWalk(db).parseTree(id));
			return p;
		} finally {
			or.release();
		}