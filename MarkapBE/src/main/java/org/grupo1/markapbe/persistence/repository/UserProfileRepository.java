package org.grupo1.markapbe.persistence.repository;

import org.grupo1.markapbe.persistence.entity.UserEntity;
import org.grupo1.markapbe.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserProfileRepository extends CrudRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findUserProfileEntityByUser(UserEntity user);

}
