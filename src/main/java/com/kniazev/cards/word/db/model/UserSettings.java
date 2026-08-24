package com.kniazev.cards.word.db.model;

import com.kniazev.cards.word.game.GameConfiguration;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode(exclude = "user")
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId                                 
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Type(JsonType.class)
    @Column(name = "game_configuration", columnDefinition = "json")
    private GameConfiguration gameConfiguration = new GameConfiguration();
    
}