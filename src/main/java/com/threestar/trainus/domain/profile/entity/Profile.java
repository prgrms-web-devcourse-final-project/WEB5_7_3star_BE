package com.threestar.trainus.domain.profile.entity;

import com.threestar.trainus.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "profile")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String profileImage;

    @Column(length = 255)
    private String intro;
}
