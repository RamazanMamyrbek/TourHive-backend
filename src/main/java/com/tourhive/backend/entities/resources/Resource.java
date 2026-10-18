package com.tourhive.backend.entities.resources;

import com.tourhive.backend.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resources")
public class Resource extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "\"key\"", nullable = false, unique = true, length = 500)
    private String key;

    @Column(name = "folder", nullable = false, length = 100)
    private String folder;

    @Column(name = "size", nullable = false)
    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ResourceType contentType;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    public Resource() {
    }

    public Resource(String name, String key, String folder, Long size, ResourceType contentType, String mimeType) {
        this.name = name;
        this.key = key;
        this.folder = folder;
        this.size = size;
        this.contentType = contentType;
        this.mimeType = mimeType;
    }
}
