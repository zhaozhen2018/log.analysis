package etl.cmd.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.Logger;
import org.junit.Test;

import etl.cmd.LoadDataCmd;
import etl.util.DBUtil;
import etl.util.Util;

public class TestLoadDatabaseCmd extends TestETLCmd {
	public static final Logger logger = Logger.getLogger(TestLoadDatabaseCmd.class);

	private void test1Fun() throws Exception {
		BufferedReader br = null;
		try {
			//
			String dfsFolder = "/pde/etlcfg/";
			String staticCfgName = "pde.loadcsv1.properties";
			String csvFileName = "part-loaddatabase";
			String csvFolder = "/pde/loadcsv/";
			PropertiesConfiguration pc = Util.getPropertiesConfigFromDfs(getFs(), dfsFolder + staticCfgName);
			String wfid = "";
			int linesInFile = 0;
			String line = null;
			List<String> numberOfRowsupdated;

			getFs().delete(new Path(dfsFolder), true);
			getFs().delete(new Path(csvFolder), true);
			getFs().mkdirs(new Path(dfsFolder));
			getFs().mkdirs(new Path(csvFolder));
			getFs().copyFromLocalFile(new Path(getLocalFolder() + staticCfgName), new Path(dfsFolder + staticCfgName));
			getFs().copyFromLocalFile(new Path(getLocalFolder() + csvFileName), new Path(csvFolder + csvFileName));

			// run cmd
			LoadDataCmd cmd = new LoadDataCmd(wfid, dfsFolder + staticCfgName, null, getDefaultFS());
			numberOfRowsupdated = cmd.sgProcess();
			// fetch db data
			List<String> dbData = DBUtil.checkCsv("select * from lsl_sample", pc, 0, 0, ",");
			logger.info(dbData);
			// check number of lines in file same as rowsupdated, db
			// data against each line
			br = new BufferedReader(new InputStreamReader(getFs().open(new Path(csvFolder + csvFileName))));
			while ((line = br.readLine()) != null) {
				linesInFile++;
				logger.info(line);
				assertTrue(dbData.contains(line.trim()));
			}

			assertTrue(Integer.parseInt(numberOfRowsupdated.get(0)) == linesInFile);
			assertTrue(dbData.size() > linesInFile);
			logger.info(linesInFile);
		} catch (Exception e) {
			logger.error("Exception occured due to invalid data-history path", e);
		} finally {
			if (br != null)
				br.close();
		}
	}
	
	@Test
	public void test1() throws Exception{
		if (getDefaultFS().contains("127.0.0.1")){
			test1Fun();
		}else{
			UserGroupInformation ugi = UserGroupInformation.createProxyUser("dbadmin", UserGroupInformation.getLoginUser());
			ugi.doAs(new PrivilegedExceptionAction<Void>() {
				public Void run() throws Exception {
					test1Fun();
					return null;
				}
			});
		}
	}
}
