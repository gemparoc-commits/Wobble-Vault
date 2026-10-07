package com.wobblevault.backend.features.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.wobblevault.backend.entity.Permission;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDTO {

    private UUID id;
    private String pageName;

    public PermissionDTO(Permission permission) {
        this.id = permission.getId();
        this.pageName = permission.getPageName();
    }
}
