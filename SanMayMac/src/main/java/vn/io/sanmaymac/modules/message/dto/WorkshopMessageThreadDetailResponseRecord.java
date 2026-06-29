package vn.io.sanmaymac.modules.message.dto;

import java.util.List;

public record WorkshopMessageThreadDetailResponseRecord(
        WorkshopMessageThreadResponseRecord thread,
        List<WorkshopMessageResponseRecord> messages) {
}