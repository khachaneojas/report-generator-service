package com.service.report.generator.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Comment("The files table stores information about various files uploaded into the system, providing details on each files metadata and facilitating efficient file management and retrieval.")
@Table(name = "files")
public class FileDataModel extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("This column uniquely identifies a file within the database")
    @Column(name = "fil_id")
    private Long fileId;

    @Comment("This column stores the name of file.")
    @Column(name = "fil_name", nullable = false)
    private String fileName;

    @Comment("This column stores the original name of file given by user.")
    @Column(name = "fil_origin", nullable = false)
    private String fileOriginal;

    @Comment("This column stores the type of file.")
    @Column(name = "fil_type", nullable = false)
    private String fileType;

    @Comment("This column stores the path of file.")
    @Column(name = "fil_path", nullable = false)
    private String filePath;

}
