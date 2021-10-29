package com.intel.cosbench.api.obs;

import com.intel.cosbench.api.storage.StorageAPI;
import com.intel.cosbench.api.storage.StorageAPIFactory;

public class ObsStorageFactory implements StorageAPIFactory {

	@Override
	public String getStorageName() {
		return "obs";
	}

	@Override
	public StorageAPI getStorageAPI() {
		return new ObsStorage();
	}

}
