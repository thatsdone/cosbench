package com.intel.cosbench.api.obs;

import static com.intel.cosbench.client.obs.ObsConstants.AUTH_PASSWORD_DEFAULT;
import static com.intel.cosbench.client.obs.ObsConstants.AUTH_PASSWORD_KEY;
import static com.intel.cosbench.client.obs.ObsConstants.AUTH_USERNAME_DEFAULT;
import static com.intel.cosbench.client.obs.ObsConstants.AUTH_USERNAME_KEY;
import static com.intel.cosbench.client.obs.ObsConstants.CONN_TIMEOUT_DEFAULT;
import static com.intel.cosbench.client.obs.ObsConstants.CONN_TIMEOUT_KEY;
import static com.intel.cosbench.client.obs.ObsConstants.ENDPOINT_DEFAULT;
import static com.intel.cosbench.client.obs.ObsConstants.ENDPOINT_KEY;
import static com.intel.cosbench.client.obs.ObsConstants.PATH_STYLE_ACCESS_DEFAULT;
import static com.intel.cosbench.client.obs.ObsConstants.PATH_STYLE_ACCESS_KEY;
import static com.intel.cosbench.client.obs.ObsConstants.PROXY_HOST_KEY;
import static com.intel.cosbench.client.obs.ObsConstants.PROXY_PORT_KEY;

import com.obs.services.*;
import com.obs.services.model.*;
import com.obs.services.exception.*;


import java.io.InputStream;

import com.intel.cosbench.api.context.AuthContext;
import com.intel.cosbench.api.context.Context;
import com.intel.cosbench.api.storage.NoneStorage;
import com.intel.cosbench.api.storage.StorageException;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.log.Logger;

public class ObsStorage extends NoneStorage {

	private int timeout;
	private String accessKey;
	private String secretKey;
	private String endpoint;
	private String region;
	
	private ObsClient obsClient = null;

	@Override
	public void init(Config config, Logger logger) {
		super.init(config, logger);

		timeout = config.getInt(CONN_TIMEOUT_KEY, CONN_TIMEOUT_DEFAULT);
		parms.put(CONN_TIMEOUT_KEY, timeout);

		endpoint = config.get(ENDPOINT_KEY, ENDPOINT_DEFAULT);
		accessKey = config.get(AUTH_USERNAME_KEY, AUTH_USERNAME_DEFAULT);
		secretKey = config.get(AUTH_PASSWORD_KEY, AUTH_PASSWORD_DEFAULT);
		boolean pathStyleAccess = config.getBoolean(PATH_STYLE_ACCESS_KEY, PATH_STYLE_ACCESS_DEFAULT);
		String proxyHost = config.get(PROXY_HOST_KEY, "");
		String proxyPort = config.get(PROXY_PORT_KEY, "");
		
		parms.put(ENDPOINT_KEY, endpoint);
		parms.put(AUTH_USERNAME_KEY, accessKey);
		parms.put(AUTH_PASSWORD_KEY, secretKey);
		parms.put(PATH_STYLE_ACCESS_KEY, pathStyleAccess);
		parms.put(PROXY_HOST_KEY, proxyHost);
		parms.put(PROXY_PORT_KEY, proxyPort);
		
		logger.debug("using storage config: {}", parms);

		//FIXME(thatsdone): handle pathstyle/proxy
		
		if ((!proxyHost.equals("")) && (!proxyPort.equals(""))) {
			;
		}
	
		//NOTE(thatsdone): Huawei Cloud OBS requires to set location (region) in addition to endpoint.
		String[] epa = endpoint.split("\\.");
	    if (epa.length >= 3 && epa[0].equals("https://obs") && epa[2].equals("myhuaweicloud")) {
	    	region = epa[1];
	    } else {
	    	logger.error("cosbench-obs: Failed to extract region from endpoint.");
	    }
		
		obsClient = new ObsClient(accessKey, secretKey, endpoint);
		logger.debug("Huawei Cloud OBS client has been initialized");

	}

	@Override
	public void setAuthContext(AuthContext info) {
		super.setAuthContext(info);
	}

	@Override
	public void dispose() {
		super.dispose();
		obsClient = null;
	}

	@Override
	public Context getParms() {
		return super.getParms();
	}

	@Override
	public InputStream getObject(String container, String object, Config config) {
		super.getObject(container, object, config);
		InputStream stream = null;

		ObsObject obsObject = null;
		try {
			obsObject = obsClient.getObject(container, object, null);
		} catch (ObsException obse) {
			throw new StorageException((obse.getErrorMessage()));
		} catch (Exception e) {
			throw new StorageException(e);
		}

		if (obsObject != null) {
			stream = obsObject.getObjectContent();	
		}
		return stream;
	}

	@Override
	public void createContainer(String container, Config config) {
		super.createContainer(container, config);

		//FIXME(thatsdone): to check already existing bucket necessary?
        CreateBucketRequest request = new CreateBucketRequest();
		try {
            request.setBucketName(container);
            //FIXME(thatsdone): ACL, StorageClass, Multi-AZ bucket
			request.setLocation(region); // Loction is Region
            HeaderResponse response = obsClient.createBucket(request);
		} catch (ObsException obse) {
			throw new StorageException((obse.getErrorMessage()));
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void createObject(String container, String object, InputStream data, long length, Config config) {
		super.createObject(container, object, data, length, config);

		//FIXME(thatsdone): length, content-type? ("application/octet-stream") necessary?
        //CreateBucketRequest request = new CreateBucketRequest();
		try {
            HeaderResponse response = obsClient.putObject(object, container, data);
		} catch (ObsException obse) {
			throw new StorageException((obse.getErrorMessage()));
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void deleteContainer(String container, Config config) {
		super.deleteContainer(container, config);

		try {
            HeaderResponse response = obsClient.deleteBucket(container);
		} catch (ObsException obse) {
			throw new StorageException((obse.getErrorMessage()));
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void deleteObject(String container, String object, Config config) {
		super.deleteObject(container, object, config);

		try {
            HeaderResponse response = obsClient.deleteObject(container, object, null);
		} catch (ObsException obse) {
			throw new StorageException((obse.getErrorMessage()));
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}
}
