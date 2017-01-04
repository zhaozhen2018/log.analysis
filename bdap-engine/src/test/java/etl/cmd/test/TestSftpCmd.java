package etl.cmd.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.hadoop.fs.Path;
//log4j2
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import bdap.util.HdfsUtil;
import bdap.util.SftpInfo;
import bdap.util.SftpUtil;
import etl.cmd.SftpCmd;
import etl.engine.EngineUtil;
import etl.util.StringUtil;

public class TestSftpCmd extends TestETLCmd {
	public static final Logger logger = LogManager.getLogger(TestSftpCmd.class);
	
	@Before
	public void beforeMethod() {
		org.junit.Assume.assumeTrue(super.isTestSftp());
	}
	
	public String getResourceSubFolder(){
		return "sftp"+File.separator;
	}
	
	@Test
	public void test1() throws Exception {
		String cfg = "sftp_test.properties";
		// values in the static cfg
		String fileName = "backup_test1_data";
		
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		String ftpFolder = cmd.getFromDirs()[0];
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		
		cmd.sgProcess();
		// check incoming fodler
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertTrue(fl.contains(fileName));
		// check remote dir
		fl = SftpUtil.sftpList(sftpInfo, ftpFolder);
		assertFalse(fl.contains(fileName));
	}

	public void testSftpSessionConnectionFailure() throws Exception {
		logger.info("Testing sftpcmd common failure test case");
		String fileName = "backup_test1_data";
		String cfg = "sftp_test.properties";
		
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		cmd.setHost("192.168.12.13");
		cmd.setSftpConnectRetryWait(5*1000);
		cmd.setSftpConnectRetryCount(2);
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		cmd.sgProcess();
		// check incoming fodler
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertFalse(fl.contains(fileName));
	}
	
	@Test
	public void testDeleteFileNotEnabledFun() throws Exception{
		logger.info("Testing sftpcmd delete not enabled test case");

		String cfg = "sftp_test.properties";
		// values in the static cfg
		String fileName = "backup_test1_data";
		
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		String ftpFolder = cmd.getFromDirs()[0];
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		cmd.setSftpClean(false);
		cmd.sgProcess();
		//
		// check incoming folder
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertTrue(fl.contains(fileName));
		// check remote dir
		fl = SftpUtil.sftpList(sftpInfo, ftpFolder);
		assertTrue(fl.contains(fileName));
	}
	
	@Test
	public void testFileFilter() throws Exception {
		String cfg = "sftp_selectfile.properties";
		
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		String ftpFolder = cmd.getFromDirs()[0];
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		cmd.sgProcess();
		
		// check incoming fodler
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		assertFalse(fl.contains("RTDB_ACCESS.friday"));
		assertTrue(fl.contains("RTDB_ACCESS.monday"));
	}
	
	@Test
	public void testMultiFolders() throws Exception{
		String cfg = "sftp_multiple_dirs.properties";
		String fileName0 = "RTDB_ACCESS.friday";
		String fileName1 = "RTDB_ACCESS.monday";
		
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		String ftpFolder = StringUtil.commonPath(cmd.getFromDirs());
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		logger.info(fl);
		assertTrue(fl.contains(fileName0));
		assertTrue(fl.contains(fileName1));
	}
	
	@Test
	public void testLimitFiles() throws Exception{
		String cfg = "sftp_limit.properties";
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		
		String dfsIncomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(dfsIncomingFolder), true);
		getFs().mkdirs(new Path(dfsIncomingFolder));
		String ftpFolder = cmd.getFromDirs()[0];
		
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(getFs(), dfsIncomingFolder);
		logger.info(fl);
		assertTrue(fl.size()==1);
	}
	
	//you need to manually mkdir cmd.getFromDirs
	@Test
	public void fileNamesOnly() throws Exception{
		String cfg = "sftp_filenames.properties";
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		String dfsIncomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(dfsIncomingFolder), true);
		getFs().mkdirs(new Path(dfsIncomingFolder));
		String ftpFolder = cmd.getFromDirs()[0];
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(cmd.getFs(), dfsIncomingFolder);
		String file = fl.get(0);//file name is map key
		logger.info(fl);
		assertTrue(fl.size()==1);
		assertTrue(fl.get(0).equals("0"));
		
		List<String> contents = HdfsUtil.stringsFromDfsFile(cmd.getFs(), dfsIncomingFolder+file);
		logger.info(String.format("fileNamesOnly contents:\n%s", String.join("\n", contents)));
		assertTrue(contents.size()==3);
	}
	
	@Test
	public void testRecursive() throws Exception{
		String cfg = "sftp_recursive.properties";
		String fileName0 = "RTDB_ACCESS.friday";
		String fileName1 = "RTDB_ACCESS.monday";
		String fileName2 = "source1";
		String fileName3 = "source2";
		
		SftpCmd cmd = new SftpCmd("wf1", null, this.getResourceSubFolder() + cfg, getDefaultFS(), null);
		String ftpFolder = StringUtil.commonPath(cmd.getFromDirs());
		String incomingFolder = cmd.getIncomingFolder();
		getFs().delete(new Path(incomingFolder), true);
		getFs().mkdirs(new Path(incomingFolder));
		
		SftpInfo sftpInfo = EngineUtil.getInstance().getSftpInfo();
		SftpUtil.sftpFromLocal(sftpInfo, super.getLocalFolder()+"data", ftpFolder);
		
		List<String> ret = cmd.sgProcess();
		logger.info(ret);
		
		//assertion
		List<String> fl = HdfsUtil.listDfsFile(getFs(), incomingFolder);
		logger.info(String.format("testRecursive fl:\n%s", String.join("\n", fl)));
		assertTrue(fl.size() == 4);
		assertTrue(fl.contains(fileName0) || fl.contains(fileName1));
		assertTrue(fl.contains(fileName2) || fl.contains(fileName3));
	}
}
