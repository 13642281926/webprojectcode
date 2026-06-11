package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Column(length = 255)
    private String signature;

    @Column(nullable = false, length = 20)
    private String role = "user";

    @Column(nullable = false)
    private Integer studyDays;

    @Column(nullable = false)
    private Integer totalHours;
}
