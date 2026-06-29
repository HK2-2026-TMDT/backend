package vn.io.sanmaymac.modules.bidding.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.io.sanmaymac.common.service.MediaStorageService;

@Service
public class BiddingDesignStorageService {
    private final MediaStorageService mediaStorageService;

    public BiddingDesignStorageService(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    public String store(MultipartFile file, String prefix) {
        return mediaStorageService.store(file, prefix);
    }
}