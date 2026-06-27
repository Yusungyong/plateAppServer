package com.plateapp.plate_main.admin.storeapproval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "store_application_change_request_items")
@Getter
public class StoreApplicationChangeRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Column(nullable = false, length = 120)
    private String field;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(name = "reason_code", nullable = false, length = 80)
    private String reasonCode;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "edit_path", columnDefinition = "text")
    private String editPath;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected StoreApplicationChangeRequestItem() {
    }

    public static StoreApplicationChangeRequestItem create(
            Long changeRequestId,
            String field,
            String label,
            String reasonCode,
            String message,
            String editPath,
            Integer displayOrder
    ) {
        StoreApplicationChangeRequestItem item = new StoreApplicationChangeRequestItem();
        item.changeRequestId = changeRequestId;
        item.field = field;
        item.label = label;
        item.reasonCode = reasonCode;
        item.message = message;
        item.editPath = editPath;
        item.displayOrder = displayOrder == null ? 0 : displayOrder;
        return item;
    }
}
