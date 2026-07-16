package com.plateapp.plate_main.mypage.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.mypage.config.MyHubProperties;
import com.plateapp.plate_main.mypage.dto.MyHubResponse;
import org.springframework.stereotype.Service;

@Service
public class MyHubService {

    private final MyHubProperties properties;
    private final MyHubSnapshotReader snapshotReader;

    public MyHubService(MyHubProperties properties, MyHubSnapshotReader snapshotReader) {
        this.properties = properties;
        this.snapshotReader = snapshotReader;
    }

    /**
     * The rollout guard intentionally runs before entering the transactional
     * reader, so a disabled route cannot acquire a database connection.
     */
    public MyHubResponse getHub(String username, int previewLimit) {
        if (!properties.isEnabled() || !properties.isImageVisibilityReady()) {
            throw new AppException(ErrorCode.MY_HUB_FEATURE_DISABLED);
        }
        return snapshotReader.read(username, previewLimit);
    }
}
